package ru.otus.trackingbot.bot;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.bot.keyboard.KeyboardFactory;
import ru.otus.trackingbot.config.BotInfoConfig;
import ru.otus.trackingbot.constant.BotConstants;
import ru.otus.trackingbot.entity.*;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.*;
import ru.otus.trackingbot.util.DateUtils;
import ru.otus.trackingbot.util.MessageFormatter;
import ru.otus.trackingbot.util.StatusEmojiUtils;
import ru.otus.trackingbot.util.TrackingNumberUtils;

/**
 * Основной класс Telegram бота для отслеживания посылок.
 */
@Component
@Slf4j
public class TrackingBot extends TelegramLongPollingBot {

    private final String botUsername;

    private final UserService userService;
    private final ParcelService parcelService;
    private final UserParcelService userParcelService;
    private final TrackingCacheService trackingCacheService;
    private final TrackingServiceFactory trackingServiceFactory;
    private final KeyboardFactory keyboardFactory;
    private final BotInfoConfig botInfoConfig;

    /**
     * Хранилище состояния ожидания ввода трек-номера для каждого пользователя.
     */
    private final Map<Long, Boolean> waitingForTrackingNumber = new ConcurrentHashMap<>();

    /**
     * Флаг для отключения регистрации команд при тестировании
     */
    private final boolean skipCommandRegistration;

