package ru.otus.crm.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.core.repository.DataTemplate;
import ru.otus.core.sessionmanager.TransactionManager;
import ru.otus.crm.model.User;

public class DbServiceUserImpl implements DBServiceUser {
    private static final Logger log = LoggerFactory.getLogger(DbServiceUserImpl.class);

    private final DataTemplate<User> userDataTemplate;
    private final TransactionManager transactionManager;

    public DbServiceUserImpl(TransactionManager transactionManager, DataTemplate<User> userDataTemplate) {
        this.transactionManager = transactionManager;
        this.userDataTemplate = userDataTemplate;
    }

    @Override
    public Optional<User> getUserByLogin(String login) {
        return transactionManager.doInReadOnlyTransaction(session -> {
            List<User> users = userDataTemplate.findByEntityField(session, "login", login);
            if (users != null && !users.isEmpty()) {
                var userOptional = Optional.ofNullable(users.getFirst());
                log.info("user: {}", userOptional);
                return userOptional;
            }
            return Optional.empty();
        });
    }
}
