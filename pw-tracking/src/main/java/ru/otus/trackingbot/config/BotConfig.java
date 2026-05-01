package ru.otus.trackingbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.otus.trackingbot.bot.TrackingBot;
import ru.otus.trackingbot.bot.keyboard.KeyboardFactory;
import ru.otus.trackingbot.service.*;

/**
 * Конфигурация Telegram бота.
 * <p>
 * Настраивает и регистрирует бота в Telegram API,
 * используя токен и имя бота из application.properties.
 * </p>
 */
@Configuration
public class BotConfig {

    private final String botToken;
    private final String botUsername;
    private final UserService userService;
    private final ParcelService parcelService;
    private final UserParcelService userParcelService;
    private final TrackingCacheService trackingCacheService;
    private final TrackingServiceFactory trackingServiceFactory;
    private final KeyboardFactory keyboardFactory;
    private final BotInfoConfig botInfoConfig;

    public BotConfig(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UserService userService,
            ParcelService parcelService,
            UserParcelService userParcelService,
            TrackingCacheService trackingCacheService,
            TrackingServiceFactory trackingServiceFactory,
            KeyboardFactory keyboardFactory,
            BotInfoConfig botInfoConfig) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userService = userService;
        this.parcelService = parcelService;
        this.userParcelService = userParcelService;
        this.trackingCacheService = trackingCacheService;
        this.trackingServiceFactory = trackingServiceFactory;
        this.keyboardFactory = keyboardFactory;
        this.botInfoConfig = botInfoConfig;
    }

    /**
     * Создает экземпляр TrackingBot.
     *
     * @return настроенный экземпляр бота
     */
    @Bean
    public TrackingBot trackingBot() {
        return new TrackingBot(
                botToken,
                botUsername,
                userService,
                parcelService,
                userParcelService,
                trackingCacheService,
                trackingServiceFactory,
                keyboardFactory,
                botInfoConfig);
    }

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
}
