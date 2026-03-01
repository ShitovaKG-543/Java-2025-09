package ru.otus.trackingbot.bot.keyboard;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.util.StatusEmojiUtils;

/**
 * Фабрика для создания клавиатур Telegram бота.
 * <p>
 * Предоставляет методы для генерации различных типов клавиатур:
 * обычных (ReplyKeyboardMarkup) и инлайн-клавиатур (InlineKeyboardMarkup)
 * для взаимодействия с пользователем.
 * </p>
 */
@Component
public class KeyboardFactory {

    /**
     * Создает главную клавиатуру с основными командами бота.
     * <p>
     * Клавиатура содержит следующие кнопки:
     * <ul>
     *     <li>📦 Мои посылки</li>
     *     <li>🔍 Отследить посылку</li>
     *     <li>📊 Статистика</li>
     *     <li>🔔 Уведомления</li>
     *     <li>❓ Помощь</li>
     *     <li>ℹ️ О боте</li>
     * </ul>
     * </p>
     *
     * @return настроенная ReplyKeyboardMarkup для главного меню
     */
    public ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        // Первый ряд
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📦 Мои посылки"));
        row1.add(new KeyboardButton("🔍 Отследить посылку"));

        // Второй ряд
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📊 Статистика"));
        row2.add(new KeyboardButton("🔔 Уведомления"));

