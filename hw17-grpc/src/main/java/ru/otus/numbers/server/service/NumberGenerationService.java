package ru.otus.numbers.server.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.numbers.common.AppConstants;
import ru.otus.numbers.common.model.NumberRange;

public class NumberGenerationService {
    private static final Logger log = LoggerFactory.getLogger(NumberGenerationService.class);

    private final BlockingQueue<Integer> numberQueue;
    private final AtomicBoolean generating;
    private Thread generatorThread;
    private final AtomicInteger lastGeneratedValue;

    public NumberGenerationService() {
        this.numberQueue = new LinkedBlockingQueue<>();
        this.generating = new AtomicBoolean(false);
        this.lastGeneratedValue = new AtomicInteger(-1);
    }

    public void startGeneration(NumberRange range) {
        if (generating.get()) {
            throw new IllegalStateException("Генерация уже ведется");
        }

        generating.set(true);
        generatorThread = new Thread(() -> generateNumbers(range));
        generatorThread.setName("number-generator");
        generatorThread.setDaemon(true);
        generatorThread.start();
    }

    private void generateNumbers(NumberRange range) {
        try {
            for (int i = range.getFirstValue() + 1; i <= range.getLastValue() && generating.get(); i++) {

                numberQueue.put(i);
                lastGeneratedValue.set(i);
                log.info("Generated number: {}", i);

                Thread.sleep(AppConstants.SERVER_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            log.info("Прервана генерация числа");
            Thread.currentThread().interrupt();
        } finally {
            generating.set(false);
            log.info("Остановлена генерация числа");
        }
    }

    /**
     * Получает следующее число с таймаутом
     */
    public Integer getNextNumber(long timeout, TimeUnit unit) throws InterruptedException {
        return numberQueue.poll(timeout, unit);
    }

    /**
     * Проверяет, есть ли еще числа
     */
    public boolean hasMoreNumbers() {
        return generating.get() || !numberQueue.isEmpty();
    }

    public void stopGeneration() {
        generating.set(false);
        if (generatorThread != null) {
            generatorThread.interrupt();
        }
    }
}
