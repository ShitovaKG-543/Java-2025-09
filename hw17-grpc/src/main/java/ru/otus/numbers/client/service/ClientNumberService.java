package ru.otus.numbers.client.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientNumberService {
    private static final Logger log = LoggerFactory.getLogger(ClientNumberService.class);

    private final AtomicInteger currentValue;
    private final AtomicInteger lastServerValue; // Хранит последнее значение от сервера, обнуляется после использования

    public ClientNumberService() {
        this.currentValue = new AtomicInteger(0);
        this.lastServerValue = new AtomicInteger(-1);
    }

    /**
     * Инкрементирует текущее значение (для первого вывода)
     */
    public int incrementAndGet() {
        return currentValue.incrementAndGet();
    }

    /**
     * Обновляет последнее полученное от сервера значение
     */
    public void updateLastServerValue(int value) {
        lastServerValue.set(value);
        log.debug("Server value updated: {}", value);
    }

    /**
     * Вычисляет следующее значение по формуле.
     * Если есть необработанное значение от сервера, использует его и обнуляет.
     */
    public int calculateNextValue() {
        // Получаем и сразу обнуляем lastServerValue, если оно не -1
        int serverValue = lastServerValue.getAndSet(-1);

        if (serverValue != -1) {
            // Есть новое значение от сервера - применяем формулу
            int newValue = currentValue.addAndGet(serverValue + 1);
            log.debug("Applied server value {} to get {}", serverValue, newValue);
            return newValue;
        } else {
            // Нет нового значения - простой инкремент
            int newValue = currentValue.incrementAndGet();
            log.debug("Simple increment to {}", newValue);
            return newValue;
        }
    }

    public int getCurrentValue() {
        return currentValue.get();
    }

    public int getLastServerValue() {
        return lastServerValue.get();
    }
}
