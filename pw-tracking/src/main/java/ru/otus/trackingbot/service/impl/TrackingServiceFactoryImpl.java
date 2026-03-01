package ru.otus.trackingbot.service.impl;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.otus.trackingbot.service.AbstractTrackingService;
import ru.otus.trackingbot.service.TrackingServiceFactory;

@Component
@Slf4j
public class TrackingServiceFactoryImpl implements TrackingServiceFactory {

    private final List<AbstractTrackingService> services;
    private final Map<String, AbstractTrackingService> serviceCache;
    private final Map<String, Integer> detectionStats;

    @Autowired
    public TrackingServiceFactoryImpl(List<AbstractTrackingService> services) {
        this.services = services;
        this.serviceCache = new ConcurrentHashMap<>();
        this.detectionStats = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        // Кэшируем сервисы по названиям
        for (AbstractTrackingService service : services) {
            serviceCache.put(service.getServiceName().toLowerCase(), service);
            detectionStats.put(service.getServiceName(), 0);
            log.info("✅ Зарегистрирован трекинговый сервис: {}", service.getServiceName());
        }
        log.info("Всего зарегистрировано сервисов: {}", services.size());
    }

    @Override
    public Optional<AbstractTrackingService> getService(String serviceName) {
        if (serviceName == null) return Optional.empty();
        return Optional.ofNullable(serviceCache.get(serviceName.toLowerCase()));
    }

    @Override
    public Optional<AbstractTrackingService> detectService(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return Optional.empty();
        }

        // Очищаем трек-номер от лишних символов
        String cleanNumber = trackingNumber.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");

        for (AbstractTrackingService service : services) {
            if (service.isValidTrackingNumber(cleanNumber)) {
                // Обновляем статистику
                detectionStats.merge(service.getServiceName(), 1, Integer::sum);

                log.debug("Трек-номер {} определен как {}", trackingNumber, service.getServiceName());
                return Optional.of(service);
            }
        }

        log.debug("Не удалось определить службу доставки для трек-номера {}", trackingNumber);
        return Optional.empty();
    }

    @Override
    public List<AbstractTrackingService> getAllServices() {
        return Collections.unmodifiableList(services);
    }

    @Override
    public boolean isSupported(String trackingNumber) {
        return detectService(trackingNumber).isPresent();
    }

    @Override
    public Map<String, Integer> getServicesStatistics() {
        return Collections.unmodifiableMap(detectionStats);
    }
}
