package ru.otus;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.otus.trackingbot.bot.TrackingBot;
import ru.otus.trackingbot.config.BotConfig;

public class TrackingBotApplication {
    public static void main(String[] args) {
        try {
            System.out.println("┌─────────────────────────────────────┐");
            System.out.println("│   🚀 Tracking Bot v1.0.0            │");
            System.out.println("│   📦 Отслеживание посылок           │");
            System.out.println("└─────────────────────────────────────┘");
            System.out.println();

            System.out.println("📂 Загрузка конфигурации из application.yml...");
            BotConfig config = new BotConfig();

            System.out.println("🤖 Регистрация бота в Telegram API...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TrackingBot(config));

            System.out.println();
            System.out.println("✅ Бот успешно запущен!");
            System.out.println("📦 Отслеживание: " + getEnabledServices(config));
            System.out.println("🔗 Подключитесь: https://t.me/" + config.getBotUsername());
            System.out.println("📝 Логи: " + config.getLogFile());
            System.out.println();
            System.out.println("⏳ Ожидание сообщений...");

        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка запуска бота: " + e.getMessage());
            System.err.println("📝 Проверьте токен и username в application.yml");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getEnabledServices(BotConfig config) {
        StringBuilder services = new StringBuilder();
        if (config.isCdekEnabled()) services.append("СДЭК ");
        if (config.isRussianPostEnabled()) services.append("Почта России ");
        return services.length() > 0 ? services.toString() : "нет активных сервисов";
    }
}
