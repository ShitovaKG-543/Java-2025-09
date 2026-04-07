package ru.otus.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private Long chatId;
    private String username;
    private boolean active;

}