        // Третий ряд
        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("❓ Помощь"));
        row3.add(new KeyboardButton("ℹ️ О боте"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    /**
     * Создает клавиатуру для подтверждения действий пользователя.
     * <p>
     * Используется для запроса подтверждения перед выполнением
     * критических операций (удаление, остановка отслеживания и т.д.).
     * Клавиатура одноразовая (скрывается после нажатия).
     * </p>
     *
     * @return клавиатура с кнопками "✅ Да", "❌ Нет", "🔙 Назад"
     */
    public ReplyKeyboardMarkup getConfirmationKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("✅ Да"));
        row.add(new KeyboardButton("❌ Нет"));
        row.add(new KeyboardButton("🔙 Назад"));

        rows.add(row);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    /**
     * Создает клавиатуру для отмены текущей операции.
     * <p>
     * Используется в режимах ожидания ввода данных
     * (например, при вводе трек-номера).
     * </p>
     *
     * @return клавиатура с одной кнопкой "❌ Отмена"
     */
    public ReplyKeyboardMarkup getCancelKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("❌ Отмена"));

        rows.add(row);
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    /**
     * Создает инлайн-клавиатуру со списком посылок пользователя.
     * <p>
     * Каждая посылка представлена отдельной кнопкой с callback-данными.
     * Длинные названия обрезаются до 30 символов.
     * В конце списка добавляется кнопка возврата в главное меню.
     * </p>
     *
     * @param userParcels список посылок пользователя
     * @return инлайн-клавиатура со списком посылок
     */
    public InlineKeyboardMarkup getParcelsKeyboard(List<UserParcel> userParcels) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (UserParcel userParcel : userParcels) {
            Parcel parcel = userParcel.getParcel();
            String displayName =
                    userParcel.getCustomName() != null ? userParcel.getCustomName() : parcel.getTrackingNumber();

            // Обрезаем длинные названия
            if (displayName.length() > 30) {
                displayName = displayName.substring(0, 27) + "...";
            }

            // Добавляем индикатор активности
            String prefix = userParcel.getIsActive() ? "🟢 " : "⏸️ ";
            String emoji = StatusEmojiUtils.getEmoji(userParcel.getLastStatus());

            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(prefix + emoji + " " + displayName);
            button.setCallbackData("parcel_" + userParcel.getId());
            row.add(button);
            rows.add(row);
        }

        // Добавляем кнопку "Назад в меню"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад в меню");
        backButton.setCallbackData("back_to_menu");
        backRow.add(backButton);
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создает инлайн-клавиатуру с действиями для выбранной посылки.
     * <p>
     * Доступные действия:
     * <ul>
     *     <li>📋 Полная информация</li>
     *     <li>📜 История статусов</li>
     *     <li>🔄 Обновить статус</li>
     *     <li>⏸/▶️ Остановить/Возобновить отслеживание</li>
     *     <li>🗑 Удалить из отслеживания</li>
     *     <li>🔙 К списку посылок</li>
     * </ul>
     * </p>
     *
     * @param parcelId идентификатор посылки
     * @param isActive активна ли посылка (отслеживается)
     * @return инлайн-клавиатура с действиями для посылки
     */
    public InlineKeyboardMarkup getParcelActionsKeyboard(Long parcelId, boolean isActive) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Первый ряд - информация
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton infoButton = new InlineKeyboardButton();
        infoButton.setText("📋 Полная информация");
        infoButton.setCallbackData("parcel_info_" + parcelId);
        row1.add(infoButton);
        rows.add(row1);

        // Второй ряд - история
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton historyButton = new InlineKeyboardButton();
        historyButton.setText("📜 История статусов");
        historyButton.setCallbackData("parcel_history_" + parcelId);
        row2.add(historyButton);
        rows.add(row2);

        // Третий ряд - обновить и остановить/возобновить
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        updateButton.setText("🔄 Обновить статус");
        updateButton.setCallbackData("parcel_update_" + parcelId);
        row3.add(updateButton);

        InlineKeyboardButton stopButton = new InlineKeyboardButton();
        if (isActive) {
            stopButton.setText("⏸ Остановить отслеживание");
            stopButton.setCallbackData("parcel_stop_" + parcelId);
        } else {
            stopButton.setText("▶️ Возобновить отслеживание");
            stopButton.setCallbackData("parcel_resume_" + parcelId);
        }
        row3.add(stopButton);
        rows.add(row3);

        // Четвертый ряд - удалить
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("🗑 Удалить из отслеживания");
        deleteButton.setCallbackData("parcel_delete_" + parcelId);
        row4.add(deleteButton);
        rows.add(row4);

        // Пятый ряд - назад к списку посылок
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 К списку посылок");
        backButton.setCallbackData("back_to_parcels");
        row5.add(backButton);
        rows.add(row5);

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создает инлайн-клавиатуру для экрана полной информации о посылке.
     * Содержит только кнопку возврата к действиям с посылкой.
     *
     * @param parcelId идентификатор посылки
     * @return инлайн-клавиатура с кнопкой "Назад к действиям"
     */
    public InlineKeyboardMarkup getBackToParcelKeyboard(Long parcelId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад к действиям");
        backButton.setCallbackData("parcel_" + parcelId);
        row.add(backButton);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создает инлайн-клавиатуру для экрана истории статусов.
     * <p>
     * Содержит кнопки:
     * <ul>
     *     <li>🔄 Обновить историю</li>
     *     <li>🔙 Назад к действиям</li>
     * </ul>
     * </p>
     *
     * @param parcelId идентификатор посылки
     * @return инлайн-клавиатура для истории статусов
     */
    public InlineKeyboardMarkup getBackFromHistoryKeyboard(Long parcelId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton refreshButton = new InlineKeyboardButton();
        refreshButton.setText("🔄 Обновить историю");
        refreshButton.setCallbackData("parcel_history_" + parcelId);
        row1.add(refreshButton);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад к действиям");
        backButton.setCallbackData("parcel_" + parcelId);
        row2.add(backButton);
        rows.add(row2);

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создает инлайн-клавиатуру для подтверждения удаления посылки.
     * <p>
     * Содержит кнопки:
     * <ul>
     *     <li>✅ Да, удалить</li>
     *     <li>❌ Отмена</li>
     * </ul>
     * </p>
     *
     * @param parcelId идентификатор посылки
     * @return инлайн-клавиатура для подтверждения удаления
     */
    public InlineKeyboardMarkup getConfirmDeleteKeyboard(Long parcelId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("✅ Да, удалить");
        confirmButton.setCallbackData("parcel_delete_confirm_" + parcelId);
        row.add(confirmButton);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton();
        cancelButton.setText("❌ Отмена");
        cancelButton.setCallbackData("parcel_" + parcelId);
        row.add(cancelButton);
        rows.add(row);

        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создает инлайн-клавиатуру для настройки уведомлений.
     * <p>
     * Кнопка переключает состояние уведомлений:
     * <ul>
     *     <li>🔔 Включить уведомления</li>
     *     <li>🔕 Выключить уведомления</li>
     * </ul>
     * Также содержит кнопку возврата в главное меню.
     * </p>
     *
     * @param enabled текущее состояние уведомлений (включены/выключены)
     * @return инлайн-клавиатура для настройки уведомлений
     */
    public InlineKeyboardMarkup getNotificationsKeyboard(boolean enabled) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();

        if (enabled) {
            button.setText("🔕 Выключить уведомления");
            button.setCallbackData("notifications_off");
        } else {
            button.setText("🔔 Включить уведомления");
            button.setCallbackData("notifications_on");
        }

        row.add(button);
        rows.add(row);

        // Кнопка назад
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад в меню");
        backButton.setCallbackData("back_to_menu");
        backRow.add(backButton);
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }
}
