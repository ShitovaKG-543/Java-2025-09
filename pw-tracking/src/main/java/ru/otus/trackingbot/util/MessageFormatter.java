package ru.otus.trackingbot.util;

import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.ParcelStatusHistory;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;

/**
 * Утилитный класс для форматирования сообщений.
 */
public final class MessageFormatter {

    private MessageFormatter() {
        // Приватный конструктор для утилитного класса
    }

    /**
     * Форматирует полную информацию о посылке из базы данных.
     *
     * @param userParcel связь пользователя с посылкой
     * @param parcel объект посылки
     * @param lastStatus последний статус из истории
     * @return отформатированная строка с информацией
     */
    public static String formatParcelInfoFromDB(UserParcel userParcel, Parcel parcel, ParcelStatusHistory lastStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>📦 Полная информация о посылке</b>\n\n");

        // Основная информация
        sb.append("📋 <b>Трек-номер:</b> <code>")
                .append(parcel.getTrackingNumber())
                .append("</code>\n");
        sb.append("🚚 <b>Служба доставки:</b> ").append(parcel.getServiceName()).append("\n");

        if (parcel.getDescription() != null && !parcel.getDescription().isEmpty()) {
            sb.append("📝 <b>Описание:</b> ").append(parcel.getDescription()).append("\n");
        }

        if (parcel.getWeight() != null && parcel.getWeight().doubleValue() > 0) {
            sb.append("⚖️ <b>Вес:</b> ")
                    .append(String.format("%.2f кг", parcel.getWeight()))
                    .append("\n");
        }

        if (parcel.getEstimatedDelivery() != null) {
            sb.append("📅 <b>Ожидаемая дата доставки:</b> ")
                    .append(parcel.getEstimatedDelivery())
                    .append("\n");
        }

        sb.append("\n");

        // Информация об отслеживании
        sb.append("<b>📊 Информация об отслеживании</b>\n");

        String status = userParcel.getLastStatus();
        if (status == null || status.isEmpty()) {
            status = "Статус неизвестен";
        }

        String statusEmoji = StatusEmojiUtils.getEmoji(status);
        sb.append("📌 <b>Текущий статус:</b> ")
                .append(statusEmoji)
                .append(" ")
                .append(status)
                .append("\n");

        if (userParcel.getLastStatusDescription() != null
                && !userParcel.getLastStatusDescription().isEmpty()) {
            sb.append("📝 <i>").append(userParcel.getLastStatusDescription()).append("</i>\n");
        }

        sb.append("🕐 <b>Последняя проверка:</b> ")
                .append(DateUtils.format(userParcel.getLastChecked()))
                .append("\n");
        sb.append("🔔 <b>Уведомлений отправлено:</b> ")
                .append(userParcel.getNotificationCount())
                .append("\n");
        sb.append("📅 <b>Добавлена в отслеживание:</b> ")
                .append(DateUtils.format(userParcel.getAddedAt()))
                .append("\n");

        if (userParcel.getIsActive()) {
            sb.append("✅ <b>Отслеживание:</b> активно\n");
        } else {
            sb.append("⏸ <b>Отслеживание:</b> остановлено\n");
        }

        if (lastStatus != null) {
            sb.append("\n<b>📍 Последняя операция в истории:</b>\n");
            if (lastStatus.getOperationDate() != null) {
                sb.append("🕐 <b>Дата:</b> ")
                        .append(DateUtils.format(lastStatus.getOperationDate()))
                        .append("\n");
            }
            sb.append("📌 <b>Операция:</b> ").append(lastStatus.getStatusName()).append("\n");
            if (lastStatus.getOperationPlace() != null
                    && !lastStatus.getOperationPlace().isEmpty()) {
                sb.append("🏢 <b>Место:</b> ")
                        .append(lastStatus.getOperationPlace())
                        .append("\n");
            }
        }

        sb.append("\n───────────────────\n");
        sb.append("<i>💡 Чтобы обновить статус, используйте кнопку «Обновить статус» в меню посылки.</i>");

        return sb.toString();
    }

    /**
     * Форматирует информацию об отслеживании для отображения.
     *
     * @param info информация об отслеживании
     * @param title заголовок
     * @return отформатированная строка
     */
    public static String formatTrackingInfo(TrackingInfo info, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n\n");

        String statusEmoji = StatusEmojiUtils.getEmoji(info.getStatus());
        sb.append(statusEmoji)
                .append(" <b>Статус:</b> ")
                .append(info.getStatus())
                .append("\n");

        if (info.getStatusDescription() != null) {
            sb.append("📝 ").append(info.getStatusDescription()).append("\n");
        }

        sb.append("📋 <b>Трек-номер:</b> <code>")
                .append(info.getTrackingNumber())
                .append("</code>\n");
        sb.append("🚚 <b>Служба:</b> ").append(info.getServiceName()).append("\n");

        if (info.isDelivered()) {
            sb.append("✅ <b>Посылка доставлена!</b> 🎉\n");
        }

        if (info.getWeight() != null && info.getWeight() > 0) {
            sb.append("⚖️ <b>Вес:</b> ")
                    .append(String.format("%.2f кг", info.getWeight()))
                    .append("\n");
        }

        if (info.getLastOperation() != null && info.getLastOperation().getDate() != null) {
            sb.append("\n<b>📍 Последняя операция:</b>\n");
            sb.append("🕐 ")
                    .append(DateUtils.format(info.getLastOperation().getDate()))
                    .append("\n");

            if (info.getLastOperation().getOperationPlace() != null) {
                sb.append("🏢 ")
                        .append(info.getLastOperation().getOperationPlace())
                        .append("\n");
            }
        }

        sb.append("\n🕒 <b>Проверено:</b> ").append(DateUtils.format(info.getLastCheck()));

        return sb.toString();
    }

    /**
     * Форматирует уведомление об изменении статуса.
     *
     * @param userParcel связь пользователя с посылкой
     * @param info информация об отслеживании
     * @param oldStatus старый статус
     * @param newStatus новый статус
     * @return отформатированное уведомление
     */
    public static String formatStatusChangeNotification(
            UserParcel userParcel, TrackingInfo info, String oldStatus, String newStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 <b>Изменение статуса посылки!</b>\n\n");
        sb.append("📦 <b>Трек-номер:</b> <code>")
                .append(userParcel.getParcel().getTrackingNumber())
                .append("</code>\n");
        sb.append("📌 <b>Было:</b> ")
                .append(oldStatus != null ? oldStatus : "неизвестно")
                .append("\n");
        sb.append("📌 <b>Стало:</b> ").append(newStatus).append("\n");

        if (info.getStatusDescription() != null) {
            sb.append("📝 ").append(info.getStatusDescription()).append("\n");
        }

        if (info.isDelivered()) {
            sb.append("\n🎉 <b>ПОСЫЛКА ДОСТАВЛЕНА!</b> 🎉");
        }

        return sb.toString();
    }
}
