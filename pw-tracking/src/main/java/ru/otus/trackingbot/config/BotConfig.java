package ru.otus.trackingbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.otus.trackingbot.bot.TrackingBot;

@Configuration
public class BotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Bean
    public TelegramBotsApi telegramBotsApi(TrackingBot trackingBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(trackingBot);
        return botsApi;
    }

    @Bean
    public TrackingBot trackingBot() {
        return new TrackingBot(botToken);
    }
}
