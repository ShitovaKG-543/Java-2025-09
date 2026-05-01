package ru.otus.trackingbot.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.otus.trackingbot.bot.TrackingBot;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.ParcelUpdateService;
import ru.otus.trackingbot.service.UserParcelService;

/**
 * Тесты для планировщика TrackingScheduler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Модульные тесты TrackingScheduler")
class TrackingSchedulerTest {

    @Mock
    private UserParcelService userParcelService;

    @Mock
    private ParcelUpdateService parcelUpdateService;

    @Mock
    private TrackingBot trackingBot;

    @InjectMocks
    private TrackingScheduler trackingScheduler;

    private static final Long CHAT_ID = 123456789L;
    private static final String TRACKING_NUMBER = "RA123456789RU";

    private User testUser;
    private Parcel testParcel;
    private UserParcel testUserParcel;

    @BeforeEach
    void setUp() {
        // Устанавливаем batchSize через рефлексию (важно!)
        ReflectionTestUtils.setField(trackingScheduler, "batchSize", 10);

        testUser = User.builder()
                .id(1L)
                .chatId(CHAT_ID)
                .firstName("Тестовый")
                .lastName("Пользователь")
                .notificationEnabled(true)
                .build();

        testParcel = Parcel.builder()
                .trackingNumber(TRACKING_NUMBER)
                .serviceName("Почта России")
                .build();

        testUserParcel = UserParcel.builder()
                .id(1L)
                .user(testUser)
                .parcel(testParcel)
                .isActive(true)
                .lastStatus("В пути")
                .lastChecked(LocalDateTime.now().minusHours(1))
                .build();
    }

    // =====================================================
    // ТЕСТЫ НА ПУСТЫЕ СПИСКИ
    // =====================================================

    @Test
    @DisplayName("Проверка статуса - нет посылок для проверки")
    void checkTrackingStatuses_noParcels() {
        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(Collections.emptyList());

        trackingScheduler.checkTrackingStatuses();

        verify(userParcelService).getParcelsToUpdateWithDetails();
        verify(parcelUpdateService, never()).updateParcelStatus(any(), anyBoolean());
    }

    @Test
    @DisplayName("Проверка статуса - пользователь отключил уведомления")
    void checkTrackingStatuses_notificationsDisabled() {
        testUser.setNotificationEnabled(false);
        testUserParcel.setUser(testUser);

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));

        trackingScheduler.checkTrackingStatuses();

        verify(userParcelService).getParcelsToUpdateWithDetails();
        verify(parcelUpdateService, never()).updateParcelStatus(any(), anyBoolean());
    }

    @Test
    @DisplayName("Проверка статуса - посылка уже доставлена (пропускаем)")
    void checkTrackingStatuses_alreadyDeliveredSkipped() {
        testUserParcel.setLastStatus("Доставлена");

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));

        trackingScheduler.checkTrackingStatuses();

        verify(userParcelService).getParcelsToUpdateWithDetails();
        verify(parcelUpdateService, never()).updateParcelStatus(any(), anyBoolean());
    }

    // =====================================================
    // ТЕСТЫ НА УСПЕШНЫЕ ОБНОВЛЕНИЯ
    // =====================================================

    @Test
    @DisplayName("Проверка статуса - успешное обновление с изменением статуса")
    void checkTrackingStatuses_successfulUpdateWithStatusChange() {
        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(true)
                .oldStatus("В пути")
                .newStatus("Доставлена")
                .info(TrackingInfo.builder()
                        .trackingNumber(TRACKING_NUMBER)
                        .status("Доставлена")
                        .statusDescription("Посылка доставлена")
                        .delivered(true)
                        .build())
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
    }

    @Test
    @DisplayName("Проверка статуса - статус не изменился")
    void checkTrackingStatuses_noStatusChange() {
        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(false)
                .oldStatus("В пути")
                .newStatus("В пути")
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
    }

    // =====================================================
    // ТЕСТЫ НА ОШИБКИ
    // =====================================================

    @Test
    @DisplayName("Проверка статуса - ошибка при обновлении")
    void checkTrackingStatuses_updateError() {
        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(false)
                .error("Ошибка API")
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
    }

    @Test
    @DisplayName("Проверка статуса - исключение при обработке")
    void checkTrackingStatuses_exceptionDuringProcessing() {
        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenThrow(new RuntimeException("Unexpected error"));

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
    }

    // =====================================================
    // ТЕСТЫ НА МНОЖЕСТВЕННЫЕ ПОСЫЛКИ
    // =====================================================

    @Test
    @DisplayName("Проверка статуса - обработка нескольких посылок")
    void checkTrackingStatuses_multipleParcels() {
        Parcel parcel2 = Parcel.builder()
                .trackingNumber("RA987654321RU")
                .serviceName("Почта России")
                .build();

        UserParcel userParcel2 = UserParcel.builder()
                .id(2L)
                .user(testUser)
                .parcel(parcel2)
                .isActive(true)
                .lastStatus("В пути")
                .lastChecked(LocalDateTime.now().minusHours(1))
                .build();

        ParcelUpdateService.ParcelUpdateResult result1 = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(false)
                .build();

        ParcelUpdateService.ParcelUpdateResult result2 = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(true)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel, userParcel2));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result1);
        when(parcelUpdateService.updateParcelStatus(eq(userParcel2), eq(false))).thenReturn(result2);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService, times(2)).updateParcelStatus(any(UserParcel.class), eq(false));
    }

    @Test
    @DisplayName("Проверка статуса - исключение при обработке не ломает остальные")
    void checkTrackingStatuses_exceptionDoesNotBreakOthers() {
        Parcel parcel2 = Parcel.builder()
                .trackingNumber("RA987654321RU")
                .serviceName("Почта России")
                .build();

        UserParcel userParcel2 = UserParcel.builder()
                .id(2L)
                .user(testUser)
                .parcel(parcel2)
                .isActive(true)
                .lastStatus("В пути")
                .lastChecked(LocalDateTime.now().minusHours(1))
                .build();

        ParcelUpdateService.ParcelUpdateResult result2 = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(false)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel, userParcel2));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenThrow(new RuntimeException("Unexpected error"));
        when(parcelUpdateService.updateParcelStatus(eq(userParcel2), eq(false))).thenReturn(result2);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService, times(2)).updateParcelStatus(any(UserParcel.class), eq(false));
    }

    // =====================================================
    // ТЕСТЫ С ДОСТАВЛЕННЫМИ ПОСЫЛКАМИ
    // =====================================================

    @Test
    @DisplayName("Проверка статуса - смесь активных и доставленных посылок")
    void checkTrackingStatuses_mixedActiveAndDelivered() {
        Parcel parcel2 = Parcel.builder()
                .trackingNumber("RA987654321RU")
                .serviceName("Почта России")
                .build();

        UserParcel deliveredParcel = UserParcel.builder()
                .id(2L)
                .user(testUser)
                .parcel(parcel2)
                .isActive(true)
                .lastStatus("Доставлена")
                .lastChecked(LocalDateTime.now().minusHours(1))
                .build();

        ParcelUpdateService.ParcelUpdateResult result1 = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(false)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel, deliveredParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result1);

        trackingScheduler.checkTrackingStatuses();

        // Доставленная посылка должна быть пропущена (не вызываем updateParcelStatus)
        verify(parcelUpdateService, times(1)).updateParcelStatus(any(UserParcel.class), eq(false));
    }
}
