package ru.otus;

import ru.otus.cache.StatusCache;
import ru.otus.db.DatabaseManager;
import ru.otus.scheduler.StatusUpdateScheduler;
import ru.otus.service.TrackingService;
import ru.otus.service.bot.PostBot;
import ru.otus.service.postal.PostalService;
import ru.otus.service.postal.RussianPostService;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;

public class Main {
    public static void main(String[] args) throws Exception {
        // Загрузка конфигурации
        Yaml yaml = new Yaml();
        InputStream input = Main.class.getClassLoader().getResourceAsStream("application.yml");
        Map<String, Object> config = yaml.load(input);

        Map<String, Object> botConfig = (Map<String, Object>) config.get("bot");
        Map<String, Object> dbConfig = (Map<String, Object>) config.get("database");
        Map<String, Object> postalConfig = (Map<String, Object>) config.get("postal");
        Map<String, Object> cacheConfig = (Map<String, Object>) postalConfig.get("cache");
        Map<String, Object> schedulerConfig = (Map<String, Object>) postalConfig.get("scheduler");

        // Инициализация БД
        DatabaseManager.init(
                (String) dbConfig.get("url"),
                (String) dbConfig.get("user"),
                (String) dbConfig.get("password")
        );

        // Инициализация кэша
        int ttlMinutes = (int) cacheConfig.get("ttl-minutes");
        StatusCache cache = new StatusCache(ttlMinutes);

        // Инициализация почтовых сервисов (легко добавлять новые)
        List<PostalService> postalServices = List.of(
                new RussianPostService()
                // new DHLService(),  // можно добавить позже
                // new EmsService()
        );

        // Инициализация сервиса отслеживания
        TrackingService trackingService = new TrackingService(cache, postalServices);

        // Инициализация бота
        PostBot bot = new PostBot(
                (String) botConfig.get("token"),
                (String) botConfig.get("username"),
                trackingService
        );

        // Запуск бота
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

        // Запуск шедулера
        StatusUpdateScheduler scheduler = new StatusUpdateScheduler(trackingService, bot);
        scheduler.start((String) schedulerConfig.get("cron"));

        System.out.println("✅ Бот запущен!");
    }
}