package ru.otus.services;

import lombok.RequiredArgsConstructor;
import ru.otus.crm.service.DBServiceUser;

@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final DBServiceUser dbServiceUser;

    @Override
    public boolean authenticate(String login, String password) {
        return dbServiceUser
                .getUserByLogin(login)
                .map(user -> user.getPassword().equals(password))
                .orElse(false);
    }
}
