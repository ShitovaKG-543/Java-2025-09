package ru.otus.processor.homework;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import ru.otus.model.Message;
import ru.otus.processor.Processor;
import ru.otus.processor.homework.exception.EvenSecondException;
import ru.otus.processor.homework.memento.DateTimeProvider;
import ru.otus.processor.homework.memento.Originator;
import ru.otus.processor.homework.memento.State;
import ru.otus.processor.homework.utils.DateTimeConverter;

@Slf4j
public class ProcessorEvenSecondException implements Processor {

    private final Originator originator;
    private final DateTimeProvider dateTimeProvider;

    public ProcessorEvenSecondException(DateTimeProvider dateTimeProvider) {
        this.dateTimeProvider = dateTimeProvider;
        this.originator = new Originator(dateTimeProvider);
    }

    // Для тестирования
    protected ProcessorEvenSecondException(DateTimeProvider dateTimeProvider, Originator originator) {
        this.dateTimeProvider = dateTimeProvider;
        this.originator = originator;
    }

    @Override
    public Message process(Message message) {

        State state = new State(message);
        LocalDateTime currentDateTime = dateTimeProvider.getDate();
        log.info("Текущее время: {}", DateTimeConverter.getDateTimeToString(currentDateTime));

        if (isEvenSecond(currentDateTime.getSecond())) {
            throw new EvenSecondException(currentDateTime);
        }

        originator.saveState(state);

        return state.getMessage();
    }

    private boolean isEvenSecond(int second) {
        return second % 2 == 0;
    }
}
