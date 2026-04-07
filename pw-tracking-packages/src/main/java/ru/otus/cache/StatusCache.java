package ru.otus.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class StatusCache {
    private final Cache<String, CachedStatus> cache;

    public StatusCache(int ttlMinutes) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    public CachedStatus get(String trackingNumber) {
        return cache.getIfPresent(trackingNumber);
    }

    public void put(String trackingNumber, String status) {
        cache.put(trackingNumber, new CachedStatus(status, System.currentTimeMillis()));
    }

    public static class CachedStatus {
        public final String status;
        public final long timestamp;

        public CachedStatus(String status, long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}
