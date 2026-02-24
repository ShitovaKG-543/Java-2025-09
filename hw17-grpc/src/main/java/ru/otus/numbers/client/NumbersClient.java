package ru.otus.numbers.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.numbers.client.observer.ClientStreamObserver;
import ru.otus.numbers.client.service.ClientNumberService;
import ru.otus.numbers.common.AppConstants;
import ru.otus.numbers.proto.NumberRangeRequest;
import ru.otus.numbers.proto.NumbersServiceGrpc;

public class NumbersClient {
    private static final Logger log = LoggerFactory.getLogger(NumbersClient.class);

    private final ManagedChannel channel;
    private final NumbersServiceGrpc.NumbersServiceStub asyncStub;
    private final ClientNumberService numberService;

    public NumbersClient() {
        this.channel = ManagedChannelBuilder.forAddress(AppConstants.SERVER_HOST, AppConstants.SERVER_PORT)
                .usePlaintext()
                .build();
        this.asyncStub = NumbersServiceGrpc.newStub(channel);
        this.numberService = new ClientNumberService();
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("Клиент начинает работу...");

        NumbersClient client = new NumbersClient();
        try {
            client.run();
        } finally {
            client.shutdown();
        }
    }

    private void run() throws InterruptedException {
        // Отправляем запрос на сервер
        NumberRangeRequest request = NumberRangeRequest.newBuilder()
                .setFirstValue(AppConstants.RANGE_START)
                .setLastValue(AppConstants.RANGE_END)
                .build();

        log.info("Отправлен запрос для диапазона {} - {}", AppConstants.RANGE_START, AppConstants.RANGE_END);

        // Создаем latch для синхронизации
        CountDownLatch startLatch = new CountDownLatch(1);
        ClientStreamObserver streamObserver = new ClientStreamObserver(numberService, startLatch);

        // Асинхронный вызов
        asyncStub.generateNumbers(request, streamObserver);

        // Сигнализируем, что клиент стартовал
        startLatch.countDown();

        // Основной цикл
        for (int i = 1; i <= AppConstants.MAX_ITERATIONS; i++) {
            int newValue = numberService.calculateNextValue();
            log.info("currentValue:{}", newValue);
            Thread.sleep(AppConstants.CLIENT_INTERVAL_MS);
        }
    }

    private void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
