package ru.otus.trackingbot.bot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.model.TrackedItem;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.repository.TrackedItemRepository;
import ru.otus.trackingbot.service.AbstractTrackingService;
import ru.otus.trackingbot.service.TrackingServiceFactory;

@Component
@Slf4j
public class TrackingBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Autowired
    private TrackingServiceFactory trackingServiceFactory;

    @Autowired
    private TrackedItemRepository trackedItemRepository;

    public TrackingBot(@Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        registerCommands();
    }

    private void registerCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Запустить бота"));
        commands.add(new BotCommand("/help", "Помощь"));
        commands.add(new BotCommand("/list", "Мои посылки"));
        commands.add(new BotCommand("/services", "Список служб доставки"));
        commands.add(new BotCommand("/stats", "Статистика"));

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
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getFrom().getFirstName();
            String userId = update.getMessage().getFrom().getId().toString();

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setParseMode("HTML");

            log.info("Получено сообщение от {} ({}): {}", userName, userId, messageText);

            if (messageText.startsWith("/")) {
                handleCommand(messageText, chatId, userName, message);
            } else {
                handleTrackingRequest(messageText, chatId, message);
            }

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения пользователю {}", chatId, e);
            }
        }
    }

    private void handleCommand(String command, long chatId, String userName, SendMessage message) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "/start":
                message.setText(String.format(
                        "👋 Привет, %s!\n\n" + "Я бот для отслеживания посылок из разных служб доставки.\n\n"
                                + "<b>🎯 Как пользоваться:</b>\n"
                                + "1️⃣ Отправьте мне трек-номер\n"
                                + "2️⃣ Я автоматически определю службу доставки\n"
                                + "3️⃣ Буду присылать уведомления об изменении статуса\n\n"
                                + "<b>📋 Команды:</b>\n"
                                + "/list - показать все отслеживаемые посылки\n"
                                + "/services - список поддерживаемых служб\n"
                                + "/stats - статистика отслеживания\n"
                                + "/help - подробная помощь",
                        userName));
                break;

            case "/help":
                message.setText("<b>📦 Помощь по использованию бота</b>\n\n" + "<b>🔹 Основные команды:</b>\n"
                        + "• /list - список всех ваших посылок\n"
                        + "• /services - какие службы доставки поддерживаются\n"
                        + "• /stats - статистика по отслеживанию\n\n"
                        + "<b>🔹 Управление посылками:</b>\n"
                        + "• Отправьте трек-номер - начать отслеживание\n"
                        + "• /status [номер] - проверить статус\n"
                        + "• /stop [номер] - прекратить отслеживание\n\n"
                        + "<b>🔹 Примеры трек-номеров:</b>\n"
                        + "• Почта России: RA644000001RU или 12345678901234\n"
                        + "• СДЭК: 1234567890\n\n"
                        + "<b>🔹 Уведомления:</b>\n"
                        + "Бот автоматически проверяет статусы каждый час и присылает уведомления об изменениях.");
                break;

            case "/services":
                showServices(message);
                break;

            case "/list":
                showTrackedItems(chatId, message);
                break;

            case "/stats":
                showStats(chatId, message);
                break;

            case "/status":
                if (arg.isEmpty()) {
                    message.setText("❌ Укажите трек-номер. Пример: /status RA644000001RU");
                } else {
                    getStatus(arg, chatId, message);
                }
                break;

            case "/stop":
                if (arg.isEmpty()) {
                    message.setText("❌ Укажите трек-номер. Пример: /stop RA644000001RU");
                } else {
                    stopTracking(arg, chatId, message);
                }
                break;

            default:
                message.setText("❌ Неизвестная команда.\n" + "Используйте /help для списка доступных команд.");
        }
    }

    private void handleTrackingRequest(String trackingNumber, long chatId, SendMessage message) {
        String cleanNumber = trackingNumber.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");

        Optional<AbstractTrackingService> serviceOpt = trackingServiceFactory.detectService(cleanNumber);

        if (serviceOpt.isPresent()) {
            AbstractTrackingService service = serviceOpt.get();

            // Проверяем, не отслеживается ли уже
            TrackedItem existing = trackedItemRepository.findByChatIdAndTrackingNumber(chatId, cleanNumber);
            if (existing != null && existing.isActive()) {
                message.setText(String.format(
                        "📦 Посылка %s уже отслеживается!\n\n" + "Текущий статус: %s\n" + "Добавлена: %s",
                        cleanNumber, existing.getLastStatus(), formatDate(existing.getCreatedAt())));
                return;
            }

            // Отправляем сообщение о начале проверки
            message.setText(String.format(
                    "🔍 Проверяю информацию по трек-номеру %s (%s)...", cleanNumber, service.getServiceName()));

            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке сообщения", e);
            }

            // Проверяем статус
            TrackingInfo info = service.trackParcel(cleanNumber);

            if (info.isSuccess()) {
                // Сохраняем в базу
                TrackedItem item = new TrackedItem();
                item.setTrackingNumber(cleanNumber);
                item.setChatId(chatId);
                item.setServiceName(service.getServiceName());
                item.setCreatedAt(LocalDateTime.now());
                item.setLastChecked(LocalDateTime.now());
                item.setLastStatus(info.getStatus());
                item.setLastStatusDescription(info.getStatusDescription());
                item.setActive(true);

                trackedItemRepository.save(item);

                String response = formatTrackingInfo(info, "✅ Посылка добавлена для отслеживания!");
                message.setText(response);
            } else {
                message.setText("❌ " + info.getError());
            }
        } else {
            message.setText("❌ Не удалось определить службу доставки.\n\n"
                    + "Проверьте правильность номера. Поддерживаемые форматы:\n"
                    + "• Почта России: RA644000001RU или 14 цифр\n"
                    + "• СДЭК: 10-20 цифр\n\n"
                    + "Используйте /services для полного списка.");
        }
    }

    private void getStatus(String trackingNumber, long chatId, SendMessage message) {
        String cleanNumber = trackingNumber.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");

        Optional<AbstractTrackingService> serviceOpt = trackingServiceFactory.detectService(cleanNumber);

        if (serviceOpt.isEmpty()) {
            message.setText("❌ Не удалось определить службу доставки для этого номера.");
            return;
        }

        AbstractTrackingService service = serviceOpt.get();

        // Отправляем сообщение о начале проверки
        message.setText(String.format("🔍 Проверяю статус для %s...", cleanNumber));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения", e);
        }

        TrackingInfo info = service.trackParcel(cleanNumber);

        if (info.isSuccess()) {
            String response = formatTrackingInfo(info, "📦 Информация о посылке");
            message.setText(response);
        } else {
            message.setText("❌ Ошибка получения информации: " + info.getError());
        }
    }

    private String formatTrackingInfo(TrackingInfo info, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(title).append("</b>\n\n");

        // Статус с эмодзи
        String statusEmoji = getStatusEmoji(info);
        sb.append(statusEmoji)
                .append(" <b>Статус:</b> ")
                .append(info.getStatus())
                .append("\n");

        if (info.getStatusDescription() != null) {
            sb.append("📝 ").append(info.getStatusDescription()).append("\n");
        }

        sb.append("📋 <b>Трек-номер:</b> ").append(info.getTrackingNumber()).append("\n");
        sb.append("🚚 <b>Служба:</b> ").append(info.getServiceName()).append("\n");

        if (info.isDelivered()) {
            sb.append("✅ <b>Посылка доставлена!</b> 🎉\n");
        }

        if (info.getWeight() != null && info.getWeight() > 0) {
            sb.append("⚖️ <b>Вес:</b> ")
                    .append(String.format("%.2f кг", info.getWeight()))
                    .append("\n");
        }

        if (info.getLastOperation() != null) {
            sb.append("\n<b>📍 Последняя операция:</b>\n");
            sb.append("🕐 ")
                    .append(formatDate(info.getLastOperation().getDate()))
                    .append("\n");

            if (info.getLastOperation().getOperationPlace() != null) {
                sb.append("🏢 ")
                        .append(info.getLastOperation().getOperationPlace())
                        .append("\n");
            }
        }

        if (info.getDestination() != null) {
            sb.append("\n<b>🏁 Получатель:</b> ").append(info.getDestination()).append("\n");
        }

        if (info.getSender() != null) {
            sb.append("<b>📤 Отправитель:</b> ").append(info.getSender()).append("\n");
        }

        sb.append("\n🕒 <b>Проверено:</b> ").append(formatDate(info.getLastCheck()));

        return sb.toString();
    }

    private String getStatusEmoji(TrackingInfo info) {
        if (info == null) return "⚪";
        if (info.isDelivered()) return "✅";

        String status = info.getStatus() != null ? info.getStatus().toLowerCase() : "";
        if (status.contains("пути") || status.contains("транзит")) {
            return "🚚";
        } else if (status.contains("принят") || status.contains("сортировк")) {
            return "📦";
        } else if (status.contains("возврат") || status.contains("ошибк")) {
            return "⚠️";
        } else if (status.contains("таможн")) {
            return "🛃";
        } else if (status.contains("вручен") || status.contains("доставлен")) {
            return "✅";
        }
        return "📌";
    }

    private void showServices(SendMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>📦 Поддерживаемые службы доставки:</b>\n\n");

        List<AbstractTrackingService> services = trackingServiceFactory.getAllServices();
        for (AbstractTrackingService service : services) {
            sb.append("🔹 <b>").append(service.getServiceName()).append("</b>\n");
            sb.append("   Формат: <code>")
                    .append(service.getTrackingNumberPattern())
                    .append("</code>\n\n");
        }

        sb.append("Отправьте трек-номер, и бот автоматически определит службу!");
        message.setText(sb.toString());
    }

    private void showTrackedItems(long chatId, SendMessage message) {
        List<TrackedItem> items = trackedItemRepository.findByChatId(chatId);

        if (items.isEmpty()) {
            message.setText(
                    "📭 У вас пока нет отслеживаемых посылок.\n\nОтправьте трек-номер, чтобы начать отслеживание!");
            return;
        }

        // Фильтруем активные и неактивные
        List<TrackedItem> active = items.stream().filter(TrackedItem::isActive).toList();
        List<TrackedItem> inactive = items.stream().filter(i -> !i.isActive()).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("<b>📋 Ваши посылки:</b>\n\n");

        if (!active.isEmpty()) {
            sb.append("<b>✅ Активные:</b>\n");
            for (int i = 0; i < active.size(); i++) {
                TrackedItem item = active.get(i);
                sb.append(
                        String.format("%d. <b>%s</b> (%s)\n", i + 1, item.getTrackingNumber(), item.getServiceName()));
                sb.append(String.format("   📌 %s\n", item.getLastStatus()));

                // Добавляем эмодзи времени
                LocalDateTime lastCheck = item.getLastChecked();
                if (lastCheck != null) {
                    long hours = java.time.Duration.between(lastCheck, LocalDateTime.now())
                            .toHours();
                    if (hours < 1) {
                        sb.append("   🟢 только что проверено\n");
                    } else if (hours < 24) {
                        sb.append(String.format("   🟡 %d ч. назад\n", hours));
                    } else {
                        sb.append(String.format("   🔴 %d д. назад\n", hours / 24));
                    }
                }
                sb.append("\n");
            }
        }

        if (!inactive.isEmpty()) {
            sb.append("<b>⏸ Неактивные:</b>\n");
            for (int i = 0; i < inactive.size(); i++) {
                TrackedItem item = inactive.get(i);
                sb.append(String.format("• %s\n", item.getTrackingNumber()));
            }
        }

        sb.append("\n<b>Всего:</b> ").append(items.size()).append(" посылок");
        sb.append(" (активных: ").append(active.size()).append(")");

        message.setText(sb.toString());
    }

    private void showStats(long chatId, SendMessage message) {
        List<TrackedItem> items = trackedItemRepository.findByChatId(chatId);
        long activeCount = items.stream().filter(TrackedItem::isActive).count();
        long deliveredCount = items.stream()
                .filter(i -> "Доставлено".equalsIgnoreCase(i.getLastStatus()))
                .count();

        Map<String, Long> serviceStats = new java.util.HashMap<>();
        items.forEach(item -> {
            String service = item.getServiceName();
            serviceStats.put(service, serviceStats.getOrDefault(service, 0L) + 1);
        });

        StringBuilder sb = new StringBuilder();
        sb.append("<b>📊 Ваша статистика отслеживания</b>\n\n");
        sb.append("📦 <b>Всего посылок:</b> ").append(items.size()).append("\n");
        sb.append("✅ <b>Активных:</b> ").append(activeCount).append("\n");
        sb.append("🎉 <b>Доставлено:</b> ").append(deliveredCount).append("\n");
        sb.append("⏸ <b>Неактивных:</b> ").append(items.size() - activeCount).append("\n\n");

        sb.append("<b>По службам доставки:</b>\n");
        serviceStats.forEach((service, count) -> {
            sb.append("• ").append(service).append(": ").append(count).append("\n");
        });

        // Добавляем информацию о времени
        if (!items.isEmpty()) {
            TrackedItem oldest = items.stream()
                    .min((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .orElse(null);

            if (oldest != null) {
                sb.append("\n📅 <b>Отслеживаете с:</b> ").append(formatDate(oldest.getCreatedAt()));
            }
        }

        message.setText(sb.toString());
    }

    private void stopTracking(String trackingNumber, long chatId, SendMessage message) {
        String cleanNumber = trackingNumber.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        TrackedItem item = trackedItemRepository.findByChatIdAndTrackingNumber(chatId, cleanNumber);

        if (item != null) {
            if (!item.isActive()) {
                message.setText("ℹ️ Отслеживание этой посылки уже остановлено.");
                return;
            }

            item.setActive(false);
            trackedItemRepository.save(item);

            message.setText(String.format(
                    "✅ Отслеживание посылки %s остановлено.\n\n"
                            + "Чтобы возобновить, просто отправьте трек-номер снова.",
                    cleanNumber));

            log.info("Пользователь {} остановил отслеживание {}", chatId, cleanNumber);
        } else {
            message.setText("❌ Посылка с таким номером не найдена в вашем списке.");
        }
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "неизвестно";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return date.format(formatter);
    }
}
