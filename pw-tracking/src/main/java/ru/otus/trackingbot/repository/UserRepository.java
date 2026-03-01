package ru.otus.trackingbot.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.otus.trackingbot.entity.User;

/**
 * Репозиторий для работы с сущностью {@link User}.
 * <p>
 * Предоставляет методы для поиска пользователей по chatId
 * и стандартные CRUD операции.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Находит пользователя по идентификатору чата Telegram.
     *
     * @param chatId идентификатор чата
     * @return Optional с найденным пользователем или пустой Optional
     */
    Optional<User> findByChatId(Long chatId);
}
