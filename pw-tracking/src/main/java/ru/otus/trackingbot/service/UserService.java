package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.repository.UserRepository;

/**
 * Сервис для управления пользователями бота.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *     <li>Регистрации и получения пользователей</li>
 *     <li>Обновления активности пользователя</li>
 *     <li>Управления настройками уведомлений</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Находит пользователя по chatId или создает нового, если не найден.
     * <p>
     * При создании нового пользователя устанавливаются значения по умолчанию
     * (уведомления включены, статус активен).
     * При нахождении существующего пользователя обновляет его данные
     * (username, firstName, lastName) и время последней активности.
     * </p>
     *
     * @param chatId идентификатор чата Telegram
     * @param username username пользователя в Telegram
     * @param firstName имя пользователя
     * @param lastName фамилия пользователя
     * @return найденный или созданный пользователь
     */
    @Transactional
    public User getOrCreateUser(Long chatId, String username, String firstName, String lastName) {
        Optional<User> existingUser = userRepository.findByChatId(chatId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setLastActivity(LocalDateTime.now());

            // Обновляем информацию, если она изменилась
            if (username != null) user.setUsername(username);
            if (firstName != null) user.setFirstName(firstName);
            if (lastName != null) user.setLastName(lastName);

            return userRepository.save(user);
        } else {
            User newUser = User.builder()
                    .chatId(chatId)
                    .username(username)
                    .firstName(firstName)
                    .lastName(lastName)
                    .isActive(true)
                    .notificationEnabled(true)
                    .build();
            log.info("Создан новый пользователь: chatId={}, username={}", chatId, username);
            return userRepository.save(newUser);
        }
    }

    /**
     * Обновляет время последней активности пользователя.
     *
     * @param chatId идентификатор чата
     */
    @Transactional
    public void updateLastActivity(Long chatId) {
        userRepository.findByChatId(chatId).ifPresent(user -> {
            user.setLastActivity(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Включает или выключает уведомления для пользователя.
     *
     * @param chatId идентификатор чата
     * @param enabled новое состояние уведомлений (true - включены, false - выключены)
     */
    @Transactional
    public void toggleNotifications(Long chatId, boolean enabled) {
        userRepository.findByChatId(chatId).ifPresent(user -> {
            user.setNotificationEnabled(enabled);
            userRepository.save(user);
            log.info("Уведомления для пользователя {} {}", chatId, enabled ? "включены" : "выключены");
        });
    }
}
