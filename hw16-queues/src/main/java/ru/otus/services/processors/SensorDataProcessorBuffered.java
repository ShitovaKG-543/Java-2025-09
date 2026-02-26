package ru.otus.services.processors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.api.SensorDataProcessor;
import ru.otus.api.model.SensorData;
import ru.otus.lib.SensorDataBufferedWriter;

@SuppressWarnings({"java:S1068", "java:S125"})
public class SensorDataProcessorBuffered implements SensorDataProcessor {
    private static final Logger log = LoggerFactory.getLogger(SensorDataProcessorBuffered.class);

    private final int bufferSize;
    private final SensorDataBufferedWriter writer;
    private final BlockingQueue<SensorData> queue;

    public SensorDataProcessorBuffered(int bufferSize, SensorDataBufferedWriter writer) {
        this.bufferSize = bufferSize;
        this.writer = writer;
        this.queue = new ArrayBlockingQueue<>(bufferSize * 2);
    }

    @Override
    public void process(SensorData data) {

        queue.offer(data);
        if (queue.size() >= bufferSize) {
            flush();
        }
    }

    public synchronized void flush() {

        // Забираем данные из очереди
        List<SensorData> bufferedData = new ArrayList<>();
        queue.drainTo(bufferedData);

        if (bufferedData.isEmpty()) {
            return;
        }

        try {
            bufferedData.sort(Comparator.comparing(SensorData::getMeasurementTime));
            writer.writeBufferedData(bufferedData);
        } catch (Exception e) {
            log.error("Ошибка в процессе записи буфера", e);
            // В случае ошибки возвращаем данные обратно
            queue.addAll(bufferedData);
        }
    }

    @Override
    public void onProcessingEnd() {
        flush();
    }
}
