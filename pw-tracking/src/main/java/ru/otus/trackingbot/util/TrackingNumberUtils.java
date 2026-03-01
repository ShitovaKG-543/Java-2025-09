package ru.otus.trackingbot.util;

/**
 * Утилитный класс для работы с трек-номерами.
 */
public final class TrackingNumberUtils {

    private TrackingNumberUtils() {
        // Приватный конструктор для утилитного класса
    }

    /**
     * Очищает трек-номер от лишних символов.
     * Удаляет пробелы, дефисы и другие специальные символы,
     * приводит к верхнему регистру.
     *
     * @param trackingNumber исходный трек-номер
     * @return очищенный трек-номер или null если входной параметр null
     */
    public static String clean(String trackingNumber) {
        if (trackingNumber == null) {
            return null;
        }
        return trackingNumber.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
