package ru.otus.listener.homework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import ru.otus.listener.Listener;
import ru.otus.model.Message;
import ru.otus.model.ObjectForMessage;

@Slf4j
public class HistoryListener implements Listener, HistoryReader {

    private final Map<Long, Message> messageHistory = new HashMap<>();

    @Override
    public void onUpdated(Message msg) {
        if (msg == null) {
            log.warn("Не удалось сохранить историю. Сообщение пусто.");
            return;
        }
        Message messageCopy = createDeepCopy(msg);
        messageHistory.put(messageCopy.getId(), messageCopy);
    }

    @Override
    public Optional<Message> findMessageById(long id) {
        return Optional.ofNullable(messageHistory.get(id));
    }

    private Message createDeepCopy(Message original) {
        Message.Builder builder = original.toBuilder();

        if (original.getField13() != null && original.getField13().getData() != null) {
            ObjectForMessage field13Copy = new ObjectForMessage();
            field13Copy.setData(new ArrayList<>(original.getField13().getData()));
            builder.field13(field13Copy);
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "History{ messageHistory=" + messageHistory;
    }
}
