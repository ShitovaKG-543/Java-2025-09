package ru.otus.trackingbot.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StatusEmojiUtils тесты")
class StatusEmojiUtilsTest {

    // ==================== ТЕСТЫ ДЛЯ getEmoji ====================

    @Test
    @DisplayName("Должен вернуть ⚪, когда статус равен null")
    void shouldReturnWhiteCircleWhenStatusIsNull() {

        String result = StatusEmojiUtils.getEmoji(null);

        assertThat(result).isEqualTo("⚪");
    }

    @Test
    @DisplayName("Должен вернуть ✅ для статуса, содержащего 'вручен'")
    void shouldReturnCheckMarkForVruchenStatus() {

        String result = StatusEmojiUtils.getEmoji("вручен");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен вернуть ✅ для статуса, содержащего 'ВРУЧЕН' (в верхнем регистре)")
    void shouldReturnCheckMarkForVruchenStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("ВРУЧЕН");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен вернуть ✅ для статуса, содержащего 'доставлен'")
    void shouldReturnCheckMarkForDostavlenStatus() {

        String result = StatusEmojiUtils.getEmoji("доставлен");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен вернуть ✅ для статуса, содержащего 'ДОСТАВЛЕН' (в верхнем регистре)")
    void shouldReturnCheckMarkForDostavlenStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("ДОСТАВЛЕН");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен вернуть ✅ для длинного статуса, содержащего 'вручен'")
    void shouldReturnCheckMarkForLongStatusWithVruchen() {

        String result = StatusEmojiUtils.getEmoji("Посылка вручена получателю");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен вернуть ✅ для длинного статуса, содержащего 'доставлен'")
    void shouldReturnCheckMarkForLongStatusWithDostavlen() {

        String result = StatusEmojiUtils.getEmoji("Посылка доставлена в пункт выдачи");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен вернуть 🚚 для статуса, содержащего 'пути'")
    void shouldReturnTruckForPutiStatus() {

        String result = StatusEmojiUtils.getEmoji("в пути");

        assertThat(result).isEqualTo("🚚");
    }

    @Test
    @DisplayName("Должен вернуть 🚚 для статуса, содержащего 'ПУТИ' (в верхнем регистре)")
    void shouldReturnTruckForPutiStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("В ПУТИ");

        assertThat(result).isEqualTo("🚚");
    }

    @Test
    @DisplayName("Должен вернуть 🚚 для статуса, содержащего 'транзит'")
    void shouldReturnTruckForTranzitStatus() {

        String result = StatusEmojiUtils.getEmoji("транзит");

        assertThat(result).isEqualTo("🚚");
    }

    @Test
    @DisplayName("Должен вернуть 🚚 для статуса, содержащего 'ТРАНЗИТ' (в верхнем регистре)")
    void shouldReturnTruckForTranzitStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("ТРАНЗИТ");

        assertThat(result).isEqualTo("🚚");
    }

    @Test
    @DisplayName("Должен вернуть 🚚 для длинного статуса, содержащего 'пути'")
    void shouldReturnTruckForLongStatusWithPuti() {

        String result = StatusEmojiUtils.getEmoji("Посылка находится в пути");

        assertThat(result).isEqualTo("🚚");
    }

    @Test
    @DisplayName("Должен вернуть 🚚 для длинного статуса, содержащего 'транзит'")
    void shouldReturnTruckForLongStatusWithTranzit() {

        String result = StatusEmojiUtils.getEmoji("Товар в транзите через сортировочный центр");

        assertThat(result).isEqualTo("🚚");
    }

    @Test
    @DisplayName("Должен вернуть 📦 для статуса, содержащего 'принят'")
    void shouldReturnPackageForPrinyatStatus() {

        String result = StatusEmojiUtils.getEmoji("принят");

        assertThat(result).isEqualTo("📦");
    }

    @Test
    @DisplayName("Должен вернуть 📦 для статуса, содержащего 'ПРИНЯТ' (в верхнем регистре)")
    void shouldReturnPackageForPrinyatStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("ПРИНЯТ");

        assertThat(result).isEqualTo("📦");
    }

    @Test
    @DisplayName("Должен вернуть 📦 для статуса, содержащего 'сортировк'")
    void shouldReturnPackageForSortirovkStatus() {

        String result = StatusEmojiUtils.getEmoji("сортировка");

        assertThat(result).isEqualTo("📦");
    }

    @Test
    @DisplayName("Должен вернуть 📦 для статуса, содержащего 'СОРТИРОВК' (в верхнем регистре)")
    void shouldReturnPackageForSortirovkStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("СОРТИРОВКА");

        assertThat(result).isEqualTo("📦");
    }

