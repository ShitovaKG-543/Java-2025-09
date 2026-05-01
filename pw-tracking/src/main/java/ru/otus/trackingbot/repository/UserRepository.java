package ru.otus.trackingbot.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Проверяет, существует ли пользователь с указанным chatId.
     *
     * @param chatId идентификатор чата
     * @return true если пользователь существует
     */
    boolean existsByChatId(Long chatId);

    /**
     * Возвращает всех пользователей с включенными уведомлениями.
     *
     * @return список пользователей
     */
    @Query("SELECT u FROM User u WHERE u.notificationEnabled = true AND u.isActive = true")
    List<User> findAllWithNotificationsEnabled();
}
