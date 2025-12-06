package ru.otus.processor.homework.memento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

@ExtendWith(MockitoExtension.class)
class OriginatorTest {

    @Mock
    private DateTimeProvider dateTimeProvider;

    @InjectMocks
    private Originator originator;

    private Message testMessage;
    private State testState;
    private State initState;

    @BeforeEach
    void setUp() {

        ObjectForMessage field13 = new ObjectForMessage();
        List<String> data = new ArrayList<>();
        data.add("data1");
        data.add("data2");
        data.add("data3");
        field13.setData(data);

        testMessage = new Message.Builder(1L)
                .field1("value1")
                .field2("value2")
                .field13(field13)
                .build();

        testState = new State(testMessage);
        initState = new State(testState);
    }

    @Test
    @DisplayName("Должен сохранить состояние с текущей датой и временем")
    void shouldSaveStateWithCurrentDateTime() {

        LocalDateTime expectedTime = LocalDateTime.of(2024, 1, 1, 12, 30, 45);
        when(dateTimeProvider.getDate()).thenReturn(expectedTime);

        originator.saveState(testState);

        verify(dateTimeProvider).getDate();
        State savedState = originator.getLastSavedState();
        assertEquals(testState.getMessage(), savedState.getMessage());
        LocalDateTime savedTime = originator.getLastSavedDateTime();
        assertEquals(expectedTime, savedTime);
    }

    @Test
    @DisplayName("Должен восстановить последнее сохраненное состояние")
    void shouldRestoreLastSavedState() {

        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 30, 45);

        when(dateTimeProvider.getDate()).thenReturn(time1);

        originator.saveState(testState);

        State savedState = originator.getLastSavedState();
        assertEquals(testState.getMessage(), savedState.getMessage());
        LocalDateTime savedTime = originator.getLastSavedDateTime();
        assertEquals(time1, savedTime);

        ObjectForMessage field13 = testState.getMessage().getField13();
        field13.getData().add("data4");

        assertNotEquals(initState, testState);
        testState = originator.restoreState();

        assertNotNull(testState);
        assertEquals(initState.getMessage(), testState.getMessage());

        assertNull(originator.getLastSavedState());
        assertNull(originator.getLastSavedDateTime());
    }

    @Test
    @DisplayName("Должен восстанавливать состояния в порядке LIFO")
    void shouldRestoreStatesInLIFOOrder() {

        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 30, 45);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 1, 12, 30, 46);
        LocalDateTime time3 = LocalDateTime.of(2024, 1, 1, 12, 30, 47);

        when(dateTimeProvider.getDate()).thenReturn(time1, time2, time3);

        Message message1 = new Message.Builder(1L).field1("msg1").build();
        Message message2 = new Message.Builder(2L).field1("msg2").build();
        Message message3 = new Message.Builder(3L).field1("msg3").build();

        State state1 = new State(message1);
        State state2 = new State(message2);
        State state3 = new State(message3);

        originator.saveState(state1);
        originator.saveState(state2);
        originator.saveState(state3);

        // Восстанавливаем последнее сохраненное состояние (message2)
        State restored3 = originator.restoreState();
        assertEquals(message3, restored3.getMessage());
        assertEquals(message2, originator.getLastSavedState().getMessage());
        assertEquals(time2, originator.getLastSavedDateTime());

        // Восстанавливаем следующее (message1)
        State restored2 = originator.restoreState();
        assertEquals(message2, restored2.getMessage());
        assertEquals(message1, originator.getLastSavedState().getMessage());
        assertEquals(time1, originator.getLastSavedDateTime());

        // Восстанавливаем первое (пустой стек)
        State restored1 = originator.restoreState();
        assertEquals(message1, restored1.getMessage());
        assertNull(originator.getLastSavedState());
        assertNull(originator.getLastSavedDateTime());
    }

    @Test
    @DisplayName("Должен создавать глубокую копию состояния при сохранении")
    void shouldCreateDeepCopyOfStateWhenSaving() {

        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 12, 30, 45);
        when(dateTimeProvider.getDate()).thenReturn(time);

        // Создаем сообщение с изменяемыми данными
        ObjectForMessage field13 = new ObjectForMessage();
        List<String> data = new ArrayList<>();
        data.add("original1");
        data.add("original2");
        field13.setData(data);
        Message originalMessage =
                new Message.Builder(1L).field1("test").field13(field13).build();
        State originalState = new State(originalMessage);

        originator.saveState(originalState);

        // Изменяем оригинальное сообщение
        field13.getData().add("modified");
        originalMessage.toBuilder().field1("modified").build();

        // Проверяем, что последнее состояние не было изменено
        assertEquals("test", originator.getLastSavedState().getMessage().getField1());
        assertEquals(
                2,
                originator
                        .getLastSavedState()
                        .getMessage()
                        .getField13()
                        .getData()
                        .size());
        assertFalse(originator
                .getLastSavedState()
                .getMessage()
                .getField13()
                .getData()
                .contains("modified"));
    }
}
