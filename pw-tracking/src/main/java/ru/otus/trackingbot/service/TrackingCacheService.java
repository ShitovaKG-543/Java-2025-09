package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.impl.RussianPostTrackingService;

/**
 * Сервис кеширования информации об отслеживании посылок.
 * <p>
 * Обеспечивает кеширование результатов запросов к API отслеживания
 * для снижения нагрузки на внешние сервисы и ускорения ответов бота.
 * </p>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *     <li>Использует Spring Cache с реализацией Caffeine</li>
 *     <li>Поддерживает проверку актуальности кеша по времени</li>
 *     <li>Позволяет принудительно обновлять кеш</li>
 *     <li>Предоставляет методы для получения статистики кеша</li>
 *     <li>Поддерживает ручное обновление кеша при изменении данных в БД</li>
 * </ul>
 */
@Service
@Slf4j
public class TrackingCacheService {

    @Autowired
    private RussianPostTrackingService trackingService;

    @Autowired
    private CacheManager cacheManager;

    @Value("${cache.tracking.ttl:300}")
    private long ttlSeconds;

    /**
     * Получает информацию о посылке с кешированием.
     * <p>
     * При первом запросе вызывает API и сохраняет результат в кеш.
     * Последующие запросы в течение TTL возвращают данные из кеша.
     * </p>
     *
     * @param trackingNumber трек-номер посылки
     * @return информация об отслеживании
     */
    @Cacheable(value = "trackingInfo", key = "#trackingNumber", unless = "#result == null || !#result.success")
    public TrackingInfo getTrackingInfo(String trackingNumber) {
        log.info("🔴 CACHE MISS - Вызов API для {}", trackingNumber);
        TrackingInfo info = trackingService.trackParcel(trackingNumber);
        if (info.isSuccess()) {
            log.info("✅ Результат сохранен в кеш для {}", trackingNumber);
        }
        return info;
    }

    /**
     * Выполняет прямой запрос к API без использования кеша.
     *
     * @param trackingNumber трек-номер посылки
     * @return информация об отслеживании
     */
    private TrackingInfo fetchFromApi(String trackingNumber) {
        log.info("🌐 Прямой вызов API для {}", trackingNumber);
        return trackingService.trackParcel(trackingNumber);
    }

    /**
     * Обновляет кеш для указанной посылки свежими данными из API.
     * Используется после обновления статуса через API.
     *
     * @param trackingNumber трек-номер посылки
     * @return свежая информация об отслеживании
     */
    public TrackingInfo refreshCache(String trackingNumber) {
        log.info("🔄 Принудительное обновление кеша для {}", trackingNumber);
        evictCache(trackingNumber);
        TrackingInfo info = fetchFromApi(trackingNumber);
        if (info.isSuccess()) {
            updateCache(trackingNumber, info);
        }
        return info;
    }

    /**
     * Обновляет кеш из данных TrackingInfo (без вызова API).
     * Используется когда данные уже получены из API и сохранены в БД.
     *
     * @param trackingNumber трек-номер
     * @param info информация для сохранения в кеш
     */
    public void updateCache(String trackingNumber, TrackingInfo info) {
        Cache cache = cacheManager.getCache("trackingInfo");
        if (cache != null && info != null && info.isSuccess()) {
            cache.put(trackingNumber, info);
            log.info("✅ Кеш обновлен для {} из полученных данных", trackingNumber);
        } else if (cache == null) {
            log.warn("Кеш trackingInfo не найден, невозможно обновить данные для {}", trackingNumber);
        } else if (info == null || !info.isSuccess()) {
            log.debug("Не удалось обновить кеш для {}: информация об ошибке", trackingNumber);
        }
    }

