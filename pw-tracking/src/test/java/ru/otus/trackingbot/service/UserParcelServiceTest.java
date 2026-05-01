package ru.otus.trackingbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.repository.UserParcelRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserParcelService тесты")
class UserParcelServiceTest {

    private static final String TEST_TRACKING_NUMBER = "TRK123456";

    @Mock
    private UserParcelRepository userParcelRepository;

    @InjectMocks
    private UserParcelService userParcelService;

    private User testUser;
    private Parcel testParcel;
    private UserParcel testUserParcel;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .chatId(123456789L)
                .username("testuser")
                .firstName("Тест")
                .lastName("Пользователь")
                .build();

        // Исправлено: у Parcel больше нет поля id
        testParcel = Parcel.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .build();

        testUserParcel = UserParcel.builder()
                .id(1L)
                .user(testUser)
                .parcel(testParcel)
                .isActive(true)
                .notificationCount(0)
                .addedAt(LocalDateTime.now())
                .build();
    }

    // ==================== ТЕСТЫ ДЛЯ addParcelForUser ====================

    @Test
    @DisplayName("Должен создать новую UserParcel, если отслеживание не найдено")
    void shouldCreateNewUserParcelWhenNotExists() {
        String customName = "Моя посылка";

        UserParcel savedUserParcel = UserParcel.builder()
                .id(2L)
                .user(testUser)
                .parcel(testParcel)
                .isActive(true)
                .customName(customName)
                .notificationCount(0)
                .addedAt(LocalDateTime.now())
                .build();

        when(userParcelRepository.findByUserAndParcelTrackingNumber(testUser, testParcel.getTrackingNumber()))
                .thenReturn(Optional.empty());
        when(userParcelRepository.save(any(UserParcel.class))).thenReturn(savedUserParcel);

        UserParcel result = userParcelService.addParcelForUser(testUser, testParcel, customName);

        assertThat(result).isNotNull();
        assertThat(result.getCustomName()).isEqualTo(customName);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getParcel()).isEqualTo(testParcel);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getNotificationCount()).isZero();

        ArgumentCaptor<UserParcel> captor = ArgumentCaptor.forClass(UserParcel.class);
        verify(userParcelRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomName()).isEqualTo(customName);
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(captor.getValue().getNotificationCount()).isZero();
    }

    @Test
    @DisplayName("Должен создать UserParcel без пользовательского имени, когда customName равен null")
    void shouldCreateWithoutCustomNameWhenNull() {
        when(userParcelRepository.findByUserAndParcelTrackingNumber(testUser, testParcel.getTrackingNumber()))
                .thenReturn(Optional.empty());
        when(userParcelRepository.save(any(UserParcel.class))).thenReturn(testUserParcel);

        UserParcel result = userParcelService.addParcelForUser(testUser, testParcel, null);

        assertThat(result.getCustomName()).isNull();
        ArgumentCaptor<UserParcel> captor = ArgumentCaptor.forClass(UserParcel.class);
        verify(userParcelRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomName()).isNull();
    }

    @Test
    @DisplayName("Должен реактивировать неактивную посылку и сбросить счетчик уведомлений")
    void shouldReactivateInactiveParcel() {
        UserParcel inactiveParcel = UserParcel.builder()
                .id(1L)
                .user(testUser)
                .parcel(testParcel)
                .isActive(false)
                .notificationCount(5)
                .lastNotification(LocalDateTime.now().minusDays(1))
                .addedAt(LocalDateTime.now().minusDays(10))
                .customName("Старое имя")
                .build();

        when(userParcelRepository.findByUserAndParcelTrackingNumber(testUser, testParcel.getTrackingNumber()))
                .thenReturn(Optional.of(inactiveParcel));
        when(userParcelRepository.save(any(UserParcel.class))).thenReturn(inactiveParcel);

        UserParcel result = userParcelService.addParcelForUser(testUser, testParcel, "Новое имя");

        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getNotificationCount()).isZero();
        assertThat(result.getLastNotification()).isNull();
        assertThat(result.getCustomName()).isEqualTo("Новое имя");
        assertThat(result.getAddedAt()).isNotNull();

        verify(userParcelRepository).save(inactiveParcel);
    }

    @Test
    @DisplayName(
            "Должен реактивировать неактивную посылку без изменения пользовательского имени, когда customName равен null")
    void shouldReactivateWithoutChangingCustomName() {
        UserParcel inactiveParcel = UserParcel.builder()
                .id(1L)
                .user(testUser)
                .parcel(testParcel)
                .isActive(false)
                .notificationCount(3)
                .customName("Старое имя")
                .build();

        when(userParcelRepository.findByUserAndParcelTrackingNumber(testUser, testParcel.getTrackingNumber()))
                .thenReturn(Optional.of(inactiveParcel));
        when(userParcelRepository.save(any(UserParcel.class))).thenReturn(inactiveParcel);

        UserParcel result = userParcelService.addParcelForUser(testUser, testParcel, null);

        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getNotificationCount()).isZero();
        assertThat(result.getCustomName()).isEqualTo("Старое имя");
    }

    @Test
    @DisplayName("Должен вернуть существующую активную посылку без изменений")
    void shouldReturnExistingActiveParcel() {
        when(userParcelRepository.findByUserAndParcelTrackingNumber(testUser, testParcel.getTrackingNumber()))
                .thenReturn(Optional.of(testUserParcel));

        UserParcel result = userParcelService.addParcelForUser(testUser, testParcel, "Новое имя");

        assertThat(result).isEqualTo(testUserParcel);
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getCustomName()).isNotEqualTo("Новое имя");
        verify(userParcelRepository, never()).save(any());
    }

    // ==================== ТЕСТЫ ДЛЯ updateUserParcelStatus ====================

    @Test
    @DisplayName("Должен обновить статус и описание статуса, когда оба параметра предоставлены")
    void shouldUpdateStatusAndDescription() {
        TrackingInfo trackingInfo = TrackingInfo.builder()
                .status("ДОСТАВЛЕНА")
                .statusDescription("Вручена получателю")
                .build();

        userParcelService.updateUserParcelStatus(testUserParcel, trackingInfo);

        assertThat(testUserParcel.getLastStatus()).isEqualTo("ДОСТАВЛЕНА");
        assertThat(testUserParcel.getLastStatusDescription()).isEqualTo("Вручена получателю");
        assertThat(testUserParcel.getLastChecked()).isNotNull();
        verify(userParcelRepository).save(testUserParcel);
    }

    @Test
    @DisplayName("Должен обновить только lastChecked, когда trackingInfo равен null")
    void shouldUpdateOnlyLastCheckedWhenTrackingInfoNull() {
        LocalDateTime beforeCheck = testUserParcel.getLastChecked();

        userParcelService.updateUserParcelStatus(testUserParcel, null);

        assertThat(testUserParcel.getLastStatus()).isNull();
        assertThat(testUserParcel.getLastStatusDescription()).isNull();
        assertThat(testUserParcel.getLastChecked()).isNotNull();
        assertThat(testUserParcel.getLastChecked()).isNotEqualTo(beforeCheck);
        verify(userParcelRepository).save(testUserParcel);
    }

    @Test
    @DisplayName("Должен обновить только статус, когда описание равно null")
    void shouldUpdateOnlyStatusWhenDescriptionNull() {
        TrackingInfo trackingInfo =
                TrackingInfo.builder().status("В ПУТИ").statusDescription(null).build();

        userParcelService.updateUserParcelStatus(testUserParcel, trackingInfo);

        assertThat(testUserParcel.getLastStatus()).isEqualTo("В ПУТИ");
        assertThat(testUserParcel.getLastStatusDescription()).isNull();
        verify(userParcelRepository).save(testUserParcel);
    }

    @Test
    @DisplayName("Должен обновить только описание, когда статус равен null")
    void shouldUpdateOnlyDescriptionWhenStatusNull() {
        TrackingInfo trackingInfo = TrackingInfo.builder()
                .status(null)
                .statusDescription("Обработка на складе")
                .build();

        userParcelService.updateUserParcelStatus(testUserParcel, trackingInfo);

        assertThat(testUserParcel.getLastStatus()).isNull();
        assertThat(testUserParcel.getLastStatusDescription()).isEqualTo("Обработка на складе");
        verify(userParcelRepository).save(testUserParcel);
    }

    @Test
    @DisplayName("Должен обработать trackingInfo с обоими null полями")
    void shouldHandleTrackingInfoWithBothNullFields() {
        TrackingInfo trackingInfo =
                TrackingInfo.builder().status(null).statusDescription(null).build();

        userParcelService.updateUserParcelStatus(testUserParcel, trackingInfo);

        assertThat(testUserParcel.getLastStatus()).isNull();
        assertThat(testUserParcel.getLastStatusDescription()).isNull();
        assertThat(testUserParcel.getLastChecked()).isNotNull();
        verify(userParcelRepository).save(testUserParcel);
    }

    // ==================== ТЕСТЫ ДЛЯ getActiveUserParcelsWithDetails ====================

    @Test
    @DisplayName("Должен вернуть активные посылки с деталями")
    void shouldReturnActiveParcelsWithDetails() {
        List<UserParcel> expectedParcels = List.of(testUserParcel);
        when(userParcelRepository.findByUserAndIsActiveTrueWithDetails(testUser))
                .thenReturn(expectedParcels);

        List<UserParcel> result = userParcelService.getActiveUserParcelsWithDetails(testUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUserParcel);
        verify(userParcelRepository).findByUserAndIsActiveTrueWithDetails(testUser);
    }

    @Test
    @DisplayName("Должен вернуть пустой список, когда у пользователя нет активных посылок")
    void shouldReturnEmptyListWhenNoActiveParcels() {
        when(userParcelRepository.findByUserAndIsActiveTrueWithDetails(testUser))
                .thenReturn(List.of());

        List<UserParcel> result = userParcelService.getActiveUserParcelsWithDetails(testUser);

        assertThat(result).isEmpty();
        verify(userParcelRepository).findByUserAndIsActiveTrueWithDetails(testUser);
    }

    // ==================== ТЕСТЫ ДЛЯ getActiveUserParcels ====================

    @Test
    @DisplayName("Должен вернуть активные посылки без деталей")
    void shouldReturnActiveParcelsWithoutDetails() {
        List<UserParcel> expectedParcels = List.of(testUserParcel);
        when(userParcelRepository.findByUserAndIsActiveTrue(testUser)).thenReturn(expectedParcels);

        List<UserParcel> result = userParcelService.getActiveUserParcels(testUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUserParcel);
        verify(userParcelRepository).findByUserAndIsActiveTrue(testUser);
    }

    // ==================== ТЕСТЫ ДЛЯ findByUserAndTrackingNumber ====================

    @Test
    @DisplayName("Должен вернуть UserParcel при поиске по трек-номеру")
    void shouldReturnUserParcelWhenFoundByTrackingNumber() {
        when(userParcelRepository.findByUserAndParcelTrackingNumber(testUser, TEST_TRACKING_NUMBER))
                .thenReturn(Optional.of(testUserParcel));

        Optional<UserParcel> result = userParcelService.findByUserAndTrackingNumber(testUser, TEST_TRACKING_NUMBER);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUserParcel);
        verify(userParcelRepository).findByUserAndParcelTrackingNumber(testUser, TEST_TRACKING_NUMBER);
    }

    @Test
    @DisplayName("Должен вернуть пустой Optional, когда трек-номер не найден")
    void shouldReturnEmptyOptionalWhenNotFound() {
        when(userParcelRepository.findByUserAndParcelTrackingNumber(testUser, "INVALID"))
                .thenReturn(Optional.empty());

        Optional<UserParcel> result = userParcelService.findByUserAndTrackingNumber(testUser, "INVALID");

        assertThat(result).isEmpty();
        verify(userParcelRepository).findByUserAndParcelTrackingNumber(testUser, "INVALID");
    }

    // ==================== ТЕСТЫ ДЛЯ getParcelsToUpdateWithDetails ====================

    @Test
    @DisplayName("Должен вернуть посылки, не обновлявшиеся последние 5 минут")
    void shouldReturnParcelsNotUpdatedInLast5Minutes() {
        List<UserParcel> parcelsToUpdate = List.of(testUserParcel);
        when(userParcelRepository.findUserParcelsToUpdateWithDetails(any(LocalDateTime.class)))
                .thenReturn(parcelsToUpdate);

        List<UserParcel> result = userParcelService.getParcelsToUpdateWithDetails();

        assertThat(result).hasSize(1);
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userParcelRepository).findUserParcelsToUpdateWithDetails(captor.capture());

        LocalDateTime cutoffTime = captor.getValue();
        assertThat(cutoffTime).isBefore(LocalDateTime.now());
        assertThat(cutoffTime).isAfter(LocalDateTime.now().minusMinutes(6));
    }

    @Test
    @DisplayName("Должен вернуть пустой список, когда все посылки актуальны")
    void shouldReturnEmptyListWhenAllParcelsUpToDate() {
        when(userParcelRepository.findUserParcelsToUpdateWithDetails(any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<UserParcel> result = userParcelService.getParcelsToUpdateWithDetails();

        assertThat(result).isEmpty();
        verify(userParcelRepository).findUserParcelsToUpdateWithDetails(any(LocalDateTime.class));
    }

    // ==================== ТЕСТЫ ДЛЯ sendNotification ====================

    @Test
    @DisplayName("Должен увеличить счетчик уведомлений и обновить дату последнего уведомления")
    void shouldIncrementNotificationCountAndUpdateDate() {
        testUserParcel.setNotificationCount(2);
        LocalDateTime before = testUserParcel.getLastNotification();

        userParcelService.sendNotification(testUserParcel, "Статус обновлен");

        assertThat(testUserParcel.getNotificationCount()).isEqualTo(3);
        assertThat(testUserParcel.getLastNotification()).isNotNull();
        assertThat(testUserParcel.getLastNotification()).isNotEqualTo(before);
        verify(userParcelRepository).save(testUserParcel);
    }

    @Test
    @DisplayName("Должен корректно обработать первое уведомление")
    void shouldHandleFirstNotificationCorrectly() {
        assertThat(testUserParcel.getNotificationCount()).isZero();
        assertThat(testUserParcel.getLastNotification()).isNull();

        userParcelService.sendNotification(testUserParcel, "Первое уведомление");

        assertThat(testUserParcel.getNotificationCount()).isEqualTo(1);
        assertThat(testUserParcel.getLastNotification()).isNotNull();
        verify(userParcelRepository).save(testUserParcel);
    }

    @Test
    @DisplayName("Должен корректно работать с параметром сообщения, даже когда он не используется")
    void shouldWorkWithMessageParameter() {
        userParcelService.sendNotification(testUserParcel, "Любое сообщение");

        assertThat(testUserParcel.getNotificationCount()).isEqualTo(1);
        verify(userParcelRepository).save(testUserParcel);
    }

    // ==================== ТЕСТЫ ДЛЯ findByIdWithDetails ====================

    @Test
    @DisplayName("Должен вернуть UserParcel при поиске по ID")
    void shouldReturnUserParcelWhenFoundById() {
        when(userParcelRepository.findByIdWithDetails(1L, testUser)).thenReturn(Optional.of(testUserParcel));

        Optional<UserParcel> result = userParcelService.findByIdWithDetails(1L, testUser);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUserParcel);
        verify(userParcelRepository).findByIdWithDetails(1L, testUser);
    }

    @Test
    @DisplayName("Должен вернуть пустой Optional, когда ID не найден")
    void shouldReturnEmptyOptionalWhenIdNotFound() {
        when(userParcelRepository.findByIdWithDetails(999L, testUser)).thenReturn(Optional.empty());

        Optional<UserParcel> result = userParcelService.findByIdWithDetails(999L, testUser);

        assertThat(result).isEmpty();
        verify(userParcelRepository).findByIdWithDetails(999L, testUser);
    }

    // ==================== ТЕСТЫ ДЛЯ deleteUserParcel ====================

    @Test
    @DisplayName("Должен удалить UserParcel из репозитория")
    void shouldDeleteUserParcel() {
        userParcelService.deleteUserParcel(testUserParcel);

        verify(userParcelRepository).delete(testUserParcel);
    }

    @Test
    @DisplayName("Должен корректно обработать вызов удаления")
    void shouldHandleDeleteCallCorrectly() {
        userParcelService.deleteUserParcel(testUserParcel);

        verify(userParcelRepository, times(1)).delete(testUserParcel);
        verifyNoMoreInteractions(userParcelRepository);
    }

    // ==================== ТЕСТЫ ДЛЯ stopTracking ====================

    @Test
    @DisplayName("Должен успешно остановить отслеживание и вернуть true")
    void shouldStopTrackingSuccessfully() {
        when(userParcelRepository.deactivateUserParcel(testUser, TEST_TRACKING_NUMBER))
                .thenReturn(1);

        boolean result = userParcelService.stopTracking(testUser, TEST_TRACKING_NUMBER);

        assertThat(result).isTrue();
        verify(userParcelRepository).deactivateUserParcel(testUser, TEST_TRACKING_NUMBER);
    }

    @Test
    @DisplayName("Должен вернуть false, когда посылка не найдена")
    void shouldReturnFalseWhenParcelNotFound() {
        when(userParcelRepository.deactivateUserParcel(testUser, "INVALID")).thenReturn(0);

        boolean result = userParcelService.stopTracking(testUser, "INVALID");

        assertThat(result).isFalse();
        verify(userParcelRepository).deactivateUserParcel(testUser, "INVALID");
    }

    @Test
    @DisplayName("Должен вернуть false, когда посылка уже неактивна")
    void shouldReturnFalseWhenParcelAlreadyInactive() {
        when(userParcelRepository.deactivateUserParcel(testUser, TEST_TRACKING_NUMBER))
                .thenReturn(0);

        boolean result = userParcelService.stopTracking(testUser, TEST_TRACKING_NUMBER);

        assertThat(result).isFalse();
        verify(userParcelRepository).deactivateUserParcel(testUser, TEST_TRACKING_NUMBER);
    }
}
