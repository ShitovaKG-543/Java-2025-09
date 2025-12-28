package ru.otus.cachehw;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyCache<K, V> implements HwCache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(MyCache.class);
    private final Map<K, V> cache = new WeakHashMap<>();
    private final List<WeakReference<HwListener<K, V>>> listeners = new ArrayList<>();

    @Override
    public void put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Ключ не может быть пустым");
        }

        cache.put(key, value);
        notifyListeners(key, value, "PUT");
        // Проверка памяти и очистка при необходимости
        checkMemoryAndCleanup();
    }

    @Override
    public void remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Ключ не может быть пустым");
        }

        V removedValue = cache.remove(key);
        if (removedValue != null) {
            notifyListeners(key, removedValue, "REMOVE");
        }
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Ключ не может быть пустым");
        }

        V value = cache.get(key);
        if (value != null) {
            notifyListeners(key, value, "GET");
        }
        return value;
    }

    @Override
    public void addListener(HwListener<K, V> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Листнер не может быть пустым");
        }

        listeners.add(new WeakReference<>(listener));
    }

    @Override
    public void removeListener(HwListener<K, V> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Листнер не может быть пустым");
        }

        listeners.removeIf(ref -> {
            HwListener<K, V> l = ref.get();
            return l == null || l.equals(listener);
        });
    }

    private void notifyListeners(K key, V value, String action) {
        Iterator<WeakReference<HwListener<K, V>>> iterator = listeners.iterator();

        while (iterator.hasNext()) {
            WeakReference<HwListener<K, V>> ref = iterator.next();
            HwListener<K, V> listener = ref.get();

            if (listener == null) {
                // Удаляем ссылки на собранные сборщиком мусора слушатели
                iterator.remove();
            } else {
                try {
                    listener.notify(key, value, action);
                } catch (Exception e) {
                    // Логируем ошибку, но не прерываем выполнение
                    System.err.println("Ошибка уведомления листнера: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Проверяет состояние памяти и при необходимости очищает кэш
     */
    private void checkMemoryAndCleanup() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        // Если используется более 80% памяти, очищаем кэш
        double memoryUsageRatio = (double) usedMemory / maxMemory;

        log.info("Использовано памяти: {}", memoryUsageRatio);

        // указан такой маленьких процент заполненности памяти, для демонстрации работы кеша при недостатке памяти
        if (memoryUsageRatio > 0.0035) {
            log.warn("Обнаружен недостаток памяти ({} % использовано). Очистка кэша...", memoryUsageRatio * 100);
            clearCache();
        }
    }

    /**
     * Очистка кэша при нехватке памяти
     */
    private void clearCache() {
        int beforeSize = cache.size();

        cache.clear();

        // Принудительный вызов GC
        System.gc();

        log.info("Кэш очищен. Размер до: {}, после: 0", beforeSize);

        // Уведомляем слушателей о полной очистке
        notifyListeners(null, null, "CLEAR_ALL");
    }
}