    /**
     * Получает информацию о посылке с проверкой актуальности кеша.
     * <p>
     * Если данные в кеше актуальны (возраст меньше maxAgeSeconds) -
     * возвращает из кеша. Если кеш устарел или отсутствует -
     * выполняет запрос к API.
     * </p>
     *
     * @param trackingNumber трек-номер посылки
     * @param maxAgeSeconds максимальный возраст кеша в секундах
     * @return актуальная информация об отслеживании
     */
    public TrackingInfo getTrackingInfoWithFreshnessCheck(String trackingNumber, int maxAgeSeconds) {
        Cache cache = cacheManager.getCache("trackingInfo");

        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(trackingNumber);
            if (wrapper != null) {
                TrackingInfo cachedInfo = (TrackingInfo) wrapper.get();
                if (cachedInfo != null && cachedInfo.isSuccess()) {
                    LocalDateTime lastCheck = cachedInfo.getLastCheck();
                    if (lastCheck != null) {
                        long ageSeconds = ChronoUnit.SECONDS.between(lastCheck, LocalDateTime.now());
                        if (ageSeconds < maxAgeSeconds) {
                            log.info(
                                    "🟢 CACHE HIT - Данные из кеша для {} (возраст: {} сек)",
                                    trackingNumber,
                                    ageSeconds);
                            return cachedInfo;
                        } else {
                            log.info(
                                    "🟡 CACHE STALE - Кеш устарел для {} (возраст: {} сек, лимит: {} сек)",
                                    trackingNumber,
                                    ageSeconds,
                                    maxAgeSeconds);
                        }
                    }
                }
            }
        }

        // Кеш отсутствует или устарел - делаем запрос к API
        log.info("🔴 CACHE MISS or STALE - Вызов API для {}", trackingNumber);
        TrackingInfo info = fetchFromApi(trackingNumber);
        if (info.isSuccess()) {
            updateCache(trackingNumber, info);
        }
        return info;
    }

    /**
     * Очищает кеш для конкретной посылки.
     *
     * @param trackingNumber трек-номер посылки
     */
    @CacheEvict(value = "trackingInfo", key = "#trackingNumber")
    public void evictCache(String trackingNumber) {
        log.debug("🗑 Кеш очищен для {}", trackingNumber);
    }

    /**
     * Получает актуальную информацию, игнорируя кеш.
     * <p>
     * Принудительно очищает кеш для указанной посылки
     * и выполняет запрос к API.
     * </p>
     *
     * @param trackingNumber трек-номер посылки
     * @return свежая информация об отслеживании
     */
    public TrackingInfo getFreshTrackingInfo(String trackingNumber) {
        log.info("🔄 Принудительное обновление для {}", trackingNumber);
        evictCache(trackingNumber);
        TrackingInfo info = fetchFromApi(trackingNumber);
        if (info.isSuccess()) {
            updateCache(trackingNumber, info);
        }
        return info;
    }

    /**
     * Возвращает возраст кеша в секундах.
     *
     * @param trackingNumber трек-номер посылки
     * @return возраст кеша в секундах или null, если кеш отсутствует
     */
    public Long getCacheAge(String trackingNumber) {
        Cache cache = cacheManager.getCache("trackingInfo");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(trackingNumber);
            if (wrapper != null) {
                TrackingInfo cachedInfo = (TrackingInfo) wrapper.get();
                if (cachedInfo != null && cachedInfo.getLastCheck() != null) {
                    return ChronoUnit.SECONDS.between(cachedInfo.getLastCheck(), LocalDateTime.now());
                }
            }
        }
        return null;
    }

    /**
     * Проверяет, есть ли информация о посылке в кеше.
     *
     * @param trackingNumber трек-номер посылки
     * @return true если данные есть в кеше
     */
    public boolean isInCache(String trackingNumber) {
        Cache cache = cacheManager.getCache("trackingInfo");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(trackingNumber);
            if (wrapper != null) {
                TrackingInfo cachedInfo = (TrackingInfo) wrapper.get();
                return cachedInfo != null && cachedInfo.isSuccess();
            }
        }
        return false;
    }

    /**
     * Выводит статистику кеша в лог.
     * Полезно для отладки и мониторинга.
     */
    public void logCacheStats() {
        Cache cache = cacheManager.getCache("trackingInfo");
        if (cache != null) {
            log.info("📊 Статистика кеша trackingInfo:");
            log.info("   - Имя кеша: {}", cache.getName());

            // Пытаемся получить нативную статистику Caffeine
            Object nativeCache = cache.getNativeCache();
            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache =
                        (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
                log.info("   - Размер кеша: {}", caffeineCache.estimatedSize());
                log.info("   - Статистика: {}", caffeineCache.stats());
            }
        } else {
            log.warn("Кеш trackingInfo не найден!");
        }
    }
}
