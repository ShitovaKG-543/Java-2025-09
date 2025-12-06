package ru.otus.processor.homework.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeConverter {

    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static String getDateTimeToString(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }
}
