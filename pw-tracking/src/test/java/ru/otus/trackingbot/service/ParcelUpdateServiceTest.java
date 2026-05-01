package ru.otus.trackingbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.ParcelStatusHistory;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelUpdateService тесты")
class ParcelUpdateServiceTest {

    private static final String TEST_TRACKING_NUMBER = "RA644000001RU";
    private static final Long TEST_USER_PARCEL_ID = 100L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private ParcelService parcelService;

    @Mock
    private UserParcelService userParcelService;

    @Mock
    private TrackingCacheService trackingCacheService;

    @InjectMocks
    private ParcelUpdateService parcelUpdateService;

    private User testUser;
    private Parcel testParcel;
    private UserParcel testUserParcel;
    private TrackingInfo testTrackingInfo;
    private ParcelStatusHistory testLastStatus;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(TEST_USER_ID)
                .chatId(123456789L)
                .username("test_user")
                .notificationEnabled(true)
                .build();

        // Исправлено: у Parcel больше нет поля id
        testParcel = Parcel.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .build();

        testUserParcel = UserParcel.builder()
                .id(TEST_USER_PARCEL_ID)
                .user(testUser)
                .parcel(testParcel)
                .isActive(true)
                .lastStatus("В пути")
                .lastStatusDescription("Посылка в пути")
                .lastChecked(LocalDateTime.now().minusHours(1))
                .build();

        testTrackingInfo = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(true)
                .status("В пути")
                .statusDescription("Посылка в пути")
                .delivered(false)
                .weight(1.0)
                .lastCheck(LocalDateTime.now())
                .build();

        testLastStatus = ParcelStatusHistory.builder()
                .id(1L)
                .parcel(testParcel)
                .statusName("В пути")
                .statusDescription("Посылка в пути")
                .operationDate(LocalDateTime.now())
                .build();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ updateParcelStatus С forceFresh = false
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - успешное обновление без изменений статуса")
    void updateParcelStatus_SuccessfulUpdateWithoutStatusChange() {
        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(false);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(testLastStatus);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getError()).isNull();
        assertThat(result.getOldStatus()).isEqualTo("В пути");
        assertThat(result.getNewStatus()).isEqualTo("В пути");
        assertThat(result.isStatusChanged()).isFalse();
        assertThat(result.isHasNewStatuses()).isFalse();
        assertThat(result.getInfo()).isEqualTo(testTrackingInfo);

