package ru.otus.trackingbot.service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.otus.trackingbot.entity.StatusDictionary;
import ru.otus.trackingbot.repository.StatusDictionaryRepository;

/**
 * Сервис для работы со справочником статусов.
 */
@Service
@Slf4j
public class StatusService {

    @Autowired
    private StatusDictionaryRepository statusDictionaryRepository;

    private final Map<String, StatusDictionary> statusCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadStatuses();
    }

    public void loadStatuses() {
        statusDictionaryRepository.findAll().forEach(status -> statusCache.put(status.getStatusCode(), status));
        log.info("Загружено {} статусов в кеш", statusCache.size());
    }

    public String getStatusName(String statusCode) {
        StatusDictionary status = statusCache.get(statusCode);
        return status != null ? status.getStatusName() : statusCode;
    }

    public String getStatusDescription(String statusCode) {
        StatusDictionary status = statusCache.get(statusCode);
        return status != null ? status.getStatusDescription() : "";
    }

    public String getStatusEmoji(String statusCode) {
        StatusDictionary status = statusCache.get(statusCode);
        return status != null && status.getEmoji() != null ? status.getEmoji() : "📌";
    }
}
