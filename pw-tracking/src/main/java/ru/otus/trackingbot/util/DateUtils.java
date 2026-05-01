package ru.otus.trackingbot.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Утилитный класс для форматирования дат и времени.
 */
public final class DateUtils {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final DateTimeFormatter FULL_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private DateUtils() {
        // Приватный конструктор для утилитного класса
    }

    /**
     * Форматирует дату в стандартный формат (dd.MM.yyyy HH:mm).
     *
     * @param date дата для форматирования
     * @return отформатированная строка или "неизвестно" если date == null
     */
    public static String format(LocalDateTime date) {
        if (date == null) {
            return "неизвестно";
        }
        return date.format(DEFAULT_FORMATTER);
    }

    /**
     * Форматирует дату в полный формат (dd.MM.yyyy HH:mm:ss).
     *
     * @param date дата для форматирования
     * @return отформатированная строка или "неизвестно" если date == null
     */
    public static String formatFull(LocalDateTime date) {
        if (date == null) {
            return "неизвестно";
        }
        return date.format(FULL_FORMATTER);
    }
}
