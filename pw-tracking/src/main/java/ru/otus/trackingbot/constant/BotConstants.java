package ru.otus.trackingbot.constant;

/**
 * Константы для бота.
 * Содержит все текстовые строки, команды, callback данные и параметры.
 */
public final class BotConstants {

    private BotConstants() {
        // Приватный конструктор для класса констант
    }

    // =====================================================
    // КОМАНДЫ
    // =====================================================
    public static final String CMD_START = "/start";
    public static final String CMD_MENU = "/menu";
    public static final String CMD_TRACK = "/track";
    public static final String CMD_LIST = "/list";
    public static final String CMD_HELP = "/help";

    // =====================================================
    // ТЕКСТЫ КНОПОК
    // =====================================================
    public static final String BTN_MY_PARCELS = "📦 Мои посылки";
    public static final String BTN_TRACK_PARCEL = "🔍 Отследить посылку";
    public static final String BTN_STATISTICS = "📊 Статистика";
    public static final String BTN_NOTIFICATIONS = "🔔 Уведомления";
    public static final String BTN_HELP = "❓ Помощь";
    public static final String BTN_ABOUT = "ℹ️ О боте";
    public static final String BTN_BACK_TO_MENU = "🔙 Главное меню";
    public static final String BTN_BACK = "🔙 Назад";
    public static final String BTN_CANCEL = "❌ Отмена";
    public static final String BTN_YES = "✅ Да";
    public static final String BTN_NO = "❌ Нет";

    // =====================================================
    // CALLBACK DATA
    // =====================================================
    public static final String CALLBACK_BACK_TO_MENU = "back_to_menu";
    public static final String CALLBACK_BACK_TO_PARCELS = "back_to_parcels";
    public static final String CALLBACK_NOTIFICATIONS_ON = "notifications_on";
    public static final String CALLBACK_NOTIFICATIONS_OFF = "notifications_off";

    // Префиксы callback данных
    public static final String PREFIX_PARCEL = "parcel";
    public static final String PREFIX_PARCEL_INFO = "parcel_info";
    public static final String PREFIX_PARCEL_HISTORY = "parcel_history";
    public static final String PREFIX_PARCEL_UPDATE = "parcel_update";
    public static final String PREFIX_PARCEL_STOP = "parcel_stop";
    public static final String PREFIX_PARCEL_RESUME = "parcel_resume";
    public static final String PREFIX_PARCEL_DELETE = "parcel_delete";
    public static final String PREFIX_PARCEL_DELETE_CONFIRM = "parcel_delete_confirm";

    // =====================================================
    // ЗАГОЛОВКИ СООБЩЕНИЙ
    // =====================================================
    public static final String TITLE_MAIN_MENU = "<b>🏠 Главное меню</b>\n\nВыберите действие:";
    public static final String TITLE_MY_PARCELS = "<b>📋 Ваши посылки</b>\n\n";
    public static final String TITLE_PARCEL_INFO = "<b>📦 Полная информация о посылке</b>\n\n";
    public static final String TITLE_STATUS_HISTORY = "<b>📜 История статусов</b>\n";
    public static final String TITLE_STATISTICS = "<b>📊 Ваша статистика</b>\n\n";
    public static final String TITLE_NOTIFICATIONS = "<b>🔔 Настройка уведомлений</b>\n\n";
    public static final String TITLE_HELP = "<b>❓ Помощь по использованию бота</b>\n\n";
    public static final String TITLE_ABOUT = "<b>ℹ️ О боте</b>\n\n";

