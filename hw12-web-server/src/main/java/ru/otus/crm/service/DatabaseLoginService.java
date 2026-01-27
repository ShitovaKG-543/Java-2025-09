package ru.otus.crm.service;

import java.util.List;
import java.util.Optional;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.RolePrincipal;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.security.Credential;
import ru.otus.crm.model.User;

public class DatabaseLoginService extends AbstractLoginService {

    private final DBServiceUser dbServiceUser;

    public DatabaseLoginService(DBServiceUser dbServiceUser) {
        this.dbServiceUser = dbServiceUser;
    }

    @Override
    protected UserPrincipal loadUserInfo(String username) {
        Optional<User> userOptional = dbServiceUser.getUserByLogin(username);

        return userOptional
                .map(user -> new UserPrincipal(username, Credential.getCredential(user.getPassword())))
                .orElse(null);
    }

    @Override
    protected List<RolePrincipal> loadRoleInfo(UserPrincipal user) {
        Optional<User> userOptional = dbServiceUser.getUserByLogin(user.getName());

        return userOptional
                .map(u -> List.of(new RolePrincipal(u.getRole().name())))
                .orElse(List.of());
    }
}
