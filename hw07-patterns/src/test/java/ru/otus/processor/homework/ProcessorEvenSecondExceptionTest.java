package ru.otus.processor.homework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.model.Message;
import ru.otus.model.ObjectForMessage;
import ru.otus.processor.homework.exception.EvenSecondException;
import ru.otus.processor.homework.memento.DateTimeProvider;
import ru.otus.processor.homework.memento.Originator;
import ru.otus.processor.homework.memento.State;

@ExtendWith(MockitoExtension.class)
class ProcessorEvenSecondExceptionTest {

    @Mock
    private Originator originator;

    @Mock
    private DateTimeProvider dateTimeProvider;

    @InjectMocks
    private ProcessorEvenSecondException processor;

    private Message testMessage;
    private Message testMessage2;

    @BeforeEach
    void setUp() {

        // Создаем тестовое сообщение с данными
        ObjectForMessage field13 = new ObjectForMessage();
        field13.setData(List.of("data1", "data2", "data3"));

        testMessage = new Message.Builder(1L)
                .field1("value1")
                .field2("value2")
                .field13(field13)
                .build();

        testMessage2 = new Message.Builder(1L)
                .field3("value3")
                .field4("value4")
                .field8("value8")
                .build();
    }

    @Test
    @DisplayName("Должен успешно обработать сообщение при нечетной секунде")
    void shouldProcessMessageSuccessfullyWhenOddSecond() {

        LocalDateTime oddSecondTime = LocalDateTime.of(2024, 1, 1, 12, 30, 31); // 31 секунда - нечетная
        when(dateTimeProvider.getDate()).thenReturn(oddSecondTime);

        Message result = processor.process(testMessage);

        assertNotNull(result);
        assertEquals(testMessage, result);
        verify(originator).saveState(any(State.class));
        verify(originator, never()).restoreState();
    }

    @Test
    @DisplayName("Должен выбросить исключение и восстановить состояние при четной секунде")
    void shouldThrowExceptionAndRestoreStateWhenEvenSecond() {

        LocalDateTime oddSecondTime = LocalDateTime.of(2024, 1, 1, 12, 30, 31); // 31 секунда - нечетная

        when(dateTimeProvider.getDate()).thenReturn(oddSecondTime);
        Message result1 = processor.process(testMessage);
        assertNotNull(result1);
        assertEquals(testMessage, result1);

        LocalDateTime evenSecondTime = LocalDateTime.of(2024, 1, 1, 12, 30, 30); // 30 секунд - четная

        when(dateTimeProvider.getDate()).thenReturn(evenSecondTime);
        EvenSecondException exception = assertThrows(EvenSecondException.class, () -> processor.process(testMessage));

        assertEquals("Обработка в четную секунду: 01.01.2024 12:30:30", exception.getMessage());
        verify(originator, times(1)).saveState(any(State.class));
        verify(dateTimeProvider, times(2)).getDate();
    }

    @Test
    @DisplayName("Должен вернуть null при четной секунде если нет сохраненного состояния")
    void shouldReturnNullWhenEvenSecondAndNoSavedState() {

        LocalDateTime evenSecondTime = LocalDateTime.of(2024, 1, 1, 12, 30, 30);

        when(dateTimeProvider.getDate()).thenReturn(evenSecondTime);

        EvenSecondException exception = assertThrows(EvenSecondException.class, () -> processor.process(testMessage));

        assertEquals("Обработка в четную секунду: 01.01.2024 12:30:30", exception.getMessage());

        verify(originator, never()).saveState(any(State.class));
    }
}
