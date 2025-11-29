package ru.otus.processor.homework.exception;

import java.time.LocalDateTime;
import ru.otus.processor.homework.utils.DateTimeConverter;

public class EvenSecondException extends RuntimeException {

    public EvenSecondException(LocalDateTime currentDateTime) {
        super(String.format("Обработка в четную секунду: %s", DateTimeConverter.getDateTimeToString(currentDateTime)));
    }
}
