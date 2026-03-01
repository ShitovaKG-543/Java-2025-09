package ru.otus.trackingbot.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TrackingServiceFactory {

    // Получить сервис по названию
    Optional<AbstractTrackingService> getService(String serviceName);

    // Получить сервис по трек-номеру (определяем автоматически)
    Optional<AbstractTrackingService> detectService(String trackingNumber);

    // Получить все доступные сервисы
    List<AbstractTrackingService> getAllServices();

    // Проверить, поддерживается ли трек-номер каким-либо сервисом
    boolean isSupported(String trackingNumber);

    // Получить статистику по сервисам
    Map<String, Integer> getServicesStatistics();
}
