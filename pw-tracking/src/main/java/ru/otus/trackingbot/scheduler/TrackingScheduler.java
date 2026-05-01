package ru.otus.trackingbot.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.bot.TrackingBot;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.ParcelUpdateService;
import ru.otus.trackingbot.service.UserParcelService;
import ru.otus.trackingbot.util.DateUtils;

/**
 * Планировщик для автоматического обновления статусов посылок.
 * <p>
 * Периодически проверяет статусы активных посылок и отправляет уведомления пользователям.
 * Разработан так, чтобы не нагружать систему - проверяет только посылки в пути.
 * </p>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *     <li>Запускается по cron-расписанию (по умолчанию раз в день в 2 часа ночи)</li>
 *     <li>Может быть полностью отключен через конфигурацию</li>
 *     <li>Отправляет уведомления только пользователям, включившим их</li>
 *     <li>Обрабатывает посылки пакетами для избежания перегрузки API</li>
 *     <li>Пропускает уже доставленные посылки</li>
 * </ul>
 *
 * <p><b>Настройка (application.yml):</b></p>
 * <pre>
 * scheduler:
 *   tracking:
 *     enabled: false          # Отключить автоматическое обновление (рекомендуется)
 *     cron: "0 0 2 * * ?"     # Раз в день в 2 часа ночи
 *     batch-size: 10          # Количество посылок в одном пакете
 * </pre>
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "scheduler.tracking.enabled", havingValue = "true", matchIfMissing = false)
public class TrackingScheduler {

    @Autowired
    private UserParcelService userParcelService;

    @Autowired
    private TrackingBot trackingBot;

    @Autowired
    private ParcelUpdateService parcelUpdateService;

    @Value("${scheduler.tracking.batch-size:10}")
    private int batchSize;

    /**
     * Плановая проверка статусов посылок.
     * <p>
     * Проверяет только посылки, которые ещё находятся в пути (не доставлены).
     * Запускается по cron-расписанию (по умолчанию раз в день в 2 часа ночи).
     * </p>
     */
    @Scheduled(cron = "${scheduler.tracking.cron:0 0 2 * * ?}")
    public void checkTrackingStatuses() {
        log.info("Запуск плановой пакетной проверки статусов");

        List<UserParcel> userParcels = userParcelService.getParcelsToUpdateWithDetails();

        if (userParcels.isEmpty()) {
            log.info("Нет посылок для проверки");
            return;
        }

        // Отбираем только посылки с включенными уведомлениями и не доставленные
        List<UserParcel> parcelsToCheck = userParcels.stream()
                .filter(up -> up.getUser().getNotificationEnabled())
                .filter(up -> !isDelivered(up.getLastStatus()))
                .toList();

        int skippedDelivered = userParcels.size() - parcelsToCheck.size();

        if (skippedDelivered > 0) {
            log.info(
                    "Найдено {} посылок для проверки ({} доставленных/пропущено, {} с отключенными уведомлениями)",
                    parcelsToCheck.size(),
                    skippedDelivered,
                    userParcels.stream()
                            .filter(up -> !up.getUser().getNotificationEnabled())
                            .count());
        } else {
            log.info("Найдено {} посылок для проверки", parcelsToCheck.size());
        }

        if (parcelsToCheck.isEmpty()) {
            log.info("Нет посылок, требующих проверки статуса");
            return;
        }

        int updatedCount = 0;
        int errorCount = 0;

        // Обрабатываем пакетами, чтобы не перегружать API
        for (int i = 0; i < parcelsToCheck.size(); i += batchSize) {
            int end = Math.min(i + batchSize, parcelsToCheck.size());
            List<UserParcel> batch = parcelsToCheck.subList(i, end);

            for (UserParcel userParcel : batch) {
                ParcelUpdateResult result = processParcel(userParcel);
                if (result.isUpdated()) {
                    updatedCount++;
                } else if (result.isError()) {
                    errorCount++;
                }
            }

            log.debug(
                    "Обработан пакет {}/{}", (i / batchSize) + 1, (parcelsToCheck.size() + batchSize - 1) / batchSize);
        }

        log.info("Пакетная проверка завершена. Обновлено статусов: {}, Ошибок: {}", updatedCount, errorCount);
    }

    /**
     * Обрабатывает одну посылку: получает свежий статус и уведомляет пользователя при изменении.
     *
     * @param userParcel связь пользователя с посылкой
     * @return результат обработки
     */
    private ParcelUpdateResult processParcel(UserParcel userParcel) {
        String trackingNumber = userParcel.getParcel().getTrackingNumber();

        try {
            ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(userParcel, false);

            if (result.isSuccess() && result.isStatusChanged()) {
                notifyUser(userParcel, result.getInfo());
                log.info(
                        "Статус обновлен для {}: {} -> {}",
                        trackingNumber,
                        result.getOldStatus(),
                        result.getNewStatus());
                return ParcelUpdateResult.updated();
            } else if (!result.isSuccess()) {
                log.warn("Не удалось получить информацию для {}: {}", trackingNumber, result.getError());
                return ParcelUpdateResult.error(result.getError());
            }

            return ParcelUpdateResult.noChange();

        } catch (Exception e) {
            log.error("Ошибка при обработке посылки {}", trackingNumber, e);
            return ParcelUpdateResult.error(e.getMessage());
        }
    }

    /**
     * Проверяет, доставлена ли посылка по её статусу.
     *
     * @param status текущий статус посылки
     * @return true если посылка доставлена
     */
    private boolean isDelivered(String status) {
        if (status == null) {
            return false;
        }
        String lowerStatus = status.toLowerCase();
        return lowerStatus.contains("доставлен")
                || lowerStatus.contains("вручен")
                || lowerStatus.contains("delivered")
                || lowerStatus.contains("вручено")
                || lowerStatus.contains("получено");
    }

    /**
     * Отправляет пользователю уведомление об изменении статуса.
     *
     * @param userParcel связь пользователя с посылкой
     * @param info информация об отслеживании с новым статусом
     */
    private void notifyUser(UserParcel userParcel, TrackingInfo info) {
        SendMessage message = new SendMessage();
        message.setChatId(userParcel.getUser().getChatId());
        message.setParseMode("HTML");

        String trackingNumber = userParcel.getParcel().getTrackingNumber();
        String displayName = userParcel.getCustomName() != null ? userParcel.getCustomName() : trackingNumber;
        String statusEmoji = info.isDelivered() ? "🎉" : "🔔";

        String text = String.format(
                "%s <b>Обновление статуса посылки</b> %s\n\n" + "📦 <b>Посылка:</b> %s\n"
                        + "📋 <b>Трек-номер:</b> <code>%s</code>\n"
                        + "🚚 <b>Служба доставки:</b> %s\n"
                        + "📌 <b>Новый статус:</b> %s\n"
                        + "📝 %s\n\n"
                        + "🕐 <b>Время проверки:</b> %s\n\n"
                        + "💡 Используйте /menu для просмотра всех посылок",
                statusEmoji,
                info.isDelivered() ? "🎉 ПОСЫЛКА ДОСТАВЛЕНА!" : "",
                displayName,
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
                    "Не удалось отправить уведомление пользователю {} для посылки {}",
                    userParcel.getUser().getChatId(),
                    trackingNumber,
                    e);
        }
    }

    /**
     * Результат обработки посылки.
     */
    private static class ParcelUpdateResult {
        private final boolean updated;
        private final boolean error;
        private final String errorMessage;

        private ParcelUpdateResult(boolean updated, boolean error, String errorMessage) {
            this.updated = updated;
            this.error = error;
            this.errorMessage = errorMessage;
        }

        /**
         * Создает результат с обновленным статусом.
         *
         * @return результат с флагом updated = true
         */
        public static ParcelUpdateResult updated() {
            return new ParcelUpdateResult(true, false, null);
        }

        /**
         * Создает результат без изменений.
         *
         * @return результат с флагами updated = false, error = false
         */
        public static ParcelUpdateResult noChange() {
            return new ParcelUpdateResult(false, false, null);
        }

        /**
         * Создает результат с ошибкой.
         *
         * @param message сообщение об ошибке
         * @return результат с флагом error = true
         */
        public static ParcelUpdateResult error(String message) {
            return new ParcelUpdateResult(false, true, message);
        }

        /**
         * Возвращает true, если статус был обновлен.
         *
         * @return true при успешном обновлении статуса
         */
        public boolean isUpdated() {
            return updated;
        }

        /**
         * Возвращает true, если произошла ошибка.
         *
         * @return true при ошибке
         */
        public boolean isError() {
            return error;
        }

        /**
         * Возвращает сообщение об ошибке.
         *
         * @return сообщение об ошибке или null
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
