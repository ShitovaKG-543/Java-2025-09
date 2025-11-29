package ru.otus.processor.homework;

import ru.otus.model.Message;
import ru.otus.processor.Processor;

public class ProcessorSwapField11AndField12 implements Processor {

    @Override
    public Message process(Message message) {
        if (message == null) {
            return null;
        }

        return message.toBuilder()
                .field11(message.getField12())
                .field12(message.getField11())
                .build();
    }
}
