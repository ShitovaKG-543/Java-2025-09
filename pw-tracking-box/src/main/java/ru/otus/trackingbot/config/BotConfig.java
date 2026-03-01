package ru.otus.trackingbot.config;

import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class BotConfig {
    private final Map<String, Object> config;

    public BotConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            if (input == null) {
                System.err.println("❌ Не удалось найти application.yml");
                System.err.println("📁 Создайте файл в src/main/resources/application.yml");
                System.err.println("📋 Пример конфигурации в application.example.yml");
                throw new RuntimeException("application.yml not found");
            }

            Yaml yaml = new Yaml();
            config = yaml.load(input);

            // Проверка конфигурации
            validateConfig();

        } catch (Exception e) {
            System.err.println("❌ Ошибка загрузки конфигурации: " + e.getMessage());
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    private void validateConfig() {
        String token = getBotToken();
        String username = getBotUsername();

        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            System.err.println("❌ Ошибка: Не указан токен бота!");
            System.err.println("📝 Отредактируйте application.yml и укажите ваш токен");
            System.err.println("🔑 Получить токен можно у @BotFather в Telegram");
            System.err.println("📋 Или скопируйте application.example.yml и переименуйте");
            throw new RuntimeException("Bot token is not configured");
        }

        if (username == null || username.isEmpty() || username.equals("YourBotUsername")) {
            System.err.println("❌ Ошибка: Не указан username бота!");
            System.err.println("📝 Отредактируйте application.yml и укажите username бота");
            throw new RuntimeException("Bot username is not configured");
        }

        // Проверка API ключей (опционально)
        if (getCdekClientId() == null
                || getCdekClientId().isEmpty()
                || getCdekClientId().equals("YOUR_CDEK_CLIENT_ID")) {
            System.err.println("⚠️ ВНИМАНИЕ: Не указаны API ключи СДЭК");
            System.err.println("   Отслеживание СДЭК будет недоступно");
        }

        if (getRussianPostApiKey() == null
                || getRussianPostApiKey().isEmpty()
                || getRussianPostApiKey().equals("YOUR_RUSSIAN_POST_API_KEY")) {
            System.err.println("⚠️ ВНИМАНИЕ: Не указан API ключ Почты России");
            System.err.println("   Отслеживание Почты России будет недоступно");
        }

        System.out.println("✅ Конфигурация загружена успешно");
        System.out.println("🤖 Bot username: @" + username);
        System.out.println("📦 Режимы работы: " + getEnabledServices());
    }

    private String getEnabledServices() {
        StringBuilder services = new StringBuilder();
        if (isCdekEnabled()) services.append("СДЭК ");
        if (isRussianPostEnabled()) services.append("Почта России ");
        return services.length() > 0 ? services.toString() : "только проверка формата";
    }

    // Вспомогательные методы для доступа к конфигурации
    @SuppressWarnings("unchecked")
    private <T> T getProperty(String... keys) {
        Map<String, Object> current = config;
        for (int i = 0; i < keys.length - 1; i++) {
            Object value = current.get(keys[i]);
            if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else {
                return null;
            }
        }
        return (T) current.get(keys[keys.length - 1]);
    }

    // Telegram конфигурация
    public String getBotToken() {
        return getProperty("telegram", "bot", "token");
    }

    public String getBotUsername() {
        return getProperty("telegram", "bot", "username");
    }

    // СДЭК конфигурация
    public String getCdekClientId() {
        return getProperty("api", "cdek", "client-id");
    }

    public String getCdekClientSecret() {
        return getProperty("api", "cdek", "client-secret");
    }

    public String getCdekTrackingUrl() {
        String url = getProperty("api", "cdek", "tracking-url");
        return url != null ? url : "https://api.cdek.ru/v2/orders/tracking";
    }

    public String getCdekAuthUrl() {
        String url = getProperty("api", "cdek", "auth-url");
        return url != null ? url : "https://api.cdek.ru/v2/oauth/token";
    }

    public boolean isCdekEnabled() {
        String clientId = getCdekClientId();
        String clientSecret = getCdekClientSecret();
        return clientId != null
                && !clientId.isEmpty()
                && !clientId.equals("YOUR_CDEK_CLIENT_ID")
                && clientSecret != null
                && !clientSecret.isEmpty()
                && !clientSecret.equals("YOUR_CDEK_CLIENT_SECRET");
    }

    // Почта России конфигурация
    public String getRussianPostApiKey() {
        return getProperty("api", "russian-post", "api-key");
    }

    public String getRussianPostTrackingUrl() {
        String url = getProperty("api", "russian-post", "tracking-url");
        return url != null ? url : "https://tracking.russianpost.ru/fc/api/v2/tracking/feas";
    }

    public boolean isRussianPostEnabled() {
        String apiKey = getRussianPostApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_RUSSIAN_POST_API_KEY");
    }

    // Настройки бота
    public int getMaxHistoryEvents() {
        Integer max = getProperty("bot", "max-history-events");
        return max != null ? max : 5;
    }

    public String getDateFormat() {
        String format = getProperty("bot", "date-format");
        return format != null ? format : "dd.MM.yyyy HH:mm:ss";
    }

    public String getLanguage() {
        String lang = getProperty("bot", "language");
        return lang != null ? lang : "ru";
    }

    // Настройки логирования
    public String getLogLevel() {
        return getProperty("logging", "level");
    }

    public String getLogFile() {
        return getProperty("logging", "file");
    }
}
