package ru.otus.trackingbot.util;

/**
 * Утилитный класс для получения эмодзи по статусу посылки.
 */
public final class StatusEmojiUtils {

    private StatusEmojiUtils() {
        // Приватный конструктор для утилитного класса
    }

    /**
     * Возвращает эмодзи, соответствующий статусу посылки.
     *
     * @param status статус посылки
     * @return эмодзи для статуса
     */
    public static String getEmoji(String status) {
        if (status == null) {
            return "⚪";
        }

        String lowerStatus = status.toLowerCase();

        if (lowerStatus.contains("вручен") || lowerStatus.contains("доставлен")) {
            return "✅";
        } else if (lowerStatus.contains("пути") || lowerStatus.contains("транзит")) {
            return "🚚";
        } else if (lowerStatus.contains("принят") || lowerStatus.contains("сортировк")) {
            return "📦";
        } else if (lowerStatus.contains("возврат")) {
            return "↩️";
        } else if (lowerStatus.contains("ошибк")) {
            return "⚠️";
        }

        return "📌";
    }

    /**
     * Возвращает эмодзи для уведомления в зависимости от статуса.
     *
     * @param isDelivered доставлена ли посылка
     * @return эмодзи для уведомления
     */
    public static String getNotificationEmoji(boolean isDelivered) {
        return isDelivered ? "🎉" : "🔔";
    }
}
