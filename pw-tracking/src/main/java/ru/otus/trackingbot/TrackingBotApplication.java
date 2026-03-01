package ru.otus.trackingbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения TrackingBot.
 * <p>
 * Точка входа в приложение Spring Boot бота для отслеживания посылок.
 * Приложение использует Spring Boot 3, интеграцию с Telegram Bot API,
 * JPA для работы с базой данных и планировщик задач для автоматического
 * обновления статусов.
 * </p>
 *
 * <p><b>Основные функции приложения:</b></p>
 * <ul>
 *     <li>Отслеживание посылок Почты России по трек-номеру</li>
 *     <li>Автоматические уведомления об изменении статуса</li>
 *     <li>Хранение истории статусов в базе данных</li>
 *     <li>Полная информация о посылке (вес, дата доставки, история)</li>
 *     <li>Настройка уведомлений для каждого пользователя</li>
 * </ul>
 *
 * <p><b>Технологии:</b></p>
 * <ul>
 *     <li>Java 21</li>
 *     <li>Spring Boot 3</li>
 *     <li>Spring Data JPA (Hibernate)</li>
 *     <li>PostgreSQL</li>
 *     <li>Telegram Bot API</li>
 *     <li>Caffeine Cache</li>
 *     <li>Lombok</li>
 *     <li>SOAP (для API Почты России)</li>
 * </ul>
 *
 * <p><b>Настройка:</b></p>
 * Для работы приложения необходимо указать в application.properties:
 * <ul>
 *     <li>telegram.bot.token - токен бота из Telegram</li>
 *     <li>telegram.bot.username - имя бота</li>
 *     <li>russianpost.tracking.login - логин для API Почты России</li>
 *     <li>russianpost.tracking.password - пароль для API Почты России</li>
 *     <li>spring.datasource.* - настройки подключения к БД</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class TrackingBotApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(TrackingBotApplication.class, args);
    }
}
