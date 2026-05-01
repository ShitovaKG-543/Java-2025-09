package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
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
 * Cache service for parcel tracking information.
 * Reduces API calls and improves response time.
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
     * Gets tracking info with caching.
     * First call hits API, subsequent calls return cached data within TTL.
     *
     * @param trackingNumber parcel tracking number
     * @return tracking information
     */
    @Cacheable(value = "trackingInfo", key = "#trackingNumber", unless = "#result == null || !#result.success")
    public TrackingInfo getTrackingInfo(String trackingNumber) {
        log.info("🔴 CACHE MISS - Calling API for {}", trackingNumber);
        TrackingInfo info = trackingService.trackParcel(trackingNumber);
        if (info.isSuccess()) {
            log.info("✅ Result cached for {}", trackingNumber);
        }
        return info;
    }

    /**
     * Gets fresh tracking info with cache freshness check.
     * Returns cached data if fresh, otherwise fetches from API.
     *
     * @param trackingNumber parcel tracking number
     * @param maxAgeSeconds maximum age of cached data in seconds
     * @return up-to-date tracking information
     */
    public TrackingInfo getTrackingInfoWithFreshnessCheck(String trackingNumber, int maxAgeSeconds) {
        Optional<TrackingInfo> cachedInfo = getCachedInfo(trackingNumber);

        if (cachedInfo.isPresent() && isCacheFresh(cachedInfo.get(), trackingNumber, maxAgeSeconds)) {
            return cachedInfo.get();
        }

        return fetchAndCache(trackingNumber);
    }

    /**
     * Gets cached tracking info if present.
     *
     * @param trackingNumber parcel tracking number
     * @return Optional with cached TrackingInfo
     */
    public Optional<TrackingInfo> getCachedInfo(String trackingNumber) {
        Cache cache = getCache();
        if (cache == null) {
            return Optional.empty();
        }

        Cache.ValueWrapper wrapper = cache.get(trackingNumber);
        if (wrapper == null) {
            return Optional.empty();
        }

        TrackingInfo info = (TrackingInfo) wrapper.get();
        if (info == null || !info.isSuccess()) {
            return Optional.empty();
        }

        return Optional.of(info);
    }

    /**
     * Checks if cached data is still fresh.
     */
    private boolean isCacheFresh(TrackingInfo cachedInfo, String trackingNumber, int maxAgeSeconds) {
        Optional<Long> age = getCacheAge(trackingNumber);

        if (age.isEmpty()) {
            log.info("🟡 CACHE STALE - No valid timestamp for {}", trackingNumber);
            return false;
        }

        long ageSeconds = age.get();

        if (ageSeconds < maxAgeSeconds) {
            log.info("🟢 CACHE HIT - Data from cache for {} (age: {} sec)", trackingNumber, ageSeconds);
            return true;
        }

        log.info(
                "🟡 CACHE STALE - Cache expired for {} (age: {} sec, limit: {} sec)",
                trackingNumber,
                ageSeconds,
                maxAgeSeconds);
        return false;
    }

    /**
     * Returns cache age in seconds.
     *
     * @param trackingNumber parcel tracking number
     * @return Optional with age in seconds, or empty if not cached
     */
    public Optional<Long> getCacheAge(String trackingNumber) {
        Optional<TrackingInfo> cachedInfo = getCachedInfo(trackingNumber);

        if (cachedInfo.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime lastCheck = cachedInfo.get().getLastCheck();
        if (lastCheck == null) {
            return Optional.empty();
        }

        long ageSeconds = ChronoUnit.SECONDS.between(lastCheck, LocalDateTime.now());
        return Optional.of(ageSeconds);
    }

    /**
     * Gets cache instance.
     */
    private Cache getCache() {
        Cache cache = cacheManager.getCache("trackingInfo");
        if (cache == null) {
            log.warn("Cache 'trackingInfo' not found");
        }
        return cache;
    }

    /**
     * Fetches fresh data from API and updates cache.
     */
    private TrackingInfo fetchAndCache(String trackingNumber) {
        log.info("🌐 Fetching fresh data from API for {}", trackingNumber);
        TrackingInfo info = trackingService.trackParcel(trackingNumber);
        if (info.isSuccess()) {
            updateCache(trackingNumber, info);
        }
        return info;
    }

    /**
     * Fetches data directly from API without cache.
     */
    private TrackingInfo fetchFromApi(String trackingNumber) {
        log.info("🌐 Direct API call for {}", trackingNumber);
        return trackingService.trackParcel(trackingNumber);
    }

    /**
     * Forces cache refresh for a parcel.
     *
     * @param trackingNumber parcel tracking number
     * @return fresh tracking information
     */
    public TrackingInfo refreshCache(String trackingNumber) {
        log.info("🔄 Force refreshing cache for {}", trackingNumber);
        evictCache(trackingNumber);
        TrackingInfo info = fetchFromApi(trackingNumber);
        if (info.isSuccess()) {
            updateCache(trackingNumber, info);
        }
        return info;
    }

    /**
     * Updates cache with provided tracking info.
     *
     * @param trackingNumber parcel tracking number
     * @param info tracking info to cache
     */
    public void updateCache(String trackingNumber, TrackingInfo info) {
        Cache cache = getCache();
        if (cache == null) {
            log.warn("Cannot update cache - cache not found for {}", trackingNumber);
            return;
        }

        if (info == null || !info.isSuccess()) {
            log.debug("Cannot update cache - invalid info for {}", trackingNumber);
            return;
        }

        cache.put(trackingNumber, info);
        log.info("✅ Cache updated for {} from provided data", trackingNumber);
    }

    /**
     * Evicts cache entry for a parcel.
     *
     * @param trackingNumber parcel tracking number
     */
    @CacheEvict(value = "trackingInfo", key = "#trackingNumber")
    public void evictCache(String trackingNumber) {
        log.debug("🗑 Cache evicted for {}", trackingNumber);
    }

    /**
     * Gets fresh tracking info ignoring cache.
     *
     * @param trackingNumber parcel tracking number
     * @return fresh tracking information
     */
    public TrackingInfo getFreshTrackingInfo(String trackingNumber) {
        log.info("🔄 Getting fresh data for {}", trackingNumber);
        evictCache(trackingNumber);
        TrackingInfo info = fetchFromApi(trackingNumber);
        if (info.isSuccess()) {
            updateCache(trackingNumber, info);
        }
        return info;
    }

    /**
     * Checks if tracking info exists in cache.
     *
     * @param trackingNumber parcel tracking number
     * @return true if cached and valid
     */
    public boolean isInCache(String trackingNumber) {
        return getCachedInfo(trackingNumber).isPresent();
    }

    /**
     * Logs cache statistics for monitoring.
     */
    public void logCacheStats() {
        Cache cache = getCache();
        if (cache == null) {
            log.warn("Cache 'trackingInfo' not found!");
            return;
        }

        log.info("📊 Cache statistics for 'trackingInfo':");
        log.info("   - Cache name: {}", cache.getName());

        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
            com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache =
                    (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
            log.info("   - Estimated size: {}", caffeineCache.estimatedSize());
            log.info("   - Stats: {}", caffeineCache.stats());
        }
    }
}
