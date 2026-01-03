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
                    log.error("Ошибка уведомления листнера: {}", e.getMessage());
                }
            }
        }
    }
}