    /**
     * Основной конструктор для Spring.
     */
    public TrackingBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UserService userService,
            ParcelService parcelService,
            UserParcelService userParcelService,
            TrackingCacheService trackingCacheService,
            TrackingServiceFactory trackingServiceFactory,
            KeyboardFactory keyboardFactory,
            BotInfoConfig botInfoConfig) {
        super(botToken);
        this.botUsername = botUsername;
        this.userService = userService;
        this.parcelService = parcelService;
        this.userParcelService = userParcelService;
        this.trackingCacheService = trackingCacheService;
        this.trackingServiceFactory = trackingServiceFactory;
        this.keyboardFactory = keyboardFactory;
        this.botInfoConfig = botInfoConfig;
        this.skipCommandRegistration = false;
        registerCommands();
    }

    /**
     * Конструктор для тестирования - позволяет отключить регистрацию команд.
     */
    protected TrackingBot(
            String botToken,
            String botUsername,
            boolean skipCommandRegistration,
            UserService userService,
            ParcelService parcelService,
            UserParcelService userParcelService,
            TrackingCacheService trackingCacheService,
            TrackingServiceFactory trackingServiceFactory,
            KeyboardFactory keyboardFactory,
            BotInfoConfig botInfoConfig) {
        super(botToken);
        this.botUsername = botUsername;
        this.userService = userService;
        this.parcelService = parcelService;
        this.userParcelService = userParcelService;
        this.trackingCacheService = trackingCacheService;
        this.trackingServiceFactory = trackingServiceFactory;
        this.keyboardFactory = keyboardFactory;
        this.botInfoConfig = botInfoConfig;
        this.skipCommandRegistration = skipCommandRegistration;
        if (!skipCommandRegistration) {
            registerCommands();
        }
    }

    private void registerCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand(BotConstants.CMD_START.substring(1), "Запустить бота"));
        commands.add(new BotCommand(BotConstants.CMD_MENU.substring(1), "Показать главное меню"));
        commands.add(new BotCommand(BotConstants.CMD_TRACK.substring(1), "Отследить посылку"));
        commands.add(new BotCommand(BotConstants.CMD_LIST.substring(1), "Мои посылки"));
        commands.add(new BotCommand(BotConstants.CMD_HELP.substring(1), "Помощь"));

        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка при регистрации команд", e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    // =====================================================
    // ОБРАБОТКА ТЕКСТОВЫХ СООБЩЕНИЙ
    // =====================================================

    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        String userName = update.getMessage().getFrom().getFirstName();
        String userLastName = update.getMessage().getFrom().getLastName();
        String username = update.getMessage().getFrom().getUserName();

        User user = userService.getOrCreateUser(chatId, username, userName, userLastName);
        userService.updateLastActivity(chatId);

        log.info("Получено сообщение от {} ({}): {}", userName, chatId, messageText);

        if (waitingForTrackingNumber.getOrDefault(chatId, false)) {
            waitingForTrackingNumber.remove(chatId);
            processTrackingNumber(messageText, chatId, user);
        } else {
            processCommand(messageText, chatId);
        }
    }

    private void processCommand(String command, long chatId) {
        switch (command) {
            case BotConstants.CMD_START:
                sendWelcomeMessage(chatId);
                break;
            case BotConstants.CMD_MENU:
            case BotConstants.BTN_BACK_TO_MENU:
                sendMainMenu(chatId);
                break;
            case BotConstants.CMD_TRACK:
            case BotConstants.BTN_TRACK_PARCEL:
                requestTrackingNumber(chatId);
                break;
            case BotConstants.CMD_LIST:
            case BotConstants.BTN_MY_PARCELS:
                showParcelsList(chatId);
                break;
            case BotConstants.CMD_HELP:
            case BotConstants.BTN_HELP:
                sendHelp(chatId);
                break;
            case BotConstants.BTN_STATISTICS:
                showStats(chatId);
                break;
            case BotConstants.BTN_NOTIFICATIONS:
                showNotificationsSettings(chatId);
                break;
            case BotConstants.BTN_ABOUT:
                sendAbout(chatId);
                break;
            default:
                sendMessage(chatId, BotConstants.MSG_UNKNOWN_COMMAND);
        }
    }

    // =====================================================
    // ОБРАБОТКА CALLBACK ЗАПРОСОВ
    // =====================================================

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        log.info("Получен callback: {} от пользователя {}", callbackData, chatId);

        // Удаляем сообщение с кнопками после обработки
        deleteMessageSafely(chatId, messageId);

        int lastUnderscoreIndex = callbackData.lastIndexOf('_');

        if (lastUnderscoreIndex == -1) {
            handleSimpleCallback(callbackData, chatId);
            return;
        }

        String idStr = callbackData.substring(lastUnderscoreIndex + 1);
        Long parcelId;
        try {
            parcelId = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            log.debug("Не удалось распарсить ID из callback: {}, обрабатываем как простой callback", callbackData);
            handleSimpleCallback(callbackData, chatId);
            return;
        }

        String action = callbackData.substring(0, lastUnderscoreIndex);
        handleParcelCallback(action, parcelId, chatId);
    }

    private void handleSimpleCallback(String callbackData, long chatId) {
        switch (callbackData) {
            case BotConstants.CALLBACK_BACK_TO_MENU:
                sendMainMenu(chatId);
                break;
            case BotConstants.CALLBACK_BACK_TO_PARCELS:
                showParcelsList(chatId);
                break;
            case BotConstants.CALLBACK_NOTIFICATIONS_ON:
                toggleNotifications(chatId, true);
                break;
            case BotConstants.CALLBACK_NOTIFICATIONS_OFF:
                toggleNotifications(chatId, false);
                break;
            default:
                log.warn("Неизвестный callback: {}", callbackData);
                sendMainMenu(chatId);
        }
    }

    private void handleParcelCallback(String action, Long parcelId, long chatId) {
        switch (action) {
            case BotConstants.PREFIX_PARCEL:
                showParcelActions(chatId, parcelId);
                break;
            case BotConstants.PREFIX_PARCEL_INFO:
                showParcelInfo(chatId, parcelId);
                break;
            case BotConstants.PREFIX_PARCEL_HISTORY:
                showParcelHistory(chatId, parcelId);
                break;
            case BotConstants.PREFIX_PARCEL_UPDATE:
                updateParcelStatus(chatId, parcelId);
                break;
            case BotConstants.PREFIX_PARCEL_STOP:
                stopParcelTracking(chatId, parcelId);
                break;
            case BotConstants.PREFIX_PARCEL_RESUME:
                resumeParcelTracking(chatId, parcelId);
                break;
            case BotConstants.PREFIX_PARCEL_DELETE:
                deleteParcel(chatId, parcelId);
                break;
            case BotConstants.PREFIX_PARCEL_DELETE_CONFIRM:
                confirmDeleteParcel(chatId, parcelId);
                break;
            default:
                log.warn("Неизвестное действие: {}", action);
                sendMainMenu(chatId);
        }
    }

    // =====================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================

    private User getUser(long chatId) {
        return userService.getOrCreateUser(chatId, null, null, null);
    }

    private void sendMessageSafely(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения пользователю {}", message.getChatId(), e);
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboardFactory.getMainKeyboard());
        sendMessageSafely(message);
    }

    private void sendMessage(long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboard);
        sendMessageSafely(message);
    }

    private void sendInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboard);
        sendMessageSafely(message);
    }

    private void sendMainMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(BotConstants.TITLE_MAIN_MENU);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboardFactory.getMainKeyboard());
        sendMessageSafely(message);
    }

    private void deleteMessageSafely(long chatId, int messageId) {
        try {
            execute(new DeleteMessage(String.valueOf(chatId), messageId));
        } catch (TelegramApiException e) {
            log.error("Ошибка при удалении сообщения", e);
        }
    }

    // =====================================================
    // ОСНОВНЫЕ МЕТОДЫ БОТА
    // =====================================================

    private void sendWelcomeMessage(long chatId) {
        User user = getUser(chatId);
        String welcome = String.format(
                BotConstants.WELCOME_TEMPLATE, user.getFirstName() != null ? user.getFirstName() : "пользователь");
        sendMessage(chatId, welcome);
    }

    private void requestTrackingNumber(long chatId) {
        waitingForTrackingNumber.put(chatId, true);
        String text = String.format(BotConstants.TRACKING_REQUEST, BotConstants.TRACKING_NUMBER_EXAMPLES);
        sendMessage(chatId, text, keyboardFactory.getCancelKeyboard());
    }

    private void processTrackingNumber(String trackingNumber, long chatId, User user) {
        String cleanNumber = TrackingNumberUtils.clean(trackingNumber);

        Optional<AbstractTrackingService> serviceOpt = trackingServiceFactory.detectService(cleanNumber);

        if (serviceOpt.isEmpty()) {
            sendMessage(chatId, String.format(BotConstants.TRACKING_INVALID_FORMAT, BotConstants.SUPPORTED_FORMATS));
            return;
        }

        sendMessage(chatId, String.format(BotConstants.TRACKING_CHECKING, cleanNumber));

        // При добавлении новой посылки всегда получаем свежие данные (кеша нет)
        TrackingInfo info = trackingCacheService.getTrackingInfo(cleanNumber);

        if (info.isSuccess()) {
            Parcel parcel = parcelService.getOrCreateParcel(cleanNumber, info.getServiceName());
            parcelService.updateParcelStatus(parcel, info);

            Optional<UserParcel> existing = userParcelService.findByUserAndTrackingNumber(user, cleanNumber);

            if (existing.isEmpty() || !existing.get().getIsActive()) {
                UserParcel userParcel = userParcelService.addParcelForUser(user, parcel, null);
                userParcel.setLastStatus(info.getStatus());
                userParcel.setLastStatusDescription(info.getStatusDescription());
                userParcel.setLastChecked(LocalDateTime.now());
                userParcelService.updateUserParcelStatus(userParcel, info);

                sendMessage(chatId, MessageFormatter.formatTrackingInfo(info, BotConstants.TRACKING_ADDED));
            } else {
                UserParcel userParcel = existing.get();
                userParcel.setLastStatus(info.getStatus());
                userParcel.setLastStatusDescription(info.getStatusDescription());
                userParcel.setLastChecked(LocalDateTime.now());
                userParcelService.updateUserParcelStatus(userParcel, info);

                sendMessage(chatId, MessageFormatter.formatTrackingInfo(info, BotConstants.TRACKING_INFO));
            }
        } else {
            sendMessage(chatId, "❌ " + info.getError());
        }
    }

    private void showParcelsList(long chatId) {
        User user = getUser(chatId);
        List<UserParcel> userParcels = userParcelService.getAllUserParcelsWithDetails(user);

        if (userParcels.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_NO_PARCELS);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(BotConstants.TITLE_MY_PARCELS);

            int activeCount = 0;
            int inactiveCount = 0;

            for (int i = 0; i < userParcels.size(); i++) {
                UserParcel up = userParcels.get(i);
                Parcel parcel = up.getParcel();
                String displayName = up.getCustomName() != null ? up.getCustomName() : parcel.getTrackingNumber();

                String status = up.getLastStatus();
                if (status == null || status.isEmpty()) {
                    status = "Статус неизвестен";
                }

                String statusEmoji = StatusEmojiUtils.getEmoji(status);

                // Добавляем индикатор активности
                String activeIndicator = up.getIsActive() ? "🟢" : "⏸️";

                sb.append(String.format(
                        "%d. %s %s %s\n   📌 %s\n\n", i + 1, activeIndicator, statusEmoji, displayName, status));

                if (up.getIsActive()) {
                    activeCount++;
                } else {
                    inactiveCount++;
                }
            }

            // Добавляем легенду
            sb.append("\n<b>Легенда:</b>\n");
            sb.append("🟢 - Активно отслеживается\n");
            sb.append("⏸️ - Отслеживание остановлено\n\n");
            sb.append("📊 <b>Итого:</b> ");
            sb.append(activeCount).append(" активных, ");
            sb.append(inactiveCount).append(" остановленных\n\n");

            sb.append(BotConstants.HISTORY_SELECT_PARCEL);

            // Используем клавиатуру со всеми посылками
            sendMessage(chatId, sb.toString(), keyboardFactory.getParcelsKeyboard(userParcels));
        }
    }

    private void showParcelActions(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        UserParcel userParcel = userParcelOpt.get();
        Parcel parcel = userParcel.getParcel();
        String displayName =
                userParcel.getCustomName() != null ? userParcel.getCustomName() : parcel.getTrackingNumber();

        String status = userParcel.getLastStatus();
        if (status == null || status.isEmpty()) {
            status = "Статус неизвестен";
        }

        String text = String.format(
                BotConstants.FORMAT_PARCEL_ACTION,
                displayName,
                parcel.getTrackingNumber(),
                parcel.getServiceName(),
                status);

        sendInlineKeyboard(chatId, text, keyboardFactory.getParcelActionsKeyboard(parcelId, userParcel.getIsActive()));
    }

    private void showParcelInfo(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        UserParcel userParcel = userParcelOpt.get();
        Parcel parcel = userParcel.getParcel();
        ParcelStatusHistory lastStatus = parcelService.getLastParcelStatus(parcel);

        String text = MessageFormatter.formatParcelInfoFromDB(userParcel, parcel, lastStatus);
        sendInlineKeyboard(chatId, text, keyboardFactory.getBackToParcelKeyboard(parcelId));
    }

    private void showParcelHistory(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        Parcel parcel = userParcelOpt.get().getParcel();
        List<ParcelStatusHistory> history = parcelService.getParcelHistory(parcel);

        if (history.isEmpty()) {
            String text = String.format(BotConstants.HISTORY_EMPTY, parcel.getTrackingNumber());
            sendInlineKeyboard(chatId, text, keyboardFactory.getBackToParcelKeyboard(parcelId));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(BotConstants.TITLE_STATUS_HISTORY);
        sb.append(String.format("<b>Посылка:</b> <code>%s</code>\n\n", parcel.getTrackingNumber()));

        int count = 0;
        for (ParcelStatusHistory status : history) {
            if (count++ >= BotConstants.MAX_HISTORY_RECORDS) {
                sb.append(String.format(
                        BotConstants.HISTORY_MORE_RECORDS, history.size() - BotConstants.MAX_HISTORY_RECORDS));
                break;
            }

            String emoji = StatusEmojiUtils.getEmoji(status.getStatusName());
            String dateStr = status.getOperationDate() != null
                    ? DateUtils.format(status.getOperationDate())
                    : BotConstants.HISTORY_DATE_UNKNOWN;
            sb.append(String.format(BotConstants.FORMAT_STATUS_HISTORY_ITEM, emoji, dateStr, status.getStatusName()));
            if (status.getOperationPlace() != null
                    && !status.getOperationPlace().isEmpty()) {
                sb.append(String.format(BotConstants.FORMAT_STATUS_HISTORY_PLACE, status.getOperationPlace()));
            }
            sb.append("\n");
        }

        sendInlineKeyboard(chatId, sb.toString(), keyboardFactory.getBackFromHistoryKeyboard(parcelId));
    }

    private void updateParcelStatus(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        UserParcel userParcel = userParcelOpt.get();
        String trackingNumber = userParcel.getParcel().getTrackingNumber();
        String oldStatus = userParcel.getLastStatus();

        TrackingInfo info = trackingCacheService.getTrackingInfoWithFreshnessCheck(
                trackingNumber, BotConstants.CACHE_FRESHNESS_SECONDS);

        if (!info.isSuccess()) {
            sendMessage(chatId, String.format(BotConstants.MSG_ERROR_UPDATE, info.getError()));
            return;
        }

        boolean hasUpdates = parcelService.updateParcelStatus(userParcel.getParcel(), info);

        String newStatus = info.getStatus();
        boolean statusChanged =
                (oldStatus == null && newStatus != null) || (oldStatus != null && !oldStatus.equals(newStatus));

        if (statusChanged) {
            userParcel.setLastStatus(newStatus);
            userParcel.setLastStatusDescription(info.getStatusDescription());
            userParcel.setLastChecked(LocalDateTime.now());
            userParcelService.updateUserParcelStatus(userParcel, info);

            log.info("Статус изменен для {}: {} -> {}", trackingNumber, oldStatus, newStatus);

            String notificationText =
                    MessageFormatter.formatStatusChangeNotification(userParcel, info, oldStatus, newStatus);
            sendMessage(chatId, notificationText);
        } else if (hasUpdates) {
            userParcel.setLastChecked(LocalDateTime.now());
            userParcelService.updateUserParcelStatus(userParcel, info);
            String message = String.format(BotConstants.TRACKING_NEW_RECORDS, trackingNumber, newStatus);
            sendMessage(chatId, message);
        } else {
            userParcel.setLastChecked(LocalDateTime.now());
            userParcelService.updateUserParcelStatus(userParcel, info);
            String message = String.format(BotConstants.TRACKING_NO_CHANGE, trackingNumber, newStatus, "");
            sendMessage(chatId, message);
        }

        String text = MessageFormatter.formatTrackingInfo(info, BotConstants.TRACKING_STATUS_UPDATED);
        sendInlineKeyboard(chatId, text, keyboardFactory.getBackToParcelKeyboard(parcelId));
    }

    private void stopParcelTracking(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        UserParcel userParcel = userParcelOpt.get();
        String trackingNumber = userParcel.getParcel().getTrackingNumber();
        String displayName = userParcel.getCustomName() != null ? userParcel.getCustomName() : trackingNumber;

        boolean stopped = userParcelService.stopTracking(user, trackingNumber);

        if (stopped) {
            // Обновляем статус в объекте (для отображения актуального состояния)
            userParcel.setIsActive(false);

            // Показываем обновленное меню посылки с кнопкой "Возобновить"
            String text = String.format(
                    BotConstants.FORMAT_PARCEL_ACTION,
                    displayName,
                    trackingNumber,
                    userParcel.getParcel().getServiceName(),
                    userParcel.getLastStatus() != null ? userParcel.getLastStatus() : "Статус неизвестен");

            sendInlineKeyboard(chatId, text, keyboardFactory.getParcelActionsKeyboard(parcelId, false));
        } else {
            sendMessage(chatId, BotConstants.MSG_ERROR_STOP_TRACKING);
        }
    }

    private void resumeParcelTracking(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        UserParcel userParcel = userParcelOpt.get();
        String trackingNumber = userParcel.getParcel().getTrackingNumber();
        String displayName = userParcel.getCustomName() != null ? userParcel.getCustomName() : trackingNumber;

        userParcel.setIsActive(true);
        userParcel.setLastChecked(LocalDateTime.now());
        userParcelService.updateUserParcelStatus(userParcel, null);

        // Показываем обновленное меню посылки с кнопкой "Остановить"
        String text = String.format(
                BotConstants.FORMAT_PARCEL_ACTION,
                displayName,
                trackingNumber,
                userParcel.getParcel().getServiceName(),
                userParcel.getLastStatus() != null ? userParcel.getLastStatus() : "Статус неизвестен");

        sendInlineKeyboard(chatId, text, keyboardFactory.getParcelActionsKeyboard(parcelId, true));
    }

    private void deleteParcel(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        UserParcel userParcel = userParcelOpt.get();
        String displayName = userParcel.getCustomName() != null
                ? userParcel.getCustomName()
                : userParcel.getParcel().getTrackingNumber();

        String text = String.format(BotConstants.MSG_DELETE_CONFIRM, displayName);
        sendInlineKeyboard(chatId, text, keyboardFactory.getConfirmDeleteKeyboard(parcelId));
    }

    private void confirmDeleteParcel(long chatId, Long parcelId) {
        User user = getUser(chatId);
        Optional<UserParcel> userParcelOpt = userParcelService.findByIdWithDetails(parcelId, user);

        if (userParcelOpt.isEmpty()) {
            sendMessage(chatId, BotConstants.MSG_PARCEL_NOT_FOUND);
            return;
        }

        UserParcel userParcel = userParcelOpt.get();
        String trackingNumber = userParcel.getParcel().getTrackingNumber();

        userParcelService.deleteUserParcel(userParcel);

        String text = String.format(BotConstants.MSG_PARCEL_DELETED, trackingNumber);
        sendMessage(chatId, text);
    }

    private void showStats(long chatId) {
        User user = getUser(chatId);
        List<UserParcel> userParcels = userParcelService.getActiveUserParcels(user);
        long activeCount = userParcels.size();
        long deliveredCount = userParcels.stream()
                .filter(up -> up.getLastStatus() != null
                        && (up.getLastStatus().toLowerCase().contains("доставлен")
                                || up.getLastStatus().toLowerCase().contains("вручен")))
                .count();

        StringBuilder sb = new StringBuilder();
        sb.append(BotConstants.TITLE_STATISTICS);
        sb.append(String.format(BotConstants.STAT_ACTIVE_PARCELS, activeCount));
        sb.append(String.format(BotConstants.STAT_DELIVERED, deliveredCount));

        if (activeCount > 0) {
            long totalNotifications = userParcels.stream()
                    .mapToInt(UserParcel::getNotificationCount)
                    .sum();
            sb.append(String.format(BotConstants.STAT_TOTAL_NOTIFICATIONS, totalNotifications));

            Optional<UserParcel> oldest = userParcels.stream().min(Comparator.comparing(UserParcel::getAddedAt));
            if (oldest.isPresent()) {
                sb.append(String.format(
                        BotConstants.STAT_TRACKING_SINCE,
                        DateUtils.format(oldest.get().getAddedAt())));
            }
        }

        sendMessage(chatId, sb.toString());
    }

    private void showNotificationsSettings(long chatId) {
        User user = getUser(chatId);
        String status = user.getNotificationEnabled()
                ? BotConstants.MSG_NOTIFICATIONS_ENABLED
                : BotConstants.MSG_NOTIFICATIONS_DISABLED;
        String text = BotConstants.TITLE_NOTIFICATIONS
                + String.format(BotConstants.NOTIFICATIONS_CURRENT_STATUS, status)
                + BotConstants.NOTIFICATIONS_DESCRIPTION;

        sendMessage(chatId, text, keyboardFactory.getNotificationsKeyboard(user.getNotificationEnabled()));
    }

    private void toggleNotifications(long chatId, boolean enable) {
        userService.toggleNotifications(chatId, enable);
        String status = enable ? BotConstants.MSG_NOTIFICATIONS_ENABLED : BotConstants.MSG_NOTIFICATIONS_DISABLED;
        sendMessage(chatId, String.format(BotConstants.MSG_NOTIFICATIONS_TOGGLED, status));
    }

    private void sendHelp(long chatId) {
        String help =
                String.format(BotConstants.HELP_TEMPLATE, BotConstants.SUPPORTED_FORMATS, botInfoConfig.getSupport());
        sendMessage(chatId, help);
    }

    private void sendAbout(long chatId) {
        String technologiesStr;
        if (botInfoConfig.getTechnologies() != null
                && !botInfoConfig.getTechnologies().isEmpty()) {
            StringBuilder techBuilder = new StringBuilder();
            for (String tech : botInfoConfig.getTechnologies()) {
                techBuilder.append("• ").append(tech).append("\n");
            }
            technologiesStr = techBuilder.toString();
        } else {
            technologiesStr = BotConstants.DEFAULT_TECHNOLOGIES;
        }

        String websiteStr = "";
        if (botInfoConfig.getWebsite() != null && !botInfoConfig.getWebsite().isEmpty()) {
            websiteStr = "🌐 " + botInfoConfig.getWebsite() + "\n";
        }

        String about = BotConstants.TITLE_ABOUT
                + String.format("Версия: %s\n", botInfoConfig.getVersion())
                + String.format("Разработчик: %s\n\n", botInfoConfig.getDeveloper())
                + "<b>Технологии:</b>\n" + technologiesStr + "\n"
                + botInfoConfig.getCopyright() + "\n\n"
                + websiteStr
                + "✉️ Поддержка: " + botInfoConfig.getSupport();

        sendMessage(chatId, about);
    }
}
