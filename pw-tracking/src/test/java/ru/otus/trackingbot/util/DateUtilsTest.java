package ru.otus.trackingbot.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DateUtils тесты")
class DateUtilsTest {

    private static final LocalDateTime TEST_DATE = LocalDateTime.of(2024, Month.DECEMBER, 25, 14, 30, 45);
    private static final String EXPECTED_DEFAULT_FORMAT = "25.12.2024 14:30";
    private static final String EXPECTED_FULL_FORMAT = "25.12.2024 14:30:45";

    @Test
    @DisplayName("Должен форматировать дату в стандартном формате (dd.MM.yyyy HH:mm)")
    void shouldFormatDateInDefaultFormat() {

        String result = DateUtils.format(TEST_DATE);

        assertThat(result).isEqualTo(EXPECTED_DEFAULT_FORMAT);
    }

    @Test
    @DisplayName("Должен вернуть 'неизвестно', когда дата равна null")
    void shouldReturnUnknownWhenDateIsNull() {

        String result = DateUtils.format(null);

        assertThat(result).isEqualTo("неизвестно");
    }

    @Test
    @DisplayName("Должен корректно форматировать дату с однозначными днем и месяцем")
    void shouldFormatDateWithSingleDigitDayAndMonth() {

        LocalDateTime date = LocalDateTime.of(2024, Month.JANUARY, 5, 8, 3, 15);
        String expected = "05.01.2024 08:03";

        String result = DateUtils.format(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен корректно форматировать дату с двузначными днем и месяцем")
    void shouldFormatDateWithDoubleDigitDayAndMonth() {

        LocalDateTime date = LocalDateTime.of(2024, Month.DECEMBER, 25, 14, 30, 45);
        String expected = "25.12.2024 14:30";

        String result = DateUtils.format(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен форматировать дату с ведущими нулями для часов и минут")
    void shouldFormatDateWithLeadingZeros() {

        LocalDateTime date = LocalDateTime.of(2024, Month.MARCH, 7, 5, 2, 10);
        String expected = "07.03.2024 05:02";

        String result = DateUtils.format(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен форматировать дату с максимальными значениями")
    void shouldFormatDateWithMaximumValues() {

        LocalDateTime date = LocalDateTime.of(9999, Month.DECEMBER, 31, 23, 59, 59);
        String expected = "31.12.9999 23:59";

        String result = DateUtils.format(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен форматировать дату с минимальными значениями")
    void shouldFormatDateWithMinimumValues() {

        LocalDateTime date = LocalDateTime.of(1, Month.JANUARY, 1, 0, 0, 0);
        String expected = "01.01.0001 00:00";

        String result = DateUtils.format(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать дату високосного года")
    void shouldHandleLeapYearDate() {

        LocalDateTime date = LocalDateTime.of(2024, Month.FEBRUARY, 29, 12, 0, 0);
        String expected = "29.02.2024 12:00";

        String result = DateUtils.format(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен форматировать разные даты без побочных эффектов")
    void shouldFormatDifferentDatesWithoutSideEffects() {

        LocalDateTime date1 = LocalDateTime.of(2024, Month.JANUARY, 1, 10, 0, 0);
        LocalDateTime date2 = LocalDateTime.of(2024, Month.FEBRUARY, 2, 11, 30, 0);

        String result1 = DateUtils.format(date1);
        String result2 = DateUtils.format(date2);

        assertThat(result1).isEqualTo("01.01.2024 10:00");
        assertThat(result2).isEqualTo("02.02.2024 11:30");
    }

    @Test
    @DisplayName("Должен форматировать дату в полном формате (dd.MM.yyyy HH:mm:ss)")
    void shouldFormatDateInFullFormat() {

        String result = DateUtils.formatFull(TEST_DATE);

        assertThat(result).isEqualTo(EXPECTED_FULL_FORMAT);
    }

    @Test
    @DisplayName("Должен корректно форматировать дату с однозначными значениями дня, месяца, часа, минуты, секунды")
    void shouldFormatDateWithSingleDigitValues() {

        LocalDateTime date = LocalDateTime.of(2024, Month.JANUARY, 5, 8, 3, 7);
        String expected = "05.01.2024 08:03:07";

        String result = DateUtils.formatFull(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен корректно форматировать дату с двузначными значениями")
    void shouldFormatDateWithDoubleDigitValues() {

        LocalDateTime date = LocalDateTime.of(2024, Month.DECEMBER, 25, 14, 30, 45);
        String expected = "25.12.2024 14:30:45";

        String result = DateUtils.formatFull(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен форматировать дату с ведущими нулями для всех полей")
    void shouldFormatDateWithLeadingZerosForAllFields() {

        LocalDateTime date = LocalDateTime.of(2024, Month.MARCH, 5, 3, 2, 1);
        String expected = "05.03.2024 03:02:01";

        String result = DateUtils.formatFull(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать дату високосного года с секундами")
    void shouldHandleLeapYearDateWithSeconds() {

        LocalDateTime date = LocalDateTime.of(2024, Month.FEBRUARY, 29, 23, 59, 30);
        String expected = "29.02.2024 23:59:30";

        String result = DateUtils.formatFull(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен форматировать одну и ту же дату по-разному для стандартного и полного формата")
    void shouldFormatSameDateDifferently() {

        LocalDateTime date = LocalDateTime.of(2024, Month.DECEMBER, 25, 14, 30, 45);

        String defaultFormat = DateUtils.format(date);
        String fullFormat = DateUtils.formatFull(date);

        assertThat(defaultFormat).isEqualTo("25.12.2024 14:30");
        assertThat(fullFormat).isEqualTo("25.12.2024 14:30:45");
        assertThat(defaultFormat).isNotEqualTo(fullFormat);
    }

    @Test
    @DisplayName("Оба метода должны вернуть 'неизвестно' для даты равной null")
    void shouldBothReturnUnknownForNullDate() {

        String defaultFormat = DateUtils.format(null);
        String fullFormat = DateUtils.formatFull(null);

        assertThat(defaultFormat).isEqualTo("неизвестно");
        assertThat(fullFormat).isEqualTo("неизвестно");
        assertThat(defaultFormat).isEqualTo(fullFormat);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать конец месяца")
    void shouldHandleEndOfMonth() {

        LocalDateTime date = LocalDateTime.of(2024, Month.JANUARY, 31, 23, 59, 59);
        String expectedDefault = "31.01.2024 23:59";
        String expectedFull = "31.01.2024 23:59:59";

        String defaultFormat = DateUtils.format(date);
        String fullFormat = DateUtils.formatFull(date);

        assertThat(defaultFormat).isEqualTo(expectedDefault);
        assertThat(fullFormat).isEqualTo(expectedFull);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать конец года")
    void shouldHandleEndOfYear() {

        LocalDateTime date = LocalDateTime.of(2024, Month.DECEMBER, 31, 23, 59, 59);
        String expectedDefault = "31.12.2024 23:59";
        String expectedFull = "31.12.2024 23:59:59";

        String defaultFormat = DateUtils.format(date);
        String fullFormat = DateUtils.formatFull(date);

        assertThat(defaultFormat).isEqualTo(expectedDefault);
        assertThat(fullFormat).isEqualTo(expectedFull);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать граничные случаи часового пояса")
    void shouldHandleTimezoneBoundaryCases() {

        LocalDateTime date = LocalDateTime.of(2024, Month.DECEMBER, 31, 23, 59, 59);

        String result = DateUtils.format(date);

        assertThat(result).isEqualTo("31.12.2024 23:59");
    }

    @Test
    @DisplayName("Должен иметь приватный конструктор")
    void shouldHavePrivateConstructor() throws Exception {

        Class<DateUtils> clazz = DateUtils.class;

        java.lang.reflect.Constructor<DateUtils> constructor = clazz.getDeclaredConstructor();

        assertThat(constructor.isAccessible()).isFalse();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()))
                .isTrue();

        // Делаем конструктор доступным для тестирования
        constructor.setAccessible(true);
        DateUtils instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
