package ru.otus.trackingbot.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурация информации о боте.
 * Данные берутся из application.yml (секция bot.info)
 */
@Data
@Component
@ConfigurationProperties(prefix = "bot.info")
public class BotInfoConfig {

    /** Версия бота */
    private String version = "1.0.0";

    /** Разработчик */
    private String developer = "Ksenia Shitova";

    /** Копирайт */
    private String copyright = "© 2026 Все права защищены";

    /** Сайт/канал бота */
    private String website;

    /** Контакт поддержки */
    private String support = "@support";

    /** Список используемых технологий */
    private List<String> technologies;
}
