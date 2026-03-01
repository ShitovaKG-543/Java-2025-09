package ru.otus.trackingbot.util;

import java.util.regex.Pattern;

public class TrackNumberValidator {

    // Паттерн для трек-номеров СДЭК (обычно 10-20 цифр)
    private static final Pattern CDEK_PATTERN = Pattern.compile("^\\d{10,20}$");

    // Паттерн для трек-номеров Почты России (14 цифр для внутренних, 13-14 для международных)
    private static final Pattern RUSSIAN_POST_PATTERN = Pattern.compile("^\\d{13,14}$");

    // Международный формат (буквы и цифры) - например, RA123456789RU
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^[A-Z]{2}\\d{9}[A-Z]{2}$");

    // Универсальный паттерн для любого трек-номера (буквы, цифры, дефисы)
    private static final Pattern ANY_TRACK_PATTERN = Pattern.compile("^[A-Z0-9-]{8,20}$", Pattern.CASE_INSENSITIVE);

    public static String detectCarrier(String trackNumber) {
        if (trackNumber == null || trackNumber.trim().isEmpty()) {
            return "UNKNOWN";
        }

        String cleaned = cleanTrackNumber(trackNumber);

        if (CDEK_PATTERN.matcher(cleaned).matches()) {
            return "CDEK";
        } else if (RUSSIAN_POST_PATTERN.matcher(cleaned).matches()) {
            return "RUSSIAN_POST";
        } else if (INTERNATIONAL_PATTERN.matcher(cleaned).matches()) {
            return "INTERNATIONAL";
        } else if (ANY_TRACK_PATTERN.matcher(cleaned).matches()) {
            return "UNKNOWN_FORMAT";
        }

        return "INVALID";
    }

    public static boolean isValidTrackNumber(String trackNumber) {
        if (trackNumber == null || trackNumber.trim().isEmpty()) {
            return false;
        }
        String cleaned = cleanTrackNumber(trackNumber);
        return CDEK_PATTERN.matcher(cleaned).matches()
                || RUSSIAN_POST_PATTERN.matcher(cleaned).matches()
                || INTERNATIONAL_PATTERN.matcher(cleaned).matches()
                || ANY_TRACK_PATTERN.matcher(cleaned).matches();
    }

    public static String cleanTrackNumber(String trackNumber) {
        if (trackNumber == null) return "";
        // Удаляем пробелы, дефисы и переводим в верхний регистр
        return trackNumber.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static String getCarrierName(String carrierCode) {
        switch (carrierCode) {
            case "CDEK":
                return "СДЭК";
            case "RUSSIAN_POST":
                return "Почта России";
            case "INTERNATIONAL":
                return "Международное";
            case "UNKNOWN_FORMAT":
                return "Неизвестный формат";
            default:
                return "Не определен";
        }
    }
}
