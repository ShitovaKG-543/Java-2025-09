package ru.otus.numbers.server.service;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.numbers.common.AppConstants;
import ru.otus.numbers.common.model.NumberRange;
import ru.otus.numbers.proto.NumberRangeRequest;
import ru.otus.numbers.proto.NumberResponse;
import ru.otus.numbers.proto.NumbersServiceGrpc;

public class GrpcNumberServiceImpl extends NumbersServiceGrpc.NumbersServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(GrpcNumberServiceImpl.class);

    @Override
    public void generateNumbers(NumberRangeRequest request, StreamObserver<NumberResponse> responseObserver) {

        NumberRange range = new NumberRange(request.getFirstValue(), request.getLastValue());

        if (!range.isValid()) {
            log.error("Невалидный диапазон: первое значение должно быть меньше чем последнее");
            responseObserver.onError(new IllegalArgumentException("Невалидный диапазон"));
            return;
        }

        log.info("Обработка запроса на диапазон: {}-{}", range.getFirstValue(), range.getLastValue());

        NumberGenerationService generationService = new NumberGenerationService();

        try {
            generationService.startGeneration(range);

            while (generationService.hasMoreNumbers()) {
                Integer number = generationService.getNextNumber(AppConstants.POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                if (number != null) {
                    NumberResponse response =
                            NumberResponse.newBuilder().setValue(number).build();
                    responseObserver.onNext(response);
                    log.info("Отправлено число: {}", number);
                }
            }

            responseObserver.onCompleted();
            log.info("Завершена отправка чисел для диапазона: {}-{}", range.getFirstValue(), range.getLastValue());

        } catch (InterruptedException e) {
            log.error("Ошибка при отправке чисел", e);
            responseObserver.onError(e);
            Thread.currentThread().interrupt();
        } finally {
            generationService.stopGeneration();
        }
    }
}
