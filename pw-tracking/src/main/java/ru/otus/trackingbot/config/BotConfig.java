package ru.otus.trackingbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.otus.trackingbot.bot.TrackingBot;

/**
 * Конфигурация Telegram бота.
 * <p>
 * Настраивает и регистрирует бота в Telegram API,
 * используя токен и имя бота из application.properties.
 * </p>
 */
@Configuration
public class BotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    /**
     * Создает и регистрирует экземпляр TelegramBotsApi.
     * <p>
     * Регистрирует бота в Telegram, чтобы он начал получать обновления.
     * </p>
     *
     * @param trackingBot экземпляр бота для регистрации
     * @return зарегистрированный TelegramBotsApi
     * @throws TelegramApiException если регистрация не удалась
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TrackingBot trackingBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(trackingBot);
        return botsApi;
    }

    /**
     * Создает экземпляр TrackingBot.
     *
     * @return настроенный экземпляр бота
     */
    @Bean
    public TrackingBot trackingBot() {
        return new TrackingBot(botToken);
    }
}
