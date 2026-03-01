package ru.otus.trackingbot.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Фабрика для получения сервисов отслеживания посылок.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *     <li>Получения сервиса по названию</li>
 *     <li>Автоматического определения сервиса по трек-номеру</li>
 *     <li>Получения списка всех доступных сервисов</li>
 *     <li>Проверки поддержки трек-номера</li>
 *     <li>Получения статистики использования сервисов</li>
 * </ul>
 * </p>
 */
public interface TrackingServiceFactory {

    /**
     * Возвращает сервис отслеживания по названию службы доставки.
     *
     * @param serviceName название службы доставки
     * @return Optional с сервисом или пустой Optional, если сервис не найден
     */
    Optional<AbstractTrackingService> getService(String serviceName);

    /**
     * Автоматически определяет службу доставки по трек-номеру.
     * <p>
     * Анализирует формат трек-номера и возвращает соответствующий сервис.
     * </p>
     *
     * @param trackingNumber трек-номер посылки
     * @return Optional с определенным сервисом или пустой Optional,
     *         если формат не поддерживается ни одним сервисом
     */
    Optional<AbstractTrackingService> detectService(String trackingNumber);

    /**
     * Возвращает список всех зарегистрированных сервисов отслеживания.
     *
     * @return неизменяемый список сервисов
     */
    List<AbstractTrackingService> getAllServices();

    /**
     * Проверяет, поддерживается ли данный трек-номер каким-либо сервисом.
     *
     * @param trackingNumber трек-номер для проверки
     * @return true если трек-номер поддерживается
     */
    boolean isSupported(String trackingNumber);

    /**
     * Возвращает статистику использования сервисов.
     * <p>
     * Статистика показывает, сколько раз каждый сервис был определен
     * для трек-номеров.
     * </p>
     *
     * @return Map с названием сервиса и количеством определений
     */
    Map<String, Integer> getServicesStatistics();
}
