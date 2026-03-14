package ru.otus.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import ru.otus.domain.Message;
import ru.otus.domain.MessageDto;
import ru.otus.service.DataStore;

@RestController
public class DataController {
    private static final Logger log = LoggerFactory.getLogger(DataController.class);
    private final DataStore dataStore;
    private final Scheduler workerPool;

    public DataController(DataStore dataStore, Scheduler workerPool) {
        this.dataStore = dataStore;
        this.workerPool = workerPool;
    }

    @PostMapping(value = "/msg/{roomId}")
    public Mono<Long> messageFromChat(@PathVariable("roomId") String roomId, @RequestBody MessageDto messageDto) {
        var messageStr = messageDto.messageStr();

        return Mono.just(new Message(null, roomId, messageStr))
                .flatMap(dataStore::saveMessage)
                .publishOn(workerPool)
                .map(Message::id)
                .subscribeOn(workerPool);
    }

    @GetMapping(value = "/msg/{roomId}", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<MessageDto> getMessagesByRoomId(@PathVariable("roomId") String roomId) {
        return dataStore
                .loadMessages(roomId)
                .map(message -> new MessageDto(message.msgText()))
                .subscribeOn(workerPool);
    }

    @GetMapping(value = "/msg/all", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<MessageDto> getAllMessages() {
        return dataStore
                .loadAllMessages()
                .map(message -> new MessageDto(String.format("[Room %s] %s", message.roomId(), message.msgText())))
                .subscribeOn(workerPool);
    }
}
