package ru.otus.trackingbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService тесты")
class UserServiceTest {

    private static final Long TEST_CHAT_ID = 123456789L;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_FIRST_NAME = "Тест";
    private static final String TEST_LAST_NAME = "Пользователь";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .chatId(TEST_CHAT_ID)
                .username("oldusername")
                .firstName("Старое")
                .lastName("Имя")
                .isActive(true)
                .notificationEnabled(true)
                .lastActivity(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ==================== ТЕСТЫ ДЛЯ getOrCreateUser ====================

    @Test
    @DisplayName("Должен вернуть существующего пользователя и обновить его информацию, когда пользователь найден")
    void shouldReturnExistingUserAndUpdateInfo() {

        LocalDateTime oldActivity = existingUser.getLastActivity();
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User result = userService.getOrCreateUser(TEST_CHAT_ID, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        assertThat(result).isEqualTo(existingUser);
        assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(result.getFirstName()).isEqualTo(TEST_FIRST_NAME);
        assertThat(result.getLastName()).isEqualTo(TEST_LAST_NAME);
        assertThat(result.getLastActivity()).isAfter(oldActivity);

        verify(userRepository).findByChatId(TEST_CHAT_ID);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Должен обновить только предоставленные поля, когда некоторые из них равны null")
    void shouldUpdateOnlyProvidedFieldsWhenSomeAreNull() {

        LocalDateTime oldActivity = existingUser.getLastActivity();
        String oldUsername = existingUser.getUsername();
        String oldFirstName = existingUser.getFirstName();

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User result = userService.getOrCreateUser(TEST_CHAT_ID, null, null, TEST_LAST_NAME);

        assertThat(result.getUsername()).isEqualTo(oldUsername);
        assertThat(result.getFirstName()).isEqualTo(oldFirstName);
        assertThat(result.getLastName()).isEqualTo(TEST_LAST_NAME);
        assertThat(result.getLastActivity()).isAfter(oldActivity);

        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Должен создать нового пользователя, когда пользователь не найден")
    void shouldCreateNewUserWhenNotFound() {

        User newUser = User.builder()
                .chatId(TEST_CHAT_ID)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .isActive(true)
                .notificationEnabled(true)
                .build();

        User savedUser = User.builder()
                .id(2L)
                .chatId(TEST_CHAT_ID)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .isActive(true)
                .notificationEnabled(true)
                .lastActivity(LocalDateTime.now())
                .build();

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.getOrCreateUser(TEST_CHAT_ID, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        assertThat(result).isEqualTo(savedUser);
        assertThat(result.getChatId()).isEqualTo(TEST_CHAT_ID);
        assertThat(result.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(result.getFirstName()).isEqualTo(TEST_FIRST_NAME);
        assertThat(result.getLastName()).isEqualTo(TEST_LAST_NAME);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getNotificationEnabled()).isTrue();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getChatId()).isEqualTo(TEST_CHAT_ID);
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(captor.getValue().getNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("Должен создать нового пользователя с null полями, когда все опциональные параметры равны null")
    void shouldCreateNewUserWithNullFieldsWhenAllOptionalParamsNull() {

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.getOrCreateUser(TEST_CHAT_ID, null, null, null);

        assertThat(result.getChatId()).isEqualTo(TEST_CHAT_ID);
        assertThat(result.getUsername()).isNull();
        assertThat(result.getFirstName()).isNull();
        assertThat(result.getLastName()).isNull();
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getNotificationEnabled()).isTrue();

        verify(userRepository).save(result);
    }

    @Test
    @DisplayName("Не должен обновлять поля пользователя со значением null, когда пользователь существует")
    void shouldNotUpdateUserFieldsWithNullValuesWhenUserExists() throws InterruptedException {

        String originalUsername = existingUser.getUsername();
        String originalFirstName = existingUser.getFirstName();
        String originalLastName = existingUser.getLastName();
        LocalDateTime originalActivity = existingUser.getLastActivity();

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setLastActivity(LocalDateTime.now());
            return savedUser;
        });

        Thread.sleep(10);
        User result = userService.getOrCreateUser(TEST_CHAT_ID, null, null, null);

        assertThat(result.getUsername()).isEqualTo(originalUsername);
        assertThat(result.getFirstName()).isEqualTo(originalFirstName);
        assertThat(result.getLastName()).isEqualTo(originalLastName);
        assertThat(result.getLastActivity()).isAfter(originalActivity);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать частичные обновления, когда предоставлен только username")
    void shouldHandlePartialUpdatesCorrectly() {

        String oldFirstName = existingUser.getFirstName();
        String oldLastName = existingUser.getLastName();

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User result = userService.getOrCreateUser(TEST_CHAT_ID, "newusername", null, null);

        assertThat(result.getUsername()).isEqualTo("newusername");
        assertThat(result.getFirstName()).isEqualTo(oldFirstName);
        assertThat(result.getLastName()).isEqualTo(oldLastName);
    }

    // ==================== ТЕСТЫ ДЛЯ updateLastActivity ====================

    @Test
    @DisplayName("Должен обновить время последней активности, когда пользователь существует")
    void shouldUpdateLastActivityWhenUserExists() {

        LocalDateTime oldActivity = existingUser.getLastActivity();
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.updateLastActivity(TEST_CHAT_ID);

        assertThat(existingUser.getLastActivity()).isAfter(oldActivity);
        verify(userRepository).findByChatId(TEST_CHAT_ID);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Не должен ничего делать, когда пользователь не найден")
    void shouldDoNothingWhenUserNotFound() {

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.empty());

        userService.updateLastActivity(TEST_CHAT_ID);

        verify(userRepository).findByChatId(TEST_CHAT_ID);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен корректно обрабатывать множественные обновления активности")
    void shouldHandleMultipleActivityUpdatesCorrectly() {

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        LocalDateTime firstUpdate = LocalDateTime.now();
        userService.updateLastActivity(TEST_CHAT_ID);

        LocalDateTime secondUpdate = LocalDateTime.now();
        userService.updateLastActivity(TEST_CHAT_ID);

        assertThat(existingUser.getLastActivity()).isAfterOrEqualTo(secondUpdate);
        verify(userRepository, times(2)).save(existingUser);
    }

    // ==================== ТЕСТЫ ДЛЯ toggleNotifications ====================

    @Test
    @DisplayName("Должен включить уведомления, когда пользователь существует и enabled=true")
    void shouldEnableNotificationsWhenUserExists() {

        existingUser.setNotificationEnabled(false);
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.toggleNotifications(TEST_CHAT_ID, true);

        assertThat(existingUser.getNotificationEnabled()).isTrue();
        verify(userRepository).findByChatId(TEST_CHAT_ID);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Должен выключить уведомления, когда пользователь существует и enabled=false")
    void shouldDisableNotificationsWhenUserExists() {

        existingUser.setNotificationEnabled(true);
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.toggleNotifications(TEST_CHAT_ID, false);

        assertThat(existingUser.getNotificationEnabled()).isFalse();
        verify(userRepository).findByChatId(TEST_CHAT_ID);
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Должен корректно переключать уведомления несколько раз")
    void shouldToggleNotificationsMultipleTimesCorrectly() {

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.toggleNotifications(TEST_CHAT_ID, false);
        assertThat(existingUser.getNotificationEnabled()).isFalse();

        userService.toggleNotifications(TEST_CHAT_ID, true);
        assertThat(existingUser.getNotificationEnabled()).isTrue();

        userService.toggleNotifications(TEST_CHAT_ID, false);
        assertThat(existingUser.getNotificationEnabled()).isFalse();

        verify(userRepository, times(3)).save(existingUser);
    }

    @Test
    @DisplayName("Не должен изменять состояние уведомлений при переключении на то же состояние")
    void shouldNotChangeNotificationStateWhenTogglingToSameState() {

        existingUser.setNotificationEnabled(true);
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.toggleNotifications(TEST_CHAT_ID, true);

        assertThat(existingUser.getNotificationEnabled()).isTrue();
        verify(userRepository).save(existingUser);
    }

    // ==================== ТЕСТЫ СЦЕНАРИЕВ ИНТЕГРАЦИИ ====================

    @Test
    @DisplayName(
            "Должен обработать полный жизненный цикл пользователя: создание, обновление активности, переключение уведомлений")
    void shouldHandleCompleteUserLifecycle() {
        // Сброс моков для этого теста
        reset(userRepository);

        // 1. Создание нового пользователя
        User newUser = User.builder()
                .id(1L)
                .chatId(TEST_CHAT_ID)
                .username(TEST_USERNAME)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .isActive(true)
                .notificationEnabled(true)
                .build();

        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        User user = userService.getOrCreateUser(TEST_CHAT_ID, TEST_USERNAME, TEST_FIRST_NAME, TEST_LAST_NAME);

        assertThat(user.getNotificationEnabled()).isTrue();
        assertThat(user.getIsActive()).isTrue();

        verify(userRepository, times(1)).save(any(User.class));

        // 2. Обновление времени последней активности
        when(userRepository.findByChatId(TEST_CHAT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateLastActivity(TEST_CHAT_ID);

        assertThat(user.getLastActivity()).isNotNull();
        verify(userRepository, times(2)).save(any(User.class));

        // 3. Отключение уведомлений
        userService.toggleNotifications(TEST_CHAT_ID, false);

        assertThat(user.getNotificationEnabled()).isFalse();
        verify(userRepository, times(3)).save(any(User.class));

        // 4. Повторное включение уведомлений
        userService.toggleNotifications(TEST_CHAT_ID, true);

        assertThat(user.getNotificationEnabled()).isTrue();
        verify(userRepository, times(4)).save(any(User.class));
    }
}
