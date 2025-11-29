package ru.otus.processor.homework.memento;

import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import ru.otus.model.Message;
import ru.otus.model.ObjectForMessage;

@RequiredArgsConstructor
public class State {

    private final Message message;

    State(State state) {
        Message.Builder builder = state.message.toBuilder();

        if (state.message.getField13() != null && state.message.getField13().getData() != null) {
            ObjectForMessage field13Copy = new ObjectForMessage();
            field13Copy.setData(new ArrayList<>(state.message.getField13().getData()));
            builder.field13(field13Copy);
        }

        this.message = builder.build();
    }

    public Message getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return "State{" + "message=" + message + '}';
    }
}
