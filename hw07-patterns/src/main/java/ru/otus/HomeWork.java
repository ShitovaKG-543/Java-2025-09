package ru.otus;

import static java.lang.Thread.sleep;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.otus.handler.ComplexProcessor;
import ru.otus.listener.homework.HistoryListener;
import ru.otus.model.Message;
import ru.otus.model.ObjectForMessage;
import ru.otus.processor.homework.ProcessorEvenSecondException;
import ru.otus.processor.homework.ProcessorSwapField11AndField12;

@Slf4j
public class HomeWork {

    /*
    Реализовать to do:
      1. Добавить поля field11 - field13 (для field13 используйте класс ObjectForMessage)
      2. Сделать процессор, который поменяет местами значения field11 и field12
      3. Сделать процессор, который будет выбрасывать исключение в четную секунду (сделайте тест с гарантированным результатом)
            Секунда должна определяьться во время выполнения.
            Тест - важная часть задания
            Обязательно посмотрите пример к паттерну Мементо!
      4. Сделать Listener для ведения истории (подумайте, как сделать, чтобы сообщения не портились)
         Уже есть заготовка - класс HistoryListener, надо сделать его реализацию
         Для него уже есть тест, убедитесь, что тест проходит
    */

    @SneakyThrows
    public static void main(String[] args) {

        var processors =
                List.of(new ProcessorSwapField11AndField12(), new ProcessorEvenSecondException(LocalDateTime::now));

        var complexProcessor = new ComplexProcessor(processors, ex -> {
            log.error(ex.getMessage());
        });
        var historyListener = new HistoryListener();
        complexProcessor.addListener(historyListener);

        ObjectForMessage objectForMessage = new ObjectForMessage();
        List<String> data = new ArrayList<>();
        data.add("data1");
        data.add("data2");
        data.add("data3");

        objectForMessage.setData(data);
        var message1 = new Message.Builder(1L)
                .field1("field1")
                .field2("field2")
                .field3("field3")
                .field6("field6")
                .field7("field7")
                .field8("field8")
                .field9("field9")
                .field10("field10")
                .field11("field11")
                .field12("field12")
                .field13(objectForMessage)
                .build();

        var result1 = complexProcessor.handle(message1);
        log.info("result: {}", result1);

        message1 = message1.toBuilder()
                .field8("field8")
                .field9("field9")
                .field10("field10")
                .field11("field11")
                .field12("field12")
                .field13(objectForMessage)
                .build();
        sleep(Duration.ofSeconds(1));
        var result2 = complexProcessor.handle(message1);
        log.info("result: {}", result2);

        message1 = message1.toBuilder()
                .field1("field1")
                .field2("field2")
                .field3("field3")
                .field6("field6")
                .field7("field7")
                .field12("field12")
                .field13(objectForMessage)
                .build();
        sleep(Duration.ofSeconds(1));
        var result3 = complexProcessor.handle(message1);
        log.info("result: {}", result3);

        message1 = message1.toBuilder()
                .field1("field1")
                .field3("field3")
                .field6("field6")
                .field7("field7")
                .field12("field12")
                .build();
        var result4 = complexProcessor.handle(message1);
        log.info("result: {}", result4);

        log.info("history: {}", historyListener);
        complexProcessor.removeListener(historyListener);
    }
}
