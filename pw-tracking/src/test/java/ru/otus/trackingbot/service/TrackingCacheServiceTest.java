package ru.otus.trackingbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.impl.RussianPostTrackingService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingCacheService тесты")
class TrackingCacheServiceTest {

    private static final String TRACKING_NUMBER = "TEST123456";

    @Mock
    private RussianPostTrackingService trackingService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private Cache.ValueWrapper valueWrapper;

    @InjectMocks
    private TrackingCacheService trackingCacheService;

    private TrackingInfo successTrackingInfo;
    private TrackingInfo failedTrackingInfo;

    @BeforeEach
    void setUp() {
        successTrackingInfo = TrackingInfo.builder()
                .trackingNumber(TRACKING_NUMBER)
                .serviceName("RussianPost")
                .status("В пути")
                .statusDescription("Посылка в пути")
                .success(true)
                .delivered(false)
                .lastCheck(LocalDateTime.now())
                .build();

        failedTrackingInfo = TrackingInfo.builder()
                .trackingNumber(TRACKING_NUMBER)
                .success(false)
                .error("Не найден")
                .build();
    }

    @Test
    @DisplayName("refreshCache - принудительно обновляет кеш")
    void refreshCache_forcesCacheRefresh() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(trackingService.trackParcel(TRACKING_NUMBER)).thenReturn(successTrackingInfo);
        doNothing().when(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));

        TrackingInfo result = trackingCacheService.refreshCache(TRACKING_NUMBER);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(trackingService).trackParcel(TRACKING_NUMBER);
        verify(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));
    }

    @Test
    @DisplayName("updateCache - успешно обновляет кеш")
    void updateCache_successfullyUpdatesCache() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        doNothing().when(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));

        trackingCacheService.updateCache(TRACKING_NUMBER, successTrackingInfo);

        verify(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));
    }

    @Test
    @DisplayName("updateCache - кеш не найден, ничего не делает")
    void updateCache_cacheNotFound_doesNothing() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(null);

        trackingCacheService.updateCache(TRACKING_NUMBER, successTrackingInfo);

        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("updateCache - информация об ошибке, не сохраняет в кеш")
    void updateCache_errorInfo_doesNotCache() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);

        trackingCacheService.updateCache(TRACKING_NUMBER, failedTrackingInfo);

        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("getTrackingInfoWithFreshnessCheck - кеш свежий, возвращает из кеша")
    void getTrackingInfoWithFreshnessCheck_cacheFresh_returnsFromCache() {

        LocalDateTime recentTime = LocalDateTime.now().minusSeconds(30);
        successTrackingInfo.setLastCheck(recentTime);

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.get(TRACKING_NUMBER)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(successTrackingInfo);

        TrackingInfo result = trackingCacheService.getTrackingInfoWithFreshnessCheck(TRACKING_NUMBER, 60);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(trackingService, never()).trackParcel(anyString());
    }

    @Test
    @DisplayName("getTrackingInfoWithFreshnessCheck - кеш устарел, вызывает API")
    void getTrackingInfoWithFreshnessCheck_cacheStale_callsApi() {

        LocalDateTime oldTime = LocalDateTime.now().minusSeconds(120);
        successTrackingInfo.setLastCheck(oldTime);

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.get(TRACKING_NUMBER)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(successTrackingInfo);
        when(trackingService.trackParcel(TRACKING_NUMBER)).thenReturn(successTrackingInfo);
        doNothing().when(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));

        TrackingInfo result = trackingCacheService.getTrackingInfoWithFreshnessCheck(TRACKING_NUMBER, 60);

        assertThat(result).isNotNull();
        verify(trackingService).trackParcel(TRACKING_NUMBER);
        verify(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));
    }

    @Test
    @DisplayName("getTrackingInfoWithFreshnessCheck - кеш отсутствует, вызывает API")
    void getTrackingInfoWithFreshnessCheck_noCache_callsApi() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.get(TRACKING_NUMBER)).thenReturn(null);
        when(trackingService.trackParcel(TRACKING_NUMBER)).thenReturn(successTrackingInfo);
        doNothing().when(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));

        TrackingInfo result = trackingCacheService.getTrackingInfoWithFreshnessCheck(TRACKING_NUMBER, 60);

        assertThat(result).isNotNull();
        verify(trackingService).trackParcel(TRACKING_NUMBER);
        verify(cache).put(eq(TRACKING_NUMBER), any(TrackingInfo.class));
    }

    @Test
    @DisplayName("getCacheAge - возвращает возраст кеша")
    void getCacheAge_returnsCacheAge() {

        LocalDateTime checkTime = LocalDateTime.now().minusSeconds(45);
        successTrackingInfo.setLastCheck(checkTime);

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.get(TRACKING_NUMBER)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(successTrackingInfo);

        Long age = trackingCacheService.getCacheAge(TRACKING_NUMBER);

        assertThat(age).isNotNull();
        assertThat(age).isBetween(44L, 46L);
    }

    @Test
    @DisplayName("getCacheAge - кеш отсутствует, возвращает null")
    void getCacheAge_noCache_returnsNull() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.get(TRACKING_NUMBER)).thenReturn(null);

        Long age = trackingCacheService.getCacheAge(TRACKING_NUMBER);

        assertThat(age).isNull();
    }

    @Test
    @DisplayName("isInCache - данные есть в кеше")
    void isInCache_dataInCache_returnsTrue() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.get(TRACKING_NUMBER)).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(successTrackingInfo);

        boolean inCache = trackingCacheService.isInCache(TRACKING_NUMBER);

        assertThat(inCache).isTrue();
    }

    @Test
    @DisplayName("isInCache - данных нет в кеше, возвращает false")
    void isInCache_noDataInCache_returnsFalse() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.get(TRACKING_NUMBER)).thenReturn(null);

        boolean inCache = trackingCacheService.isInCache(TRACKING_NUMBER);

        assertThat(inCache).isFalse();
    }

    @Test
    @DisplayName("isInCache - кеш не найден, возвращает false")
    void isInCache_cacheNotFound_returnsFalse() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(null);

        boolean inCache = trackingCacheService.isInCache(TRACKING_NUMBER);

        assertThat(inCache).isFalse();
    }

    @Test
    @DisplayName("logCacheStats - логирует статистику (просто проверяет, что нет исключений)")
    void logCacheStats_logsStatsWithoutException() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(cache);
        when(cache.getName()).thenReturn("trackingInfo");

        com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                mock(com.github.benmanes.caffeine.cache.Cache.class);
        when(caffeineCache.estimatedSize()).thenReturn(5L);
        when(caffeineCache.stats())
                .thenReturn(com.github.benmanes.caffeine.cache.stats.CacheStats.of(10, 5, 8, 2, 100, 3, 0));

        when(cache.getNativeCache()).thenReturn(caffeineCache);

        trackingCacheService.logCacheStats();

        verify(cache).getName();
        verify(cache).getNativeCache();
    }

    @Test
    @DisplayName("logCacheStats - кеш не найден, логирует warning")
    void logCacheStats_cacheNotFound_logsWarning() {

        when(cacheManager.getCache("trackingInfo")).thenReturn(null);

        trackingCacheService.logCacheStats();
    }
}
