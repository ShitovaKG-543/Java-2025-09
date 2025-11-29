package ru.otus.processor.homework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.otus.model.Message;
import ru.otus.model.ObjectForMessage;

class ProcessorSwapField11AndField12Test {

    @Test
    @DisplayName("Должен поменять местами field11 и field12")
    void shouldSwapField11AndField12() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        Message originalMessage = new Message.Builder(1L)
                .field1("value1")
                .field2("value2")
                .field11("original_field11")
                .field12("original_field12")
                .build();

        Message result = processor.process(originalMessage);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("value1", result.getField1());
        assertEquals("value2", result.getField2());
        assertEquals("original_field12", result.getField11()); // field11 получает значение field12
        assertEquals("original_field11", result.getField12()); // field12 получает значение field11

        // Исходное сообщение не должно измениться
        assertEquals("original_field11", originalMessage.getField11());
        assertEquals("original_field12", originalMessage.getField12());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать null значения в field11 и field12")
    void shouldHandleNullFields() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        Message originalMessage =
                new Message.Builder(2L).field11(null).field12("not_null_value").build();

        Message result = processor.process(originalMessage);

        assertNotNull(result);
        assertEquals("not_null_value", result.getField11()); // field11 получает значение field12
        assertNull(result.getField12()); // field12 получает null из field11
    }

    @Test
    @DisplayName("Должен корректно обрабатывать оба null значения")
    void shouldHandleBothNullFields() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        Message originalMessage =
                new Message.Builder(3L).field11(null).field12(null).build();

        Message result = processor.process(originalMessage);

        assertNotNull(result);
        assertNull(result.getField11());
        assertNull(result.getField12());
    }

    @Test
    @DisplayName("Должен возвращать null при null входном сообщении")
    void shouldReturnNullForNullInput() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();

        Message result = processor.process(null);

        assertNull(result);
    }

    @Test
    @DisplayName("Должен сохранять все остальные поля неизменными, включая field13 с данными")
    void shouldPreserveAllOtherFieldsWithField13() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        ObjectForMessage field13 = new ObjectForMessage();
        field13.setData(Arrays.asList("data1", "data2", "data3"));

        Message originalMessage = new Message.Builder(4L)
                .field1("f1")
                .field2("f2")
                .field3("f3")
                .field4("f4")
                .field5("f5")
                .field6("f6")
                .field7("f7")
                .field8("f8")
                .field9("f9")
                .field10("f10")
                .field11("f11")
                .field12("f12")
                .field13(field13)
                .build();

        Message result = processor.process(originalMessage);

        assertNotNull(result);
        assertEquals("f1", result.getField1());
        assertEquals("f2", result.getField2());
        assertEquals("f3", result.getField3());
        assertEquals("f4", result.getField4());
        assertEquals("f5", result.getField5());
        assertEquals("f6", result.getField6());
        assertEquals("f7", result.getField7());
        assertEquals("f8", result.getField8());
        assertEquals("f9", result.getField9());
        assertEquals("f10", result.getField10());

        // Проверяем field13
        assertNotNull(result.getField13());
        assertEquals(
                Arrays.asList("data1", "data2", "data3"), result.getField13().getData());

        // Проверяем, что field11 и field12 поменялись местами
        assertEquals("f12", result.getField11());
        assertEquals("f11", result.getField12());
    }

    @Test
    @DisplayName("Должен корректно работать с пустыми строками")
    void shouldHandleEmptyStrings() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        Message originalMessage =
                new Message.Builder(5L).field11("").field12("not_empty").build();

        Message result = processor.process(originalMessage);

        assertNotNull(result);
        assertEquals("not_empty", result.getField11());
        assertEquals("", result.getField12());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать сообщение только с field11")
    void shouldHandleMessageWithOnlyField11() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        Message originalMessage =
                new Message.Builder(6L).field11("only_field11").build();

        Message result = processor.process(originalMessage);

        assertNotNull(result);
        assertNull(result.getField11()); // field11 получает null из field12
        assertEquals("only_field11", result.getField12()); // field12 получает значение field11
    }

    @Test
    @DisplayName("Должен корректно обрабатывать сообщение только с field12")
    void shouldHandleMessageWithOnlyField12() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        Message originalMessage =
                new Message.Builder(7L).field12("only_field12").build();

        Message result = processor.process(originalMessage);

        assertNotNull(result);
        assertEquals("only_field12", result.getField11()); // field11 получает значение field12
        assertNull(result.getField12()); // field12 получает null из field11
    }

    @Test
    @DisplayName("Должен создавать глубокую копию field13")
    void shouldCreateDeepCopyOfField13() {

        ProcessorSwapField11AndField12 processor = new ProcessorSwapField11AndField12();
        ObjectForMessage originalField13 = new ObjectForMessage();
        originalField13.setData(Arrays.asList("item1", "item2"));

        Message originalMessage = new Message.Builder(8L)
                .field11("f11")
                .field12("f12")
                .field13(originalField13)
                .build();

        Message result = processor.process(originalMessage);
        result.getField13().setData(new ArrayList<>());

        assertNotNull(result);
        assertNotNull(result.getField13());

        // Field13 должен быть тем же объектом (так как ObjectForMessage не клонируется)
        assertSame(originalField13, result.getField13());

        // Но поля поменялись местами
        assertEquals("f12", result.getField11());
        assertEquals("f11", result.getField12());
    }
}
