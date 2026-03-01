package ru.otus.trackingbot.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TrackingNumberUtils тесты")
class TrackingNumberUtilsTest {

    // ==================== ТЕСТЫ ДЛЯ МЕТОДА clean ====================

    @Test
    @DisplayName("Должен вернуть null, когда trackingNumber равен null")
    void shouldReturnNullWhenTrackingNumberIsNull() {

        String result = TrackingNumberUtils.clean(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Должен удалять пробелы из трек-номера")
    void shouldRemoveSpaces() {

        String result = TrackingNumberUtils.clean("RA 123 456 789 RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять начальные и конечные пробелы")
    void shouldRemoveLeadingAndTrailingSpaces() {

        String result = TrackingNumberUtils.clean("  RA123456789RU  ");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять дефисы из трек-номера")
    void shouldRemoveHyphens() {

        String result = TrackingNumberUtils.clean("RA-123-456-789-RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять точки из трек-номера")
    void shouldRemoveDots() {

        String result = TrackingNumberUtils.clean("RA.123.456.789.RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять символы подчеркивания из трек-номера")
    void shouldRemoveUnderscores() {

        String result = TrackingNumberUtils.clean("RA_123_456_789_RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять слеши из трек-номера")
    void shouldRemoveSlashes() {

        String result = TrackingNumberUtils.clean("RA/123/456/789/RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять обратные слеши из трек-номера")
    void shouldRemoveBackslashes() {

        String result = TrackingNumberUtils.clean("RA\\123\\456\\789\\RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять запятые из трек-номера")
    void shouldRemoveCommas() {

        String result = TrackingNumberUtils.clean("RA,123,456,789,RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять точки с запятой из трек-номера")
    void shouldRemoveSemicolons() {

        String result = TrackingNumberUtils.clean("RA;123;456;789;RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять двоеточия из трек-номера")
    void shouldRemoveColons() {

        String result = TrackingNumberUtils.clean("RA:123:456:789:RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен удалять все специальные символы")
    void shouldRemoveAllSpecialCharacters() {

        String result = TrackingNumberUtils.clean("RA@123#456$789%RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен преобразовывать строчные буквы в заглавные")
    void shouldConvertToUppercase() {

        String result = TrackingNumberUtils.clean("ra123456789ru");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать смешанный регистр букв")
    void shouldHandleMixedCase() {

        String result = TrackingNumberUtils.clean("Ra123456789Ru");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен оставлять только буквы и цифры")
    void shouldKeepOnlyLettersAndNumbers() {

        String result = TrackingNumberUtils.clean("RA!@#$%123456789RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать формат трек-номера Почты России")
    void shouldHandleRussianPostFormat() {

        String result = TrackingNumberUtils.clean("RA123456789RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать формат трек-номера DHL")
    void shouldHandleDHLFormat() {

        String result = TrackingNumberUtils.clean("JD013456789012345");

        assertThat(result).isEqualTo("JD013456789012345");
    }

    @Test
    @DisplayName("Должен обрабатывать формат трек-номера UPS")
    void shouldHandleUPSFormat() {

        String result = TrackingNumberUtils.clean("1Z12345E1234567890");

        assertThat(result).isEqualTo("1Z12345E1234567890");
    }

    @Test
    @DisplayName("Должен обрабатывать формат трек-номера FedEx")
    void shouldHandleFedExFormat() {

        String result = TrackingNumberUtils.clean("123456789012");

        assertThat(result).isEqualTo("123456789012");
    }

    @Test
    @DisplayName("Должен обрабатывать сложный трек-номер с пробелами, дефисами и смешанным регистром")
    void shouldHandleComplexTrackingNumber() {

        String result = TrackingNumberUtils.clean("  RA-123-456-789-ru  ");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер с множественными пробелами между символами")
    void shouldHandleMultipleSpaces() {

        String result = TrackingNumberUtils.clean("RA   123   456   789   RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен возвращать пустую строку для трек-номера, содержащего только специальные символы")
    void shouldReturnEmptyStringForOnlySpecialCharacters() {

        String result = TrackingNumberUtils.clean("!@#$%^&*()");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Должен возвращать пустую строку для трек-номера, состоящего только из пробелов")
    void shouldReturnEmptyStringForOnlySpaces() {

        String result = TrackingNumberUtils.clean("     ");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Должен обрабатывать пустую строку")
    void shouldHandleEmptyString() {

        String result = TrackingNumberUtils.clean("");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Должен сохранять порядок цифр и букв")
    void shouldPreserveNumbersAndLettersInOrder() {

        String result = TrackingNumberUtils.clean("A1B2C3D4E5");

        assertThat(result).isEqualTo("A1B2C3D4E5");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер из одного символа")
    void shouldHandleSingleCharacter() {

        String result = TrackingNumberUtils.clean("A");

        assertThat(result).isEqualTo("A");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер из одной цифры")
    void shouldHandleSingleDigit() {

        String result = TrackingNumberUtils.clean("1");

        assertThat(result).isEqualTo("1");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер с символами новой строки и табуляции")
    void shouldHandleNewlinesAndTabs() {

        String result = TrackingNumberUtils.clean("RA\n123\t456\r789\nRU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Не должен изменять корректный трек-номер без специальных символов")
    void shouldNotModifyValidTrackingNumber() {

        String result = TrackingNumberUtils.clean("RA123456789RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер с круглыми скобками")
    void shouldHandleParentheses() {

        String result = TrackingNumberUtils.clean("RA(123)456-789RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер с квадратными скобками")
    void shouldHandleBrackets() {

        String result = TrackingNumberUtils.clean("RA[123]456-789RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер со знаком плюс")
    void shouldHandlePlusSign() {

        String result = TrackingNumberUtils.clean("RA+123+456+789+RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер со знаком равно")
    void shouldHandleEqualsSign() {

        String result = TrackingNumberUtils.clean("RA=123=456=789=RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    @Test
    @DisplayName("Должен обрабатывать трек-номер со знаком вопроса")
    void shouldHandleQuestionMark() {

        String result = TrackingNumberUtils.clean("RA?123?456?789?RU");

        assertThat(result).isEqualTo("RA123456789RU");
    }

    // ==================== ТЕСТЫ ДЛЯ ПРОВЕРКИ ПРИВАТНОГО КОНСТРУКТОРА ====================

    @Test
    @DisplayName("Должен иметь приватный конструктор")
    void shouldHavePrivateConstructor() throws Exception {

        Class<TrackingNumberUtils> clazz = TrackingNumberUtils.class;

        java.lang.reflect.Constructor<TrackingNumberUtils> constructor = clazz.getDeclaredConstructor();

        assertThat(constructor.isAccessible()).isFalse();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()))
                .isTrue();

        // Делаем конструктор доступным для тестирования
        constructor.setAccessible(true);
        TrackingNumberUtils instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