        verify(userParcelService).updateUserParcelStatus(testUserParcel, testTrackingInfo);
        assertThat(testUserParcel.getLastStatus()).isEqualTo("В пути");
        assertThat(testUserParcel.getLastStatusDescription()).isEqualTo("Посылка в пути");
        assertThat(testUserParcel.getLastChecked()).isNotNull();
    }

    @Test
    @DisplayName("updateParcelStatus - успешное обновление с изменением статуса")
    void updateParcelStatus_SuccessfulUpdateWithStatusChange() {
        TrackingInfo infoWithNewStatus = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(true)
                .status("Доставлен")
                .statusDescription("Посылка доставлена получателю")
                .delivered(true)
                .weight(1.0)
                .lastCheck(LocalDateTime.now())
                .build();

        ParcelStatusHistory newLastStatus = ParcelStatusHistory.builder()
                .id(2L)
                .parcel(testParcel)
                .statusName("Доставлен")
                .statusDescription("Посылка доставлена получателю")
                .operationDate(LocalDateTime.now())
                .build();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(infoWithNewStatus);
        when(parcelService.updateParcelStatus(testParcel, infoWithNewStatus)).thenReturn(true);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(newLastStatus);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOldStatus()).isEqualTo("В пути");
        assertThat(result.getNewStatus()).isEqualTo("Доставлен");
        assertThat(result.isStatusChanged()).isTrue();
        assertThat(result.isHasNewStatuses()).isTrue();

        verify(userParcelService).updateUserParcelStatus(testUserParcel, infoWithNewStatus);
        assertThat(testUserParcel.getLastStatus()).isEqualTo("Доставлен");
        assertThat(testUserParcel.getLastStatusDescription()).isEqualTo("Посылка доставлена получателю");
    }

    @Test
    @DisplayName("updateParcelStatus - успешное обновление с null старым статусом")
    void updateParcelStatus_SuccessfulUpdateWithNullOldStatus() {
        testUserParcel.setLastStatus(null);

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(true);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(testLastStatus);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOldStatus()).isNull();
        assertThat(result.getNewStatus()).isEqualTo("В пути");
        assertThat(result.isStatusChanged()).isTrue();
    }

    @Test
    @DisplayName("updateParcelStatus - успешное обновление с null последним статусом")
    void updateParcelStatus_SuccessfulUpdateWithNullLastStatus() {
        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(true);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(null);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOldStatus()).isEqualTo("В пути");
        assertThat(result.getNewStatus()).isEqualTo(testTrackingInfo.getStatus());
        assertThat(result.isStatusChanged()).isFalse();
        assertThat(result.isHasNewStatuses()).isTrue();
    }

    @Test
    @DisplayName("updateParcelStatus - успешное обновление с null последним статусом и новым статусом")
    void updateParcelStatus_SuccessfulUpdateWithNullLastStatusAndNewStatus() {
        TrackingInfo infoWithNewStatus = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(true)
                .status("Доставлен")
                .statusDescription("Посылка доставлена")
                .delivered(true)
                .weight(1.0)
                .lastCheck(LocalDateTime.now())
                .build();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(infoWithNewStatus);
        when(parcelService.updateParcelStatus(testParcel, infoWithNewStatus)).thenReturn(true);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(null);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOldStatus()).isEqualTo("В пути");
        assertThat(result.getNewStatus()).isEqualTo("Доставлен");
        assertThat(result.isStatusChanged()).isTrue();
        assertThat(result.isHasNewStatuses()).isTrue();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ updateParcelStatus С forceFresh = true
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - принудительное обновление (forceFresh = true)")
    void updateParcelStatus_ForceFresh_ShouldUseFreshTrackingInfo() {
        when(trackingCacheService.getFreshTrackingInfo(TEST_TRACKING_NUMBER)).thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(false);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(testLastStatus);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, true);

        assertThat(result.isSuccess()).isTrue();
        verify(trackingCacheService).getFreshTrackingInfo(TEST_TRACKING_NUMBER);
        verify(trackingCacheService, never()).getTrackingInfoWithFreshnessCheck(anyString(), anyInt());
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ ОБРАБОТКИ ОШИБОК
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - ошибка получения информации")
    void updateParcelStatus_InfoError_ShouldReturnError() {
        TrackingInfo errorInfo = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(false)
                .error("API connection failed")
                .build();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(errorInfo);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Ошибка получения информации: API connection failed");
        assertThat(result.getInfo()).isNull();

        verify(parcelService, never()).updateParcelStatus(any(), any());
        verify(userParcelService, never()).updateUserParcelStatus(any(), any());
    }

    @Test
    @DisplayName("updateParcelStatus - ошибка с пустым сообщением")
    void updateParcelStatus_ErrorWithEmptyMessage_ShouldReturnDefaultError() {
        TrackingInfo errorInfo = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(false)
                .error("")
                .build();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(errorInfo);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Ошибка получения информации: ");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ ParcelUpdateResult
    // =====================================================

    @Test
    @DisplayName("ParcelUpdateResult.error - должен создать результат с ошибкой")
    void parcelUpdateResult_Error_ShouldCreateErrorResult() {
        String errorMessage = "Test error message";

        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.error(errorMessage);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo(errorMessage);
        assertThat(result.getInfo()).isNull();
        assertThat(result.getOldStatus()).isNull();
        assertThat(result.getNewStatus()).isNull();
        assertThat(result.isStatusChanged()).isFalse();
        assertThat(result.isHasNewStatuses()).isFalse();
    }

    @Test
    @DisplayName("ParcelUpdateResult - builder должен корректно создавать результат")
    void parcelUpdateResult_Builder_ShouldCreateCorrectResult() {
        ParcelUpdateService.ParcelUpdateResult result = ParcelUpdateService.ParcelUpdateResult.builder()
                .success(true)
                .error(null)
                .info(testTrackingInfo)
                .oldStatus("Старый статус")
                .newStatus("Новый статус")
                .statusChanged(true)
                .hasNewStatuses(true)
                .build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getError()).isNull();
        assertThat(result.getInfo()).isEqualTo(testTrackingInfo);
        assertThat(result.getOldStatus()).isEqualTo("Старый статус");
        assertThat(result.getNewStatus()).isEqualTo("Новый статус");
        assertThat(result.isStatusChanged()).isTrue();
        assertThat(result.isHasNewStatuses()).isTrue();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ ОБНОВЛЕНИЯ ПОЛЕЙ USER_PARCEL
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - должен обновить lastChecked")
    void updateParcelStatus_ShouldUpdateLastChecked() {
        LocalDateTime beforeUpdate = LocalDateTime.now();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(false);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(testLastStatus);

        parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(testUserParcel.getLastChecked()).isAfterOrEqualTo(beforeUpdate);
        verify(userParcelService).updateUserParcelStatus(testUserParcel, testTrackingInfo);
    }

    @Test
    @DisplayName("updateParcelStatus - должен обновить lastStatus и lastStatusDescription")
    void updateParcelStatus_ShouldUpdateLastStatusAndDescription() {
        TrackingInfo newInfo = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(true)
                .status("Новый статус")
                .statusDescription("Новое описание статуса")
                .delivered(false)
                .lastCheck(LocalDateTime.now())
                .build();

        ParcelStatusHistory newLastStatus = ParcelStatusHistory.builder()
                .id(2L)
                .parcel(testParcel)
                .statusName("Новый статус")
                .build();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(newInfo);
        when(parcelService.updateParcelStatus(testParcel, newInfo)).thenReturn(true);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(newLastStatus);

        parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(testUserParcel.getLastStatus()).isEqualTo("Новый статус");
        assertThat(testUserParcel.getLastStatusDescription()).isEqualTo("Новое описание статуса");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ ПРОВЕРКИ ПАРАМЕТРОВ ВЫЗОВОВ
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - должен передать правильный maxAgeSeconds в кеш")
    void updateParcelStatus_ShouldPassCorrectMaxAgeSeconds() {
        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(eq(TEST_TRACKING_NUMBER), eq(3600)))
                .thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(false);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(testLastStatus);

        parcelUpdateService.updateParcelStatus(testUserParcel, false);

        verify(trackingCacheService).getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600);
    }

    @Test
    @DisplayName("updateParcelStatus - forceFresh = true должен игнорировать кеш")
    void updateParcelStatus_ForceFreshTrue_ShouldIgnoreCache() {
        when(trackingCacheService.getFreshTrackingInfo(TEST_TRACKING_NUMBER)).thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(false);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(testLastStatus);

        parcelUpdateService.updateParcelStatus(testUserParcel, true);

        verify(trackingCacheService).getFreshTrackingInfo(TEST_TRACKING_NUMBER);
        verify(trackingCacheService, never()).getTrackingInfoWithFreshnessCheck(anyString(), anyInt());
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ ПОГРАНИЧНЫХ СЛУЧАЕВ
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - старый и новый статус null")
    void updateParcelStatus_BothOldAndNewStatusNull() {
        testUserParcel.setLastStatus(null);

        TrackingInfo infoWithNullStatus = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(true)
                .status(null)
                .statusDescription(null)
                .lastCheck(LocalDateTime.now())
                .build();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(infoWithNullStatus);
        when(parcelService.updateParcelStatus(testParcel, infoWithNullStatus)).thenReturn(false);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(null);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOldStatus()).isNull();
        assertThat(result.getNewStatus()).isNull();
        assertThat(result.isStatusChanged()).isFalse();
    }

    @Test
    @DisplayName("updateParcelStatus - только hasNewStatuses = true без изменения статуса")
    void updateParcelStatus_OnlyHasNewStatusesTrue_WithoutStatusChange() {
        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(testTrackingInfo);
        when(parcelService.updateParcelStatus(testParcel, testTrackingInfo)).thenReturn(true);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(testLastStatus);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isStatusChanged()).isFalse();
        assertThat(result.isHasNewStatuses()).isTrue();
    }

    @Test
    @DisplayName("updateParcelStatus - только statusChanged = true без новых записей")
    void updateParcelStatus_OnlyStatusChangedTrue_WithoutNewRecords() {
        TrackingInfo infoWithNewStatus = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .success(true)
                .status("Доставлен")
                .statusDescription("Посылка доставлена")
                .lastCheck(LocalDateTime.now())
                .build();

        ParcelStatusHistory newLastStatus = ParcelStatusHistory.builder()
                .id(2L)
                .parcel(testParcel)
                .statusName("Доставлен")
                .build();

        when(trackingCacheService.getTrackingInfoWithFreshnessCheck(TEST_TRACKING_NUMBER, 3600))
                .thenReturn(infoWithNewStatus);
        when(parcelService.updateParcelStatus(testParcel, infoWithNewStatus)).thenReturn(false);
        when(parcelService.getLastParcelStatus(testParcel)).thenReturn(newLastStatus);

        ParcelUpdateService.ParcelUpdateResult result = parcelUpdateService.updateParcelStatus(testUserParcel, false);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isStatusChanged()).isTrue();
        assertThat(result.isHasNewStatuses()).isFalse();
    }
}
