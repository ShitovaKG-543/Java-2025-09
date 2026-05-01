package ru.otus.trackingbot.bot.keyboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;

@DisplayName("KeyboardFactory тесты")
class KeyboardFactoryTest {

    private KeyboardFactory keyboardFactory;

    @BeforeEach
    void setUp() {
        keyboardFactory = new KeyboardFactory();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ REPLY KEYBOARD
    // =====================================================

    @Test
    @DisplayName("getMainKeyboard - должна создавать главную клавиатуру с 3 рядами и 6 кнопками")
    void getMainKeyboard_ShouldCreateMainKeyboardWith3RowsAnd6Buttons() {
        ReplyKeyboardMarkup keyboard = keyboardFactory.getMainKeyboard();

        assertThat(keyboard).isNotNull();
        assertThat(keyboard.getResizeKeyboard()).isTrue();
        assertThat(keyboard.getOneTimeKeyboard()).isFalse();

        List<KeyboardRow> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(3);

        // Проверяем первый ряд
        KeyboardRow row1 = rows.get(0);
        assertThat(row1).hasSize(2);
        assertThat(row1.get(0).getText()).isEqualTo("📦 Мои посылки");
        assertThat(row1.get(1).getText()).isEqualTo("🔍 Отследить посылку");

        // Проверяем второй ряд
        KeyboardRow row2 = rows.get(1);
        assertThat(row2).hasSize(2);
        assertThat(row2.get(0).getText()).isEqualTo("📊 Статистика");
        assertThat(row2.get(1).getText()).isEqualTo("🔔 Уведомления");

        // Проверяем третий ряд
        KeyboardRow row3 = rows.get(2);
        assertThat(row3).hasSize(2);
        assertThat(row3.get(0).getText()).isEqualTo("❓ Помощь");
        assertThat(row3.get(1).getText()).isEqualTo("ℹ️ О боте");
    }

    @Test
    @DisplayName("getConfirmationKeyboard - должна создавать клавиатуру подтверждения с 3 кнопками")
    void getConfirmationKeyboard_ShouldCreateConfirmationKeyboardWith3Buttons() {
        ReplyKeyboardMarkup keyboard = keyboardFactory.getConfirmationKeyboard();

        assertThat(keyboard).isNotNull();
        assertThat(keyboard.getResizeKeyboard()).isTrue();
        assertThat(keyboard.getOneTimeKeyboard()).isTrue();

        List<KeyboardRow> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(1);

        KeyboardRow row = rows.get(0);
        assertThat(row).hasSize(3);
        assertThat(row.get(0).getText()).isEqualTo("✅ Да");
        assertThat(row.get(1).getText()).isEqualTo("❌ Нет");
        assertThat(row.get(2).getText()).isEqualTo("🔙 Назад");
    }

    @Test
    @DisplayName("getCancelKeyboard - должна создавать клавиатуру отмены с 1 кнопкой")
    void getCancelKeyboard_ShouldCreateCancelKeyboardWith1Button() {
        ReplyKeyboardMarkup keyboard = keyboardFactory.getCancelKeyboard();

        assertThat(keyboard).isNotNull();
        assertThat(keyboard.getResizeKeyboard()).isTrue();
        assertThat(keyboard.getOneTimeKeyboard()).isTrue();

        List<KeyboardRow> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(1);

        KeyboardRow row = rows.get(0);
        assertThat(row).hasSize(1);
        assertThat(row.get(0).getText()).isEqualTo("❌ Отмена");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ INLINE KEYBOARD (PARCELS)
    // =====================================================

    @Test
    @DisplayName("getParcelsKeyboard - с пустым списком должна содержать только кнопку назад")
    void getParcelsKeyboard_WithEmptyList_ShouldContainOnlyBackButton() {
        List<UserParcel> emptyList = new ArrayList<>();

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(emptyList);

        assertThat(keyboard).isNotNull();
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(1);

        List<InlineKeyboardButton> backRow = rows.get(0);
        assertThat(backRow).hasSize(1);
        assertThat(backRow.get(0).getText()).isEqualTo("🔙 Назад в меню");
        assertThat(backRow.get(0).getCallbackData()).isEqualTo("back_to_menu");
    }

    @Test
    @DisplayName("getParcelsKeyboard - со списком посылок должна создавать кнопки для каждой посылки")
    void getParcelsKeyboard_WithParcelsList_ShouldCreateButtonsForEachParcel() {
        List<UserParcel> userParcels = createUserParcels(3);

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(userParcels);

        assertThat(keyboard).isNotNull();
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        // 3 посылки + 1 кнопка назад = 4 ряда
        assertThat(rows).hasSize(4);

        // Проверяем кнопки посылок
        for (int i = 0; i < 3; i++) {
            List<InlineKeyboardButton> row = rows.get(i);
            assertThat(row).hasSize(1);
            // Проверяем, что текст начинается с индикатора активности и эмодзи
            String buttonText = row.get(0).getText();
            assertThat(buttonText).matches("^[🟢⏸️] [📦✅🚚📌⚪] .+");
            assertThat(row.get(0).getCallbackData()).isEqualTo("parcel_" + (i + 1));
        }

        // Проверяем кнопку назад
        List<InlineKeyboardButton> backRow = rows.get(3);
        assertThat(backRow).hasSize(1);
        assertThat(backRow.get(0).getText()).isEqualTo("🔙 Назад в меню");
        assertThat(backRow.get(0).getCallbackData()).isEqualTo("back_to_menu");
    }

    @Test
    @DisplayName("getParcelsKeyboard - должна использовать customName если он задан")
    void getParcelsKeyboard_ShouldUseCustomNameWhenProvided() {
        List<UserParcel> userParcels = new ArrayList<>();
        UserParcel userParcel = createUserParcel(1L, "TRACK123", "Моя любимая посылка", true, "В пути");
        userParcels.add(userParcel);

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(userParcels);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        String buttonText = rows.get(0).get(0).getText();
        assertThat(buttonText).contains("Моя любимая посылка");
        assertThat(buttonText).doesNotContain("TRACK123");
    }

    @Test
    @DisplayName("getParcelsKeyboard - должна обрезать длинные названия (>30 символов)")
    void getParcelsKeyboard_ShouldTruncateLongNames() {
        List<UserParcel> userParcels = new ArrayList<>();
        String longName = "Очень-очень-очень-очень-очень длинное название посылки";
        UserParcel userParcel = createUserParcel(1L, "TRACK123", longName, true, "В пути");
        userParcels.add(userParcel);

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(userParcels);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        String buttonText = rows.get(0).get(0).getText();
        // Убираем префикс с эмодзи и индикатором для проверки длины
        String namePart = buttonText.substring(buttonText.lastIndexOf(" ") + 1);
        assertThat(namePart.length()).isLessThanOrEqualTo(30);
        assertThat(namePart).endsWith("...");
    }

    @Test
    @DisplayName("getParcelsKeyboard - должна использовать трек-номер если customName не задан")
    void getParcelsKeyboard_ShouldUseTrackingNumberWhenCustomNameNotSet() {
        List<UserParcel> userParcels = new ArrayList<>();
        UserParcel userParcel = createUserParcel(1L, "TRACK123456789", null, true, "В пути");
        userParcels.add(userParcel);

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(userParcels);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        String buttonText = rows.get(0).get(0).getText();
        assertThat(buttonText).contains("TRACK123456789");
    }

    @Test
    @DisplayName("getParcelsKeyboard - для активной посылки должна показывать зеленый индикатор")
    void getParcelsKeyboard_ForActiveParcel_ShouldShowGreenIndicator() {
        List<UserParcel> userParcels = new ArrayList<>();
        UserParcel activeParcel = createUserParcel(1L, "TRACK123", null, true, "В пути");
        userParcels.add(activeParcel);

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(userParcels);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        String buttonText = rows.get(0).get(0).getText();
        assertThat(buttonText).startsWith("🟢");
    }

    @Test
    @DisplayName("getParcelsKeyboard - для неактивной посылки должна показывать индикатор паузы")
    void getParcelsKeyboard_ForInactiveParcel_ShouldShowPauseIndicator() {
        List<UserParcel> userParcels = new ArrayList<>();
        UserParcel inactiveParcel = createUserParcel(1L, "TRACK123", null, false, "Остановлено");
        userParcels.add(inactiveParcel);

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(userParcels);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        String buttonText = rows.get(0).get(0).getText();
        assertThat(buttonText).startsWith("⏸️");
    }

    @Test
    @DisplayName("getParcelsKeyboard - должна показывать правильное эмодзи для статуса посылки")
    void getParcelsKeyboard_ShouldShowCorrectStatusEmoji() {
        List<UserParcel> userParcels = new ArrayList<>();

        // Посылка со статусом "Вручено"
        UserParcel deliveredParcel = createUserParcel(1L, "TRACK001", null, true, "Вручено адресату");
        userParcels.add(deliveredParcel);

        // Посылка со статусом "В пути"
        UserParcel transitParcel = createUserParcel(2L, "TRACK002", null, true, "В пути");
        userParcels.add(transitParcel);

        // Посылка с неизвестным статусом
        UserParcel unknownParcel = createUserParcel(3L, "TRACK003", null, true, null);
        userParcels.add(unknownParcel);

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelsKeyboard(userParcels);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();

        // Проверяем эмодзи для доставленной
        String deliveredText = rows.get(0).get(0).getText();
        assertThat(deliveredText).contains("✅");

        // Проверяем эмодзи для в пути
        String transitText = rows.get(1).get(0).getText();
        assertThat(transitText).contains("🚚");

        // Проверяем эмодзи для неизвестного статуса
        String unknownText = rows.get(2).get(0).getText();
        assertThat(unknownText).contains("⚪");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ PARCEL ACTIONS KEYBOARD
    // =====================================================

    @Test
    @DisplayName("getParcelActionsKeyboard - для активной посылки должна содержать кнопку 'Остановить отслеживание'")
    void getParcelActionsKeyboard_ForActiveParcel_ShouldHaveStopTrackingButton() {
        Long parcelId = 100L;
        boolean isActive = true;

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelActionsKeyboard(parcelId, isActive);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(5);

        // Проверяем кнопку "Остановить отслеживание" (третий ряд, вторая кнопка)
        List<InlineKeyboardButton> row3 = rows.get(2);
        assertThat(row3).hasSize(2);
        assertThat(row3.get(1).getText()).isEqualTo("⏸ Остановить отслеживание");
        assertThat(row3.get(1).getCallbackData()).isEqualTo("parcel_stop_" + parcelId);
    }

    @Test
    @DisplayName("getParcelActionsKeyboard - для неактивной посылки должна содержать кнопку 'Возобновить отслеживание'")
    void getParcelActionsKeyboard_ForInactiveParcel_ShouldHaveResumeTrackingButton() {
        Long parcelId = 100L;
        boolean isActive = false;

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelActionsKeyboard(parcelId, isActive);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(5);

        // Проверяем кнопку "Возобновить отслеживание" (третий ряд, вторая кнопка)
        List<InlineKeyboardButton> row3 = rows.get(2);
        assertThat(row3).hasSize(2);
        assertThat(row3.get(1).getText()).isEqualTo("▶️ Возобновить отслеживание");
        assertThat(row3.get(1).getCallbackData()).isEqualTo("parcel_resume_" + parcelId);
    }

    @Test
    @DisplayName("getParcelActionsKeyboard - должна содержать все необходимые кнопки")
    void getParcelActionsKeyboard_ShouldContainAllRequiredButtons() {
        Long parcelId = 100L;
        boolean isActive = true;

        InlineKeyboardMarkup keyboard = keyboardFactory.getParcelActionsKeyboard(parcelId, isActive);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(5);

        // Ряд 1: Полная информация
        assertThat(rows.get(0).get(0).getText()).isEqualTo("📋 Полная информация");
        assertThat(rows.get(0).get(0).getCallbackData()).isEqualTo("parcel_info_" + parcelId);

        // Ряд 2: История статусов
        assertThat(rows.get(1).get(0).getText()).isEqualTo("📜 История статусов");
        assertThat(rows.get(1).get(0).getCallbackData()).isEqualTo("parcel_history_" + parcelId);

        // Ряд 3: Обновить статус + Остановить/Возобновить
        assertThat(rows.get(2).get(0).getText()).isEqualTo("🔄 Обновить статус");
        assertThat(rows.get(2).get(0).getCallbackData()).isEqualTo("parcel_update_" + parcelId);

        // Ряд 4: Удалить
        assertThat(rows.get(3).get(0).getText()).isEqualTo("🗑 Удалить из отслеживания");
        assertThat(rows.get(3).get(0).getCallbackData()).isEqualTo("parcel_delete_" + parcelId);

        // Ряд 5: Назад
        assertThat(rows.get(4).get(0).getText()).isEqualTo("🔙 К списку посылок");
        assertThat(rows.get(4).get(0).getCallbackData()).isEqualTo("back_to_parcels");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ BACK TO PARCEL KEYBOARD
    // =====================================================

    @Test
    @DisplayName("getBackToParcelKeyboard - должна содержать кнопку 'Назад к действиям'")
    void getBackToParcelKeyboard_ShouldContainBackToActionsButton() {
        Long parcelId = 100L;

        InlineKeyboardMarkup keyboard = keyboardFactory.getBackToParcelKeyboard(parcelId);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get(0).getText()).isEqualTo("🔙 Назад к действиям");
        assertThat(rows.get(0).get(0).getCallbackData()).isEqualTo("parcel_" + parcelId);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ BACK FROM HISTORY KEYBOARD
    // =====================================================

    @Test
    @DisplayName("getBackFromHistoryKeyboard - должна содержать кнопки 'Обновить историю' и 'Назад к действиям'")
    void getBackFromHistoryKeyboard_ShouldContainRefreshAndBackButtons() {
        Long parcelId = 100L;

        InlineKeyboardMarkup keyboard = keyboardFactory.getBackFromHistoryKeyboard(parcelId);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(2);

        // Первая кнопка: Обновить историю
        assertThat(rows.get(0).get(0).getText()).isEqualTo("🔄 Обновить историю");
        assertThat(rows.get(0).get(0).getCallbackData()).isEqualTo("parcel_history_" + parcelId);

        // Вторая кнопка: Назад к действиям
        assertThat(rows.get(1).get(0).getText()).isEqualTo("🔙 Назад к действиям");
        assertThat(rows.get(1).get(0).getCallbackData()).isEqualTo("parcel_" + parcelId);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ CONFIRM DELETE KEYBOARD
    // =====================================================

    @Test
    @DisplayName("getConfirmDeleteKeyboard - должна содержать кнопки 'Да, удалить' и 'Отмена'")
    void getConfirmDeleteKeyboard_ShouldContainConfirmAndCancelButtons() {
        Long parcelId = 100L;

        InlineKeyboardMarkup keyboard = keyboardFactory.getConfirmDeleteKeyboard(parcelId);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).hasSize(2);

        // Кнопка подтверждения
        assertThat(rows.get(0).get(0).getText()).isEqualTo("✅ Да, удалить");
        assertThat(rows.get(0).get(0).getCallbackData()).isEqualTo("parcel_delete_confirm_" + parcelId);

        // Кнопка отмены
        assertThat(rows.get(0).get(1).getText()).isEqualTo("❌ Отмена");
        assertThat(rows.get(0).get(1).getCallbackData()).isEqualTo("parcel_" + parcelId);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ NOTIFICATIONS KEYBOARD
    // =====================================================

    @Test
    @DisplayName(
            "getNotificationsKeyboard - при включенных уведомлениях должна содержать кнопку 'Выключить уведомления'")
    void getNotificationsKeyboard_WhenEnabled_ShouldHaveDisableButton() {
        boolean enabled = true;

        InlineKeyboardMarkup keyboard = keyboardFactory.getNotificationsKeyboard(enabled);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(2);

        // Первая кнопка: Выключить уведомления
        assertThat(rows.get(0).get(0).getText()).isEqualTo("🔕 Выключить уведомления");
        assertThat(rows.get(0).get(0).getCallbackData()).isEqualTo("notifications_off");

        // Вторая кнопка: Назад в меню
        assertThat(rows.get(1).get(0).getText()).isEqualTo("🔙 Назад в меню");
        assertThat(rows.get(1).get(0).getCallbackData()).isEqualTo("back_to_menu");
    }

    @Test
    @DisplayName(
            "getNotificationsKeyboard - при выключенных уведомлениях должна содержать кнопку 'Включить уведомления'")
    void getNotificationsKeyboard_WhenDisabled_ShouldHaveEnableButton() {
        boolean enabled = false;

        InlineKeyboardMarkup keyboard = keyboardFactory.getNotificationsKeyboard(enabled);

        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(2);

        // Первая кнопка: Включить уведомления
        assertThat(rows.get(0).get(0).getText()).isEqualTo("🔔 Включить уведомления");
        assertThat(rows.get(0).get(0).getCallbackData()).isEqualTo("notifications_on");

        // Вторая кнопка: Назад в меню
        assertThat(rows.get(1).get(0).getText()).isEqualTo("🔙 Назад в меню");
        assertThat(rows.get(1).get(0).getCallbackData()).isEqualTo("back_to_menu");
    }

    // =====================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================

    private List<UserParcel> createUserParcels(int count) {
        List<UserParcel> userParcels = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            userParcels.add(createUserParcel((long) i, "TRACK" + i, null, true, "В пути"));
        }
        return userParcels;
    }

    private UserParcel createUserParcel(
            Long id, String trackingNumber, String customName, boolean isActive, String lastStatus) {
        User user =
                User.builder().id(1L).chatId(123456789L).username("test_user").build();

        // Исправлено: у Parcel больше нет поля id, только trackingNumber
        Parcel parcel = Parcel.builder()
                .trackingNumber(trackingNumber) // ← trackingNumber как PK
                .serviceName("Почта России")
                .build();

        return UserParcel.builder()
                .id(id)
                .user(user)
                .parcel(parcel)
                .customName(customName)
                .isActive(isActive)
                .lastStatus(lastStatus)
                .addedAt(LocalDateTime.now())
                .build();
    }
}