    @Test
    @DisplayName("Должен вернуть 📦 для длинного статуса, содержащего 'принят'")
    void shouldReturnPackageForLongStatusWithPrinyat() {

        String result = StatusEmojiUtils.getEmoji("Посылка принята в отделении связи");

        assertThat(result).isEqualTo("📦");
    }

    @Test
    @DisplayName("Должен вернуть 📦 для статуса, содержащего только 'сортировк' без слов о транзите")
    void shouldReturnPackageForStatusWithSortirovkOnly() {

        // Используем статус, который содержит "сортировк" но не содержит "пути" или "транзит"
        String result = StatusEmojiUtils.getEmoji("Сортировка");

        assertThat(result).isEqualTo("📦");
    }

    @Test
    @DisplayName("Должен вернуть ↩️ для статуса, содержащего 'возврат'")
    void shouldReturnReturnArrowForVozvratStatus() {

        String result = StatusEmojiUtils.getEmoji("возврат");

        assertThat(result).isEqualTo("↩️");
    }

    @Test
    @DisplayName("Должен вернуть ↩️ для статуса, содержащего 'ВОЗВРАТ' (в верхнем регистре)")
    void shouldReturnReturnArrowForVozvratStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("ВОЗВРАТ");

        assertThat(result).isEqualTo("↩️");
    }

    @Test
    @DisplayName("Должен вернуть ↩️ для длинного статуса, содержащего 'возврат'")
    void shouldReturnReturnArrowForLongStatusWithVozvrat() {

        String result = StatusEmojiUtils.getEmoji("Оформлен возврат отправителю");

        assertThat(result).isEqualTo("↩️");
    }

    @Test
    @DisplayName("Должен вернуть ⚠️ для статуса, содержащего 'ошибк'")
    void shouldReturnWarningForOshibkStatus() {

        String result = StatusEmojiUtils.getEmoji("ошибка");

        assertThat(result).isEqualTo("⚠️");
    }

    @Test
    @DisplayName("Должен вернуть ⚠️ для статуса, содержащего 'ОШИБК' (в верхнем регистре)")
    void shouldReturnWarningForOshibkStatusUppercase() {

        String result = StatusEmojiUtils.getEmoji("ОШИБКА");

        assertThat(result).isEqualTo("⚠️");
    }

    @Test
    @DisplayName("Должен вернуть ⚠️ для длинного статуса, содержащего 'ошибк'")
    void shouldReturnWarningForLongStatusWithOshibk() {

        String result = StatusEmojiUtils.getEmoji("Произошла ошибка при обработке");

        assertThat(result).isEqualTo("⚠️");
    }

    @Test
    @DisplayName("Должен вернуть 📌 для неизвестного статуса")
    void shouldReturnPushpinForUnknownStatus() {

        String result = StatusEmojiUtils.getEmoji("неизвестный статус");

        assertThat(result).isEqualTo("📌");
    }

    @Test
    @DisplayName("Должен вернуть 📌 для пустой строки статуса")
    void shouldReturnPushpinForEmptyString() {

        String result = StatusEmojiUtils.getEmoji("");

        assertThat(result).isEqualTo("📌");
    }

    @Test
    @DisplayName("Должен вернуть 📌 для статуса, не соответствующего ни одному ключевому слову")
    void shouldReturnPushpinForNonMatchingStatus() {

        String result = StatusEmojiUtils.getEmoji("задержан");

        assertThat(result).isEqualTo("📌");
    }

    @Test
    @DisplayName("Должен отдавать приоритет 'вручен' перед другими ключевыми словами")
    void shouldPrioritizeVruchenOverOtherKeywords() {

        String result = StatusEmojiUtils.getEmoji("вручен и в пути");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен отдавать приоритет 'доставлен' перед другими ключевыми словами")
    void shouldPrioritizeDostavlenOverOtherKeywords() {

        String result = StatusEmojiUtils.getEmoji("доставлен после сортировки");

        assertThat(result).isEqualTo("✅");
    }

    @Test
    @DisplayName("Должен отдавать приоритет 'пути' перед 'принят'")
    void shouldPrioritizePutiOverPrinyat() {

        String result = StatusEmojiUtils.getEmoji("в пути, принят");

        assertThat(result).isEqualTo("🚚");
    }

    @Test
    @DisplayName("Должен быть нечувствителен к регистру для всех статусов")
    void shouldBeCaseInsensitive() {

        assertThat(StatusEmojiUtils.getEmoji("ВРУЧЕН")).isEqualTo("✅");
        assertThat(StatusEmojiUtils.getEmoji("ДОСТАВЛЕН")).isEqualTo("✅");
        assertThat(StatusEmojiUtils.getEmoji("В ПУТИ")).isEqualTo("🚚");
        assertThat(StatusEmojiUtils.getEmoji("ТРАНЗИТ")).isEqualTo("🚚");
        assertThat(StatusEmojiUtils.getEmoji("ПРИНЯТ")).isEqualTo("📦");
        assertThat(StatusEmojiUtils.getEmoji("СОРТИРОВКА")).isEqualTo("📦");
        assertThat(StatusEmojiUtils.getEmoji("ВОЗВРАТ")).isEqualTo("↩️");
        assertThat(StatusEmojiUtils.getEmoji("ОШИБКА")).isEqualTo("⚠️");
    }

    // ==================== ТЕСТЫ ДЛЯ getNotificationEmoji ====================

    @Test
    @DisplayName("Должен вернуть 🎉, когда isDelivered равен true")
    void shouldReturnConfettiForDeliveredTrue() {

        String result = StatusEmojiUtils.getNotificationEmoji(true);

        assertThat(result).isEqualTo("🎉");
    }

    @Test
    @DisplayName("Должен вернуть 🔔, когда isDelivered равен false")
    void shouldReturnBellForDeliveredFalse() {

        String result = StatusEmojiUtils.getNotificationEmoji(false);

        assertThat(result).isEqualTo("🔔");
    }

    @Test
    @DisplayName("Должен возвращать правильное эмодзи для обоих состояний доставки")
    void shouldReturnCorrectEmojiForBothDeliveredStates() {

        assertThat(StatusEmojiUtils.getNotificationEmoji(true)).isEqualTo("🎉");
        assertThat(StatusEmojiUtils.getNotificationEmoji(false)).isEqualTo("🔔");
    }

    // ==================== ТЕСТЫ ДЛЯ ПРОВЕРКИ ПРИВАТНОГО КОНСТРУКТОРА ====================

    @Test
    @DisplayName("Должен иметь приватный конструктор")
    void shouldHavePrivateConstructor() throws Exception {

        Class<StatusEmojiUtils> clazz = StatusEmojiUtils.class;

        java.lang.reflect.Constructor<StatusEmojiUtils> constructor = clazz.getDeclaredConstructor();

        assertThat(constructor.isAccessible()).isFalse();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()))
                .isTrue();

        // Делаем конструктор доступным для тестирования
        constructor.setAccessible(true);
        StatusEmojiUtils instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
