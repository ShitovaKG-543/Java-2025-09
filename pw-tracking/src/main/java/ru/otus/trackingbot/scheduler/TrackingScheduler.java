package ru.otus.trackingbot.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.bot.TrackingBot;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.ParcelService;
import ru.otus.trackingbot.service.ParcelUpdateService;
import ru.otus.trackingbot.service.TrackingCacheService;
import ru.otus.trackingbot.service.UserParcelService;
import ru.otus.trackingbot.util.DateUtils;

/**
 * Планировщик для автоматического обновления статусов посылок.
 * <p>
 * Периодически проверяет статусы всех активных посылок пользователей
 * и отправляет уведомления при изменениях.
 * </p>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *     <li>Запускается по cron-расписанию (по умолчанию каждые 5 минут)</li>
 *     <li>Может быть отключен через конфигурацию</li>
 *     <li>Отправляет уведомления только пользователям, включившим их</li>
 *     <li>Использует ParcelUpdateService для единообразного обновления статусов</li>
 * </ul>
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "scheduler.tracking.enabled", havingValue = "true", matchIfMissing = true)
public class TrackingScheduler {

    @Autowired
    private UserParcelService userParcelService;

    @Autowired
    private ParcelService parcelService;

    @Autowired
    private TrackingCacheService trackingCacheService;

    @Autowired
    private TrackingBot trackingBot;

    @Autowired
    private ParcelUpdateService parcelUpdateService;

    /**
     * Плановая проверка статусов всех посылок.
     * <p>
     * Выполняется по cron-расписанию (по умолчанию "0 0/5 * * * ?" - каждые 5 минут).
     * Для каждой активной посылки, у которой включены уведомления пользователя,
     * проверяет статус и отправляет уведомление при изменении.
     * </p>
     */
    @Scheduled(cron = "${scheduler.tracking.cron:0 0 0 * * ?}")
    public void checkTrackingStatuses() {
        log.info("🔄 Запуск плановой проверки статусов посылок");

        List<UserParcel> userParcels = userParcelService.getParcelsToUpdateWithDetails();

        if (userParcels.isEmpty()) {
            log.info("Нет посылок для проверки");
            return;
        }

        log.info("Найдено {} посылок для проверки", userParcels.size());
        int updatedCount = 0;
        int errorCount = 0;

        for (UserParcel userParcel : userParcels) {
            try {
                // Проверяем, включены ли уведомления у пользователя
                if (!userParcel.getUser().getNotificationEnabled()) {
                    continue;
                }

                // Используем общий сервис
                ParcelUpdateService.ParcelUpdateResult result =
                        parcelUpdateService.updateParcelStatus(userParcel, false);

                if (result.isSuccess() && result.isStatusChanged()) {
                    notifyUser(userParcel, result.getInfo());
                    updatedCount++;
                    log.info(
                            "Обновлен статус для {}: {} -> {}",
                            userParcel.getParcel().getTrackingNumber(),
                            result.getOldStatus(),
                            result.getNewStatus());
                } else if (!result.isSuccess()) {
                    errorCount++;
                    log.warn(
                            "Ошибка получения информации для {}: {}",
                            userParcel.getParcel().getTrackingNumber(),
                            result.getError());
                }

                // Небольшая задержка, чтобы не перегружать API
                Thread.sleep(1000);

            } catch (Exception e) {
                errorCount++;
                log.error(
                        "Ошибка при проверке посылки {}", userParcel.getParcel().getTrackingNumber(), e);
            }
        }

        log.info("✅ Проверка завершена. Обновлено статусов: {}, Ошибок: {}", updatedCount, errorCount);
    }

    /**
     * Отправляет пользователю уведомление об изменении статуса.
     *
     * @param userParcel связь пользователя с посылкой
     * @param info информация об отслеживании
     */
    private void notifyUser(UserParcel userParcel, TrackingInfo info) {
        SendMessage message = new SendMessage();
        message.setChatId(userParcel.getUser().getChatId());
        message.setParseMode("HTML");

        String statusEmoji = info.isDelivered() ? "🎉" : "🔔";
        String trackingNumber = userParcel.getParcel().getTrackingNumber();

        String text = String.format(
                "%s <b>Обновление статуса посылки</b> %s\n\n" + "📦 <b>Трек-номер:</b> %s\n"
                        + "🚚 <b>Служба:</b> %s\n"
                        + "📌 <b>Новый статус:</b> %s\n"
                        + "📝 %s\n\n"
                        + "🕐 <b>Время проверки:</b> %s",
                statusEmoji,
                info.isDelivered() ? "🎉 ПОСЫЛКА ДОСТАВЛЕНА!" : "",
                trackingNumber,
                userParcel.getParcel().getServiceName(),
                info.getStatus(),
                info.getStatusDescription() != null ? info.getStatusDescription() : "",
                DateUtils.formatFull(LocalDateTime.now()));

        message.setText(text);

        try {
            trackingBot.execute(message);
            userParcelService.sendNotification(userParcel, text);
            log.info(
                    "Уведомление отправлено пользователю {} для посылки {}",
                    userParcel.getUser().getChatId(),
                    trackingNumber);
        } catch (TelegramApiException e) {
            log.error(
                    "Ошибка при отправке уведомления пользователю {}",
                    userParcel.getUser().getChatId(),
                    e);
        }
    }
}
