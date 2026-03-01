package ru.otus.trackingbot.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.otus.trackingbot.bot.TrackingBot;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.ParcelService;
import ru.otus.trackingbot.service.ParcelUpdateService;
import ru.otus.trackingbot.service.TrackingCacheService;
import ru.otus.trackingbot.service.UserParcelService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingScheduler тесты")
class TrackingSchedulerTest {

    @Mock
    private UserParcelService userParcelService;

    @Mock
    private ParcelService parcelService;

    @Mock
    private TrackingCacheService trackingCacheService;

    @Mock
    private TrackingBot trackingBot;

    @Mock
    private ParcelUpdateService parcelUpdateService;

    @InjectMocks
    private TrackingScheduler trackingScheduler;

    private static final Long CHAT_ID = 123456789L;
    private static final String TRACKING_NUMBER = "TEST123456";
    private static final String SERVICE_NAME = "TestService";

    private User testUser;
    private Parcel testParcel;
    private UserParcel testUserParcel;
    private TrackingInfo testTrackingInfo;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .chatId(CHAT_ID)
                .firstName("TestUser")
                .notificationEnabled(true)
                .build();

        testParcel = Parcel.builder()
                .id(1L)
                .trackingNumber(TRACKING_NUMBER)
                .serviceName(SERVICE_NAME)
                .build();

        testUserParcel = UserParcel.builder()
                .id(1L)
                .user(testUser)
                .parcel(testParcel)
                .isActive(true)
                .build();

        testTrackingInfo = TrackingInfo.builder()
                .trackingNumber(TRACKING_NUMBER)
                .serviceName(SERVICE_NAME)
                .status("В пути")
                .statusDescription("Посылка в пути")
                .success(true)
                .delivered(false)
                .build();
    }

    @Test
    @DisplayName("checkTrackingStatuses - нет посылок для проверки")
    void checkTrackingStatuses_noParcels() throws TelegramApiException {

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(Collections.emptyList());

        trackingScheduler.checkTrackingStatuses();

        verify(userParcelService).getParcelsToUpdateWithDetails();
        verify(parcelUpdateService, never()).updateParcelStatus(any(), anyBoolean());
        verify(trackingBot, never()).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("checkTrackingStatuses - пользователь отключил уведомления")
    void checkTrackingStatuses_userNotificationsDisabled() throws TelegramApiException {

        testUser.setNotificationEnabled(false);
        testUserParcel.setUser(testUser);

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));

        trackingScheduler.checkTrackingStatuses();

        verify(userParcelService).getParcelsToUpdateWithDetails();
        verify(parcelUpdateService, never()).updateParcelStatus(any(), anyBoolean());
        verify(trackingBot, never()).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("checkTrackingStatuses - успешное обновление статуса")
    void checkTrackingStatuses_successfulUpdate() throws TelegramApiException {

        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(true)
                .oldStatus("Создана")
                .newStatus("В пути")
                .info(testTrackingInfo)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);
        when(trackingBot.execute(any(SendMessage.class))).thenReturn(null);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
        verify(trackingBot).execute(any(SendMessage.class));
        verify(userParcelService).sendNotification(eq(testUserParcel), anyString());
    }

    @Test
    @DisplayName("checkTrackingStatuses - статус не изменился")
    void checkTrackingStatuses_noStatusChange() throws TelegramApiException {

        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(false)
                .info(testTrackingInfo)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
        verify(trackingBot, never()).execute(any(SendMessage.class));
        verify(userParcelService, never()).sendNotification(any(), anyString());
    }

    @Test
    @DisplayName("checkTrackingStatuses - ошибка при обновлении статуса")
    void checkTrackingStatuses_updateError() throws TelegramApiException {

        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(false)
                .error("Ошибка API")
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
        verify(trackingBot, never()).execute(any(SendMessage.class));
        verify(userParcelService, never()).sendNotification(any(), anyString());
    }

    @Test
    @DisplayName("checkTrackingStatuses - доставленная посылка отправляет уведомление с эмодзи")
    void checkTrackingStatuses_deliveredParcel() throws TelegramApiException {

        testTrackingInfo.setDelivered(true);

        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(true)
                .oldStatus("В пути")
                .newStatus("Доставлена")
                .info(testTrackingInfo)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);
        when(trackingBot.execute(any(SendMessage.class))).thenReturn(null);

        trackingScheduler.checkTrackingStatuses();

        verify(trackingBot).execute(any(SendMessage.class));
        verify(userParcelService).sendNotification(eq(testUserParcel), anyString());
    }

    @Test
    @DisplayName("checkTrackingStatuses - множественные посылки")
    void checkTrackingStatuses_multipleParcels() throws TelegramApiException {

        UserParcel userParcel2 = UserParcel.builder()
                .id(2L)
                .user(testUser)
                .parcel(Parcel.builder()
                        .id(2L)
                        .trackingNumber("TEST789")
                        .serviceName("Service2")
                        .build())
                .isActive(true)
                .build();

        ParcelUpdateService.ParcelUpdateResult result1 = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(true)
                .info(testTrackingInfo)
                .build();

        ParcelUpdateService.ParcelUpdateResult result2 = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(false)
                .info(testTrackingInfo)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel, userParcel2));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result1);
        when(parcelUpdateService.updateParcelStatus(eq(userParcel2), eq(false))).thenReturn(result2);
        when(trackingBot.execute(any(SendMessage.class))).thenReturn(null);

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService, times(2)).updateParcelStatus(any(), eq(false));
        verify(trackingBot, times(1)).execute(any(SendMessage.class));
        verify(userParcelService, times(1)).sendNotification(any(), anyString());
    }

    @Test
    @DisplayName("checkTrackingStatuses - исключение при обработке")
    void checkTrackingStatuses_exceptionDuringProcessing() throws TelegramApiException {

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenThrow(new RuntimeException("Unexpected error"));

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
        verify(trackingBot, never()).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("checkTrackingStatuses - ошибка при отправке уведомления")
    void checkTrackingStatuses_errorSendingNotification() throws TelegramApiException {

        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .statusChanged(true)
                .info(testTrackingInfo)
                .build();

        when(userParcelService.getParcelsToUpdateWithDetails()).thenReturn(List.of(testUserParcel));
        when(parcelUpdateService.updateParcelStatus(eq(testUserParcel), eq(false)))
                .thenReturn(result);
        when(trackingBot.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("API error"));

        trackingScheduler.checkTrackingStatuses();

        verify(parcelUpdateService).updateParcelStatus(eq(testUserParcel), eq(false));
        verify(trackingBot).execute(any(SendMessage.class));
        // sendNotification не вызывается, потому что была ошибка при отправке
        verify(userParcelService, never()).sendNotification(any(), anyString());
    }
}
