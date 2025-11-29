package ru.otus.processor.homework.memento;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.extern.slf4j.Slf4j;
import ru.otus.processor.homework.utils.DateTimeConverter;

@Slf4j
public class Originator {

    private final Deque<Memento> stack = new ArrayDeque<>();

    private final DateTimeProvider dateTimeProvider;

    public Originator(DateTimeProvider dateTimeProvider) {
        this.dateTimeProvider = dateTimeProvider;
    }

    public void saveState(State state) {
        LocalDateTime currentDateTime = dateTimeProvider.getDate();
        stack.push(new Memento(state, currentDateTime));
        log.info("Сохранено состояние: {}", state);
        log.info("Время сохранения: {}", DateTimeConverter.getDateTimeToString(currentDateTime));
    }

    public State restoreState() {
        if (stack.isEmpty()) {
            log.info("Состояние восстановлено до исходного");
            return null;
        }
        var memento = stack.pop();
        log.info("Восстановленно до состояния: {} с датой создания {}", memento.state(), memento.createdAt());
        return memento.state();
    }

    public State getLastSavedState() {
        if (stack.isEmpty()) {
            log.info("Сохраненные состояния отсутствуют");
            return null;
        }
        return stack.peek().state();
    }

    public LocalDateTime getLastSavedDateTime() {
        if (stack.isEmpty()) {
            log.info("Сохраненные состояния отсутствуют");
            return null;
        }
        return stack.peek().createdAt();
    }
}
