package ru.otus.numbers.client.observer;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.numbers.client.service.ClientNumberService;
import ru.otus.numbers.proto.NumberResponse;

public class ClientStreamObserver implements StreamObserver<NumberResponse> {
    private static final Logger log = LoggerFactory.getLogger(ClientStreamObserver.class);

    private final ClientNumberService numberService;
    private final CountDownLatch startLatch;
    private final AtomicBoolean isFirstValueProcessed;

    public ClientStreamObserver(ClientNumberService numberService, CountDownLatch startLatch) {
        this.numberService = numberService;
        this.startLatch = startLatch;
        this.isFirstValueProcessed = new AtomicBoolean(false);
    }

    @Override
    public void onNext(NumberResponse response) {
        int value = response.getValue();

        if (!isFirstValueProcessed.get()) {
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            isFirstValueProcessed.set(true);
        }

        numberService.updateLastServerValue(value);
        log.info("new value:{}", value);
    }

    @Override
    public void onError(Throwable t) {
        log.error("Ошибка получения потока", t);
        startLatch.countDown();
    }

    @Override
    public void onCompleted() {
        log.info("Запрос выполнен");
        startLatch.countDown();
    }
}