    // =====================================================
    // ТЕКСТЫ СООБЩЕНИЙ
    // =====================================================
    public static final String MSG_PARCEL_NOT_FOUND = "❌ Посылка не найдена";
    public static final String MSG_UNKNOWN_COMMAND =
            "❌ Неизвестная команда.\nИспользуйте /menu для открытия главного меню.";
    public static final String MSG_NO_PARCELS = "📭 <b>У вас пока нет отслеживаемых посылок</b>\n\n"
            + "Нажмите «Отследить посылку» в главном меню, чтобы добавить первую посылку.";
    public static final String MSG_TRACKING_STOPPED =
            "⏸ <b>Отслеживание остановлено</b>\n\nПосылка: <code>%s</code>\n\n"
                    + "Чтобы возобновить отслеживание, нажмите «Возобновить отслеживание» в меню посылки.";
    public static final String MSG_TRACKING_RESUMED = "▶️ Отслеживание посылки <code>%s</code> возобновлено.";
    public static final String MSG_PARCEL_DELETED = "🗑 Посылка <code>%s</code> удалена из списка отслеживания.";
    public static final String MSG_DELETE_CONFIRM = "⚠️ <b>Подтверждение удаления</b>\n\n"
            + "Вы действительно хотите удалить посылку <code>%s</code> из списка отслеживания?\n\n"
            + "Вся история статусов будет потеряна.";
    public static final String MSG_ERROR_STOP_TRACKING = "❌ Ошибка при остановке отслеживания";
    public static final String MSG_ERROR_UPDATE = "❌ Ошибка обновления: %s";
    public static final String MSG_NOTIFICATIONS_TOGGLED = "🔔 Уведомления %s";
    public static final String MSG_NOTIFICATIONS_ENABLED = "включены ✅";
    public static final String MSG_NOTIFICATIONS_DISABLED = "выключены ❌";

    // =====================================================
    // ТЕКСТЫ ДЛЯ ИСТОРИИ СТАТУСОВ
    // =====================================================
    public static final String HISTORY_EMPTY = "📜 История статусов для посылки %s\n\nИстория пока отсутствует.";
    public static final String HISTORY_MORE_RECORDS = "\n<i>... и еще %d записей</i>";
    public static final String HISTORY_DATE_UNKNOWN = "дата неизвестна";
    public static final String HISTORY_SELECT_PARCEL = "👇 <b>Выберите посылку для просмотра деталей</b>";

    // =====================================================
    // ТЕКСТЫ ДЛЯ СТАТИСТИКИ
    // =====================================================
    public static final String STAT_ACTIVE_PARCELS = "📦 <b>Активных посылок:</b> %d\n";
    public static final String STAT_DELIVERED = "✅ <b>Доставлено:</b> %d\n";
    public static final String STAT_TOTAL_NOTIFICATIONS = "🔔 <b>Всего уведомлений:</b> %d\n";
    public static final String STAT_TRACKING_SINCE = "\n📅 <b>Отслеживаете с:</b> %s";

    // =====================================================
    // ТЕКСТЫ ДЛЯ НАСТРОЕК УВЕДОМЛЕНИЙ
    // =====================================================
    public static final String NOTIFICATIONS_CURRENT_STATUS = "Текущий статус: %s\n\n";
    public static final String NOTIFICATIONS_DESCRIPTION = "Уведомления приходят при изменении статуса ваших посылок.";

    // =====================================================
    // ТЕКСТЫ ДЛЯ ОТСЛЕЖИВАНИЯ
    // =====================================================
    public static final String TRACKING_REQUEST = "🔍 <b>Введите трек-номер посылки</b>\n\n%s\n\nИли нажмите ❌ Отмена";
    public static final String TRACKING_CHECKING = "🔍 Проверяю информацию по трек-номеру <b>%s</b>...";
    public static final String TRACKING_INVALID_FORMAT =
            "❌ <b>Неверный формат трек-номера</b>\n\n%s\n\nПопробуйте еще раз через /track";
    public static final String TRACKING_ADDED = "✅ <b>Посылка добавлена для отслеживания!</b>";
    public static final String TRACKING_INFO = "📦 <b>Информация о посылке</b>";
    public static final String TRACKING_UPDATING = "🔄 Обновляю статус для <code>%s</code>%s...";
    public static final String TRACKING_STATUS_CHANGED = "🔔 <b>Изменение статуса посылки!</b>\n\n";
    public static final String TRACKING_STATUS_OLD = "📌 <b>Было:</b> %s\n";
    public static final String TRACKING_STATUS_NEW = "📌 <b>Стало:</b> %s\n";
    public static final String TRACKING_DELIVERED = "\n🎉 <b>ПОСЫЛКА ДОСТАВЛЕНА!</b> 🎉";
    public static final String TRACKING_NO_CHANGE =
            "✅ Статус посылки <code>%s</code> не изменился.\n\n📌 Текущий статус: %s%s";
    public static final String TRACKING_NEW_RECORDS =
            "📊 Для посылки <code>%s</code> добавлены новые записи в историю, но статус не изменился.\n\nТекущий статус: %s";
    public static final String TRACKING_STATUS_UPDATED = "✅ <b>Статус обновлен</b>";

