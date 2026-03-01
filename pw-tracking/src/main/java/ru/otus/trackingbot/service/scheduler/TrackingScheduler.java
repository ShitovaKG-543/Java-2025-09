package ru.otus.trackingbot.service.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.bot.TrackingBot;
import ru.otus.trackingbot.model.TrackedItem;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.repository.TrackedItemRepository;
import ru.otus.trackingbot.service.AbstractTrackingService;
import ru.otus.trackingbot.service.TrackingServiceFactory;

@Component
@Slf4j
public class TrackingScheduler {

    @Autowired
    private TrackedItemRepository trackedItemRepository;

    @Autowired
    private TrackingServiceFactory trackingServiceFactory;

    @Autowired
    private TrackingBot trackingBot;

    @Value("${scheduler.tracking.fixed-delay}")
    private long fixedDelay;

    @Value("${scheduler.tracking.initial-delay}")
    private long initialDelay;

    // Проверка статусов с заданной периодичностью
    @Scheduled(
            fixedDelayString = "${scheduler.tracking.fixed-delay}",
            initialDelayString = "${scheduler.tracking.initial-delay}")
    public void checkTrackingStatuses() {
        log.info("🔄 Запуск плановой проверки статусов посылок");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        List<TrackedItem> itemsToCheck = trackedItemRepository.findItemsToUpdate(cutoffTime);

        if (itemsToCheck.isEmpty()) {
            log.info("Нет посылок для проверки");
            return;
        }

        log.info("Найдено {} посылок для проверки", itemsToCheck.size());
        int updatedCount = 0;
        int errorCount = 0;

        for (TrackedItem item : itemsToCheck) {
            try {
                Optional<AbstractTrackingService> serviceOpt =
                        trackingServiceFactory.detectService(item.getTrackingNumber());

                if (serviceOpt.isPresent()) {
                    TrackingInfo info = serviceOpt.get().trackParcel(item.getTrackingNumber());

                    if (info.isSuccess()) {
                        // Проверяем, изменился ли статус
                        if (!info.getStatus().equals(item.getLastStatus())) {
                            notifyUser(item, info);
                            item.setLastStatus(info.getStatus());
                            item.setLastStatusDescription(info.getStatusDescription());
                            item.setLastNotification(LocalDateTime.now());
                            item.setNotificationCount(item.getNotificationCount() + 1);
                            updatedCount++;
                        }

                        // Если посылка доставлена, можно деактивировать (опционально)
                        if (info.isDelivered() && item.isActive()) {
                            log.info("Посылка {} доставлена, деактивируем отслеживание", item.getTrackingNumber());
                            // Можно либо деактивировать, либо оставить для истории
                            // item.setActive(false); // Раскомментируйте если нужно авто-деактивировать
                        }

                        item.setLastChecked(LocalDateTime.now());
                        trackedItemRepository.save(item);
                    }
                }

                // Небольшая задержка, чтобы не перегружать API
                Thread.sleep(500);

            } catch (Exception e) {
                errorCount++;
                log.error("Ошибка при проверке посылки {}: {}", item.getTrackingNumber(), e.getMessage());
            }
        }

        log.info("✅ Проверка завершена. Обновлено: {}, Ошибок: {}", updatedCount, errorCount);
    }

    private void notifyUser(TrackedItem item, TrackingInfo info) {
        SendMessage message = new SendMessage();
        message.setChatId(item.getChatId());
        message.setParseMode("HTML");

        String statusColor = getStatusColor(info);
        String statusEmoji = info.isDelivered() ? "🎉" : "🔔";

        String text = String.format(
                "%s <b>Обновление статуса посылки</b> %s\n\n" + "📦 <b>Трек-номер:</b> %s\n"
                        + "🚚 <b>Служба:</b> %s\n"
                        + "%s <b>Новый статус:</b> %s\n"
                        + "📝 %s\n\n"
                        + "📍 <b>Место:</b> %s\n"
                        + "🕐 <b>Время:</b> %s\n\n"
                        + "Статус обновлений: %d",
                statusEmoji,
                info.isDelivered() ? "🎉 ПОСЫЛКА ДОСТАВЛЕНА!" : "",
                item.getTrackingNumber(),
                item.getServiceName(),
                statusColor,
                info.getStatus(),
                info.getStatusDescription() != null ? info.getStatusDescription() : "",
                info.getLastOperation() != null ? info.getLastOperation().getOperationPlace() : "неизвестно",
                formatDate(LocalDateTime.now()),
                item.getNotificationCount() + 1);

        message.setText(text);

        try {
            trackingBot.execute(message);
            log.info(
                    "Уведомление отправлено пользователю {} для посылки {}",
                    item.getChatId(),
                    item.getTrackingNumber());
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке уведомления пользователю {}", item.getChatId(), e);
        }
    }

    private String getStatusColor(TrackingInfo info) {
        if (info.isDelivered()) return "✅";
        if (info.getStatus() == null) return "⚪";

        String status = info.getStatus().toLowerCase();
        if (status.contains("пути") || status.contains("транзит")) {
            return "🟡";
        } else if (status.contains("принят") || status.contains("сортировк")) {
            return "🔵";
        } else if (status.contains("возврат") || status.contains("ошибк")) {
            return "🔴";
        }
        return "⚪";
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "неизвестно";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return date.format(formatter);
    }

    // Проверка "застрявших" посылок (раз в день)
    @Scheduled(cron = "0 0 12 * * ?") // Каждый день в 12:00
    public void checkStuckParcels() {
        log.info("🔍 Проверка застрявших посылок");

        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
        List<TrackedItem> oldItems = trackedItemRepository.findByActiveTrue().stream()
                .filter(item -> item.getCreatedAt().isBefore(twoWeeksAgo))
                // Вместо проверки isDelivered() проверяем статус через сервис
                .filter(item -> {
                    // Получаем сервис для этого трек-номера
                    Optional<AbstractTrackingService> serviceOpt =
                            trackingServiceFactory.detectService(item.getTrackingNumber());

                    if (serviceOpt.isPresent()) {
                        // Быстро проверяем статус (можно использовать кэш или последний известный статус)
                        String lastStatus = item.getLastStatus();
                        return lastStatus == null
                                || !(lastStatus.toLowerCase().contains("доставлен")
                                        || lastStatus.toLowerCase().contains("вручен"));
                    }
                    return true;
                })
                .toList();

        for (TrackedItem item : oldItems) {
            SendMessage message = new SendMessage();
            message.setChatId(item.getChatId());
            message.setText(String.format(
                    "⚠️ <b>Внимание!</b>\n\n"
                            + "Посылка %s отслеживается уже более 2 недель, но всё ещё не доставлена.\n"
                            + "Последний статус: %s\n\n"
                            + "Рекомендуем проверить информацию на официальном сайте.",
                    item.getTrackingNumber(), item.getLastStatus() != null ? item.getLastStatus() : "неизвестен"));
            message.setParseMode("HTML");

            try {
                trackingBot.execute(message);
                log.info("Отправлено уведомление о застрявшей посылке {}", item.getTrackingNumber());
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке уведомления о застрявшей посылке", e);
            }
        }
    }
}
