package ru.otus.trackingbot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация кеширования для приложения.
 * <p>
 * Использует Caffeine Cache для хранения информации об отслеживании посылок.
 * Кеширование позволяет снизить нагрузку на внешние API и ускорить ответы бота.
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.tracking.ttl:300}")
    private long ttlSeconds;

    @Value("${cache.tracking.max-size:1000}")
    private long maxSize;

    /**
     * Создает менеджер кеша для отслеживания посылок.
     * <p>
     * Настройки кеша:
     * <ul>
     *     <li>Время жизни записи - из конфигурации (по умолчанию 3600 секунд)</li>
     *     <li>Максимальный размер - из конфигурации (по умолчанию 1000 записей)</li>
     *     <li>Включена статистика использования кеша</li>
     * </ul>
     * </p>
     *
     * @return настроенный CacheManager для кеша trackingInfo
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("trackingInfo");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .recordStats());
        return cacheManager;
    }
}
