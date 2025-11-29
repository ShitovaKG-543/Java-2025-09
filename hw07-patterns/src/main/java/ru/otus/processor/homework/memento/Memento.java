package ru.otus.processor.homework.memento;

import java.time.LocalDateTime;

record Memento(State state, LocalDateTime createdAt) {
    Memento(State state, LocalDateTime createdAt) {
        this.state = new State(state);
        this.createdAt = createdAt;
    }
}