    // =====================================================
    // ИНФОРМАЦИЯ О КЕШЕ
    // =====================================================
    public static final String CACHE_INFO_FRESH = " (кеш актуален, возраст: %d сек)";
    public static final String CACHE_INFO_STALE = " (кеш устарел, возраст: %d сек)";
    public static final String CACHE_INFO_MISS = " (кеш отсутствует)";
    public static final String CACHE_FRESHNESS_HINT =
            "\n\n💡 Данные из кеша (возраст: %d сек). Чтобы получить свежие данные, попробуйте позже.";

    // =====================================================
    // ФОРМАТЫ ТРЕК-НОМЕРОВ
    // =====================================================
    public static final String TRACKING_NUMBER_EXAMPLES =
            "Примеры:\n• RA644000001RU (международный)\n• 12345678901234 (внутренний)";
    public static final String SUPPORTED_FORMATS =
            "Поддерживаемые форматы:\n• RA644000001RU (международный)\n• 12345678901234 (внутренний)";

    // =====================================================
    // ПАРАМЕТРЫ
    // =====================================================
    public static final int CACHE_FRESHNESS_SECONDS = 3600; // 1 час
    public static final int MAX_HISTORY_RECORDS = 15;

    // =====================================================
    // ШАБЛОНЫ ДЛЯ ФОРМАТИРОВАНИЯ
    // =====================================================
    public static final String FORMAT_PARCEL_ACTION =
            "<b>📦 %s</b>\n\n📋 Трек-номер: <code>%s</code>\n🚚 Служба: %s\n📌 Статус: %s\n\n<b>Выберите действие:</b>";
    public static final String FORMAT_STATUS_HISTORY_ITEM = "%s <b>%s</b>\n   📌 %s\n";
    public static final String FORMAT_STATUS_HISTORY_PLACE = "   📍 %s\n";

    // =====================================================
    // ПОЛНЫЕ ШАБЛОНЫ СООБЩЕНИЙ
    // =====================================================

    // Приветствие
    public static final String WELCOME_TEMPLATE =
            "👋 <b>Добро пожаловать, %s!</b>\n\n" + "Я бот для отслеживания посылок Почты России.\n\n"
                    + "📌 <b>Что я умею:</b>\n"
                    + "• Отслеживать посылки по трек-номеру\n"
                    + "• Присылать уведомления об изменении статуса\n"
                    + "• Хранить историю всех статусов\n"
                    + "• Показывать полную информацию о посылке\n\n"
                    + "Нажмите /menu, чтобы открыть главное меню.";

    // Помощь
    public static final String HELP_TEMPLATE = TITLE_HELP + "<b>📌 Основные возможности:</b>\n"
            + "• Отслеживание посылок Почты России\n"
            + "• Автоматические уведомления об изменениях\n"
            + "• История всех статусов\n"
            + "• Полная информация о посылке\n\n"
            + "<b>🎮 Как пользоваться:</b>\n"
            + "1. Нажмите «Отследить посылку»\n"
            + "2. Введите трек-номер\n"
            + "3. Бот покажет статус и добавит в список\n\n"
            + "<b>📋 Форматы трек-номеров:</b>\n"
            + "%s\n\n"
            + "<b>🔔 Уведомления:</b>\n"
            + "Бот проверяет статусы каждый час и присылает уведомления при изменениях.\n\n"
            + "По вопросам и предложениям: %s";

    // Технологии по умолчанию
    public static final String DEFAULT_TECHNOLOGIES = "• Java 21\n" + "• Spring Boot 3\n"
            + "• Telegram Bot API\n"
            + "• PostgreSQL\n"
            + "• SOAP API Почты России\n"
            + "• Caffeine Cache\n"
            + "• Lombok";
}
