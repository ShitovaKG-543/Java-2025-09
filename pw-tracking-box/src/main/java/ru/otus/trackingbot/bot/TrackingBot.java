package ru.otus.trackingbot.bot;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.config.BotConfig;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.CdekTrackingService;
import ru.otus.trackingbot.service.RussianPostTrackingService;
import ru.otus.trackingbot.service.TrackingService;
import ru.otus.trackingbot.util.TrackNumberValidator;

public class TrackingBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final List<TrackingService> trackingServices;
    private final DateTimeFormatter dateFormatter;
    private final int maxHistoryEvents;

    public TrackingBot(BotConfig config) {
        this.config = config;
        this.trackingServices = new ArrayList<>();
        this.dateFormatter = DateTimeFormatter.ofPattern(config.getDateFormat());
        this.maxHistoryEvents = config.getMaxHistoryEvents();

        // Инициализация сервисов отслеживания (только если есть ключи)
        if (config.isCdekEnabled()) {
            trackingServices.add(new CdekTrackingService(config));
            System.out.println("✅ Сервис СДЭК активирован");
        } else {
            System.out.println("⚠️ Сервис СДЭК отключен (нет API ключей)");
        }

        if (config.isRussianPostEnabled()) {
            trackingServices.add(new RussianPostTrackingService(config));
            System.out.println("✅ Сервис Почта России активирован");
        } else {
            System.out.println("⚠️ Сервис Почта России отключен (нет API ключей)");
        }

        System.out.println("🤖 Бот инициализирован. Активных сервисов: " + trackingServices.size());
    }

    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getFrom().getFirstName();

            System.out.println("📨 [" + config.getDateFormat() + "] Сообщение от " + firstName + " (chatId: " + chatId
                    + "): " + messageText);

            if (messageText.startsWith("/")) {
                handleCommand(chatId, messageText);
            } else {
                if (TrackNumberValidator.isValidTrackNumber(messageText)) {
                    handleTrackNumber(chatId, messageText);
                } else {
                    sendMessage(
                            chatId,
                            "❓ Я не понимаю эту команду.\n\n" + "Отправьте трек-номер посылки или используйте /help",
                            createMainKeyboard());
                }
            }
        }
    }

    private void handleCommand(long chatId, String command) {
        switch (command) {
            case "/start":
                sendStartMessage(chatId);
                break;
            case "/help":
                sendHelpMessage(chatId);
                break;
            case "/about":
                sendAboutMessage(chatId);
                break;
            case "/status":
                sendStatusMessage(chatId);
                break;
            default:
                sendMessage(chatId, "❌ Неизвестная команда. Используйте /help", null);
        }
    }

    private void sendStartMessage(long chatId) {
        String services = getServicesString();
        String welcomeText =
                "📦 *Добро пожаловать в бот отслеживания посылок!*\n\n" + "Я помогу вам отследить посылки:\n"
                        + services
                        + "\n" + "*Как пользоваться:*\n"
                        + "Просто отправьте мне трек-номер посылки, и я покажу текущий статус.\n\n"
                        + "*Примеры трек-номеров:*\n"
                        + "• СДЭК: `1234567890`\n"
                        + "• Почта России: `12345678901234`\n"
                        + "• Международный: `RA123456789RU`\n\n"
                        + "*Доступные команды:*\n"
                        + "/help - подробная помощь\n"
                        + "/about - информация о боте\n"
                        + "/status - статус сервисов";

        sendMessage(chatId, welcomeText, createMainKeyboard());
    }

    private void sendHelpMessage(long chatId) {
        String helpText = "📋 *Как пользоваться ботом:*\n\n" + "*1. Получите трек-номер*\n"
                + "Трек-номер можно найти в чеке или в личном кабинете интернет-магазина.\n\n"
                + "*2. Отправьте номер боту*\n"
                + "Просто введите трек-номер в чат одним сообщением.\n\n"
                + "*3. Получите информацию*\n"
                + "Бот определит перевозчика и покажет:\n"
                + "• Текущий статус\n"
                + "• Местоположение\n"
                + "• Историю перемещений (до "
                + maxHistoryEvents + " событий)\n" + "• Маршрут отправления\n\n"
                + "*Поддерживаемые форматы:*\n"
                + "• 🔢 СДЭК: 10-20 цифр\n"
                + "• 🔢 Почта России: 13-14 цифр\n"
                + "• 🔤 Международные: RA123456789RU\n\n"
                + getServicesStatus();

        sendMessage(chatId, helpText, createMainKeyboard());
    }

    private void sendAboutMessage(long chatId) {
        String aboutText = "ℹ️ *О боте*\n\n" + "*Версия:* 1.0.0\n"
                + "*Разработчик:* Tracking Bot\n"
                + "*Описание:* Бот для отслеживания посылок\n"
                + "через API СДЭК и Почты России\n\n"
                + "*📌 Особенности:*\n"
                + "• Автоматическое определение перевозчика\n"
                + "• Детальная информация о посылке\n"
                + "• История статусов\n"
                + "• Информация о маршруте\n\n"
                + "*⚙️ Текущие настройки:*\n"
                + "• Язык: "
                + config.getLanguage() + "\n" + "• Формат даты: "
                + config.getDateFormat() + "\n" + "• Макс. событий: "
                + maxHistoryEvents + "\n\n" + "*🔧 Технологии:*\n"
                + "• Java 21\n"
                + "• Gradle Kotlin DSL\n"
                + "• Telegram Bots API\n"
                + "• OkHttp\n"
                + "• Gson\n"
                + "• SnakeYAML";

        sendMessage(chatId, aboutText, createMainKeyboard());
    }

    private void sendStatusMessage(long chatId) {
        String status = "📊 *Статус сервисов*\n\n" + getServicesStatus()
                + "\n\n" + "*Форматы номеров:*\n"
                + "✅ СДЭК: "
                + (config.isCdekEnabled() ? "активен" : "отключен") + "\n" + "✅ Почта России: "
                + (config.isRussianPostEnabled() ? "активен" : "отключен") + "\n"
                + "✅ Международные: поддерживаются\n\n"
                + "*Настройки:*\n"
                + "• Язык: "
                + config.getLanguage() + "\n" + "• Формат даты: "
                + config.getDateFormat() + "\n" + "• Макс. событий: "
                + maxHistoryEvents;

        sendMessage(chatId, status, createMainKeyboard());
    }

    private String getServicesString() {
        StringBuilder services = new StringBuilder();
        if (config.isCdekEnabled()) services.append("• 🚚 СДЭК\n");
        if (config.isRussianPostEnabled()) services.append("• ✉️ Почта России\n");
        if (!config.isCdekEnabled() && !config.isRussianPostEnabled()) {
            services.append("• ❌ Сервисы не активированы\n");
        }
        return services.toString();
    }

    private String getServicesStatus() {
        StringBuilder status = new StringBuilder("*Доступные сервисы:*\n");
        status.append("• 🚚 СДЭК: ").append(config.isCdekEnabled() ? "✅" : "❌").append("\n");
        status.append("• ✉️ Почта России: ")
                .append(config.isRussianPostEnabled() ? "✅" : "❌")
                .append("\n");

        if (!config.isCdekEnabled() || !config.isRussianPostEnabled()) {
            status.append("\n*Для активации:*\n");
            status.append("Укажите API ключи в application.yml");
        }
        return status.toString();
    }

    private void handleTrackNumber(long chatId, String trackNumber) {
        String cleanTrackNumber = TrackNumberValidator.cleanTrackNumber(trackNumber);
        sendMessage(chatId, "🔍 *Проверяю трек-номер:* `" + cleanTrackNumber + "`", null);

        String carrier = TrackNumberValidator.detectCarrier(cleanTrackNumber);

        if (carrier.equals("INVALID")) {
            sendMessage(
                    chatId,
                    "❌ *Неверный формат трек-номера!*\n\n" + "*Проверьте формат:*\n"
                            + "• СДЭК: только цифры (10-20 символов)\n"
                            + "• Почта России: 13-14 цифр\n"
                            + "• Международные: RA123456789RU\n\n"
                            + "Используйте /help для подробной информации.",
                    createMainKeyboard());
            return;
        }

        if (trackingServices.isEmpty()) {
            sendMessage(
                    chatId,
                    "❌ *Нет активных сервисов отслеживания*\n\n" + "Администратор не настроил API ключи.\n"
                            + "Попробуйте позже или обратитесь к администратору.",
                    createMainKeyboard());
            return;
        }

        TrackingService selectedService = null;
        for (TrackingService service : trackingServices) {
            if (service.canHandle(cleanTrackNumber)) {
                selectedService = service;
                break;
            }
        }

        if (selectedService == null) {
            trackWithMultipleServices(chatId, cleanTrackNumber);
        } else {
            trackWithService(chatId, cleanTrackNumber, selectedService);
        }
    }

    private void trackWithMultipleServices(long chatId, String trackNumber) {
        sendMessage(chatId, "🔄 *Пробую проверить во всех активных службах...*", null);

        for (TrackingService service : trackingServices) {
            try {
                TrackingInfo info = service.track(trackNumber);

                if (info.isSuccess()
                        && info.getStatus() != null
                        && !info.getStatus().isEmpty()
                        && !info.getStatus().equals("Информация отсутствует")) {
                    sendTrackingInfo(chatId, info);
                    return;
                }
            } catch (Exception e) {
                System.err.println("Ошибка при проверке в " + service.getServiceName() + ": " + e.getMessage());
            }
        }

        sendMessage(
                chatId,
                "❌ *Не удалось найти информацию о посылке*\n\n" + "*Возможные причины:*\n"
                        + "• Неверный трек-номер\n"
                        + "• Посылка еще не зарегистрирована\n"
                        + "• Проблемы с API службы доставки",
                createMainKeyboard());
    }

    private void trackWithService(long chatId, String trackNumber, TrackingService service) {
        sendMessage(chatId, "📡 *Запрашиваю информацию из " + service.getServiceName() + "...*", null);

        try {
            TrackingInfo info = service.track(trackNumber);

            if (info.isSuccess() && info.getStatus() != null) {
                sendTrackingInfo(chatId, info);
            } else {
                String errorMsg = info.getErrorMessage() != null ? info.getErrorMessage() : "Неизвестная ошибка";
                sendMessage(
                        chatId, "❌ *" + errorMsg + "*\n\nПопробуйте другой трек-номер или /help", createMainKeyboard());
            }
        } catch (Exception e) {
            sendMessage(chatId, "❌ *Ошибка при обращении к API:* " + e.getMessage(), createMainKeyboard());
            e.printStackTrace();
        }
    }

    private void sendTrackingInfo(long chatId, TrackingInfo info) {
        StringBuilder response = new StringBuilder();

        response.append("📦 *Информация о посылке*\n");
        response.append("━━━━━━━━━━━━━━━━\n\n");

        response.append("🔢 *Трек-номер:* `").append(info.getTrackNumber()).append("`\n");
        response.append("🚚 *Перевозчик:* ").append(info.getCarrier()).append("\n\n");

        response.append("📍 *Текущий статус:*\n");
        response.append("➡️ ").append(info.getStatus()).append("\n\n");

        if (info.getDescription() != null
                && !info.getDescription().isEmpty()
                && !info.getDescription().equals(info.getStatus())) {
            response.append("📝 ").append(info.getDescription()).append("\n\n");
        }

        if (info.getFromLocation() != null || info.getToLocation() != null) {
            response.append("🗺 *Маршрут:*\n");
            if (info.getFromLocation() != null && !info.getFromLocation().isEmpty()) {
                response.append("📤 Откуда: ").append(info.getFromLocation()).append("\n");
            }
            if (info.getToLocation() != null && !info.getToLocation().isEmpty()) {
                response.append("📥 Куда: ").append(info.getToLocation()).append("\n");
            }
            response.append("\n");
        }

        if (info.getLastUpdate() != null) {
            response.append("⏱ *Последнее обновление:*\n");
            response.append("   ")
                    .append(info.getLastUpdate().format(dateFormatter))
                    .append("\n\n");
        }

        if (!info.getHistory().isEmpty()) {
            response.append("📋 *История отслеживания (последние ")
                    .append(maxHistoryEvents)
                    .append("):*\n");

            int start = Math.max(0, info.getHistory().size() - maxHistoryEvents);
            for (int i = start; i < info.getHistory().size(); i++) {
                TrackingInfo.TrackingHistory event = info.getHistory().get(i);
                response.append("━━━━━━━━━━━━━━\n");
                response.append("🕐 ")
                        .append(event.getDateTime().format(dateFormatter))
                        .append("\n");
                response.append("📍 ").append(event.getLocation()).append("\n");
                response.append("📌 ").append(event.getStatus()).append("\n");
            }
        }

        sendMessage(chatId, response.toString(), createMainKeyboard());
    }

    private void sendMessage(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");

        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка отправки сообщения: " + e.getMessage());
            try {
                message.setParseMode(null);
                execute(message);
            } catch (TelegramApiException ex) {
                System.err.println("❌ Критическая ошибка отправки: " + ex.getMessage());
            }
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/help"));
        row1.add(new KeyboardButton("/about"));
        row1.add(new KeyboardButton("/status"));

        keyboard.add(row1);
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }
}
