package ru.otus.trackingbot.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.bot.TrackingBot;

@TestConfiguration
public class TestBotConfig {

    @Bean
    @Primary
    public TrackingBot trackingBot() throws TelegramApiException {
        TrackingBot mockBot = Mockito.mock(TrackingBot.class);

        // Мокаем execute для SendMessage
        Mockito.doNothing().when(mockBot).execute(Mockito.any(SendMessage.class));

        // Мокаем execute для DeleteMessage
        Mockito.doNothing().when(mockBot).execute(Mockito.any(DeleteMessage.class));

        return mockBot;
    }
}
