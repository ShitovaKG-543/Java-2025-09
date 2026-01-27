package ru.otus.crm.service;

import java.util.Optional;
import ru.otus.crm.model.User;

public interface DBServiceUser {

    Optional<User> getUserByLogin(String login);
}
