package ru.otus.service.bot;

import ru.otus.service.TrackingService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class PostBot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final TrackingService trackingService;

    public PostBot(String botToken, String botUsername, TrackingService trackingService) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.trackingService = trackingService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            if (text.equals("/start")) {
                sendMessage(chatId, "📮 Привет! Отправь мне трек-номер посылки Почты России, и я буду отслеживать её статус.");
            }
            else if (text.equals("/list")) {
                var parcels = trackingService.getAllUserParcels(chatId);
                if (parcels.isEmpty()) {
                    sendMessage(chatId, "У вас нет отслеживаемых посылок.");
                } else {
                    StringBuilder sb = new StringBuilder("📋 Ваши посылки:\n");
                    for (var p : parcels) {
                        sb.append("• ").append(p.getTrackingNumber())
                                .append(": ").append(p.getLastStatus()).append("\n");
                    }
                    sendMessage(chatId, sb.toString());
                }
            }
            else if (text.matches("\\d+")) {
                String status = trackingService.getTrackingStatus(chatId, text);
                sendMessage(chatId, "📬 Статус посылки " + text + ":\n" + status);
            }
            else {
                sendMessage(chatId, "Неизвестная команда. Используйте /start, /list или отправьте трек-номер.");
            }
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}