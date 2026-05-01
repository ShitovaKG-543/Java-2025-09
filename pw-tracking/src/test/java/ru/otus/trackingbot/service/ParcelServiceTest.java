package ru.otus.trackingbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.ParcelStatusHistory;
import ru.otus.trackingbot.model.Operation;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.repository.ParcelRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelService тесты")
class ParcelServiceTest {

    private static final String TEST_TRACKING_NUMBER = "RA644000001RU";
    private static final String TEST_SERVICE_NAME = "Почта России";

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private ParcelStatusHistoryService statusHistoryService;

    @Mock
    private TrackingCacheService trackingCacheService;

    @InjectMocks
    private ParcelService parcelService;

    private Parcel testParcel;
    private TrackingInfo testTrackingInfo;
    private List<Operation> testOperations;

    @BeforeEach
    void setUp() {
        // Исправлено: у Parcel больше нет поля id
        testParcel = Parcel.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .createdAt(LocalDateTime.now())
                .build();

        // Остальная инициализация без изменений
        Operation operation1 = Operation.builder()
                .operationId("1")
                .operationName("Принято в отделении связи")
                .operationPlace("Москва")
                .date(LocalDateTime.now().minusDays(2))
                .weight(1000)
                .build();

        Operation operation2 = Operation.builder()
                .operationId("2")
                .operationName("Прибыло в сортировочный центр")
                .operationPlace("Санкт-Петербург")
                .date(LocalDateTime.now().minusDays(1))
                .weight(1000)
                .build();

        testOperations = Arrays.asList(operation1, operation2);

        testTrackingInfo = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .success(true)
                .status("В пути")
                .statusDescription("Посылка в пути")
                .delivered(false)
                .weight(1.0)
                .lastOperation(operation2)
                .allOperations(testOperations)
                .lastCheck(LocalDateTime.now())
                .build();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getOrCreateParcel
    // =====================================================
    @Test
    @DisplayName("getOrCreateParcel - существующая посылка должна быть возвращена")
    void getOrCreateParcel_ExistingParcel_ShouldReturnExisting() {
        assertThat(testParcel).isNotNull();

        // Мокаем findByTrackingNumber, а не findById!
        when(parcelRepository.findByTrackingNumber(TEST_TRACKING_NUMBER)).thenReturn(Optional.of(testParcel));

        Parcel result = parcelService.getOrCreateParcel(TEST_TRACKING_NUMBER, TEST_SERVICE_NAME);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testParcel);
        assertThat(result.getTrackingNumber()).isEqualTo(TEST_TRACKING_NUMBER);
        verify(parcelRepository, never()).save(any(Parcel.class));
    }

    @Test
    @DisplayName("getOrCreateParcel - новая посылка должна быть создана")
    void getOrCreateParcel_NewParcel_ShouldCreateAndReturn() {

        when(parcelRepository.findByTrackingNumber(TEST_TRACKING_NUMBER)).thenReturn(Optional.empty());
        when(parcelRepository.save(any(Parcel.class))).thenReturn(testParcel);

        Parcel result = parcelService.getOrCreateParcel(TEST_TRACKING_NUMBER, TEST_SERVICE_NAME);

        assertThat(result).isEqualTo(testParcel);
        verify(parcelRepository).findByTrackingNumber(TEST_TRACKING_NUMBER);
        verify(parcelRepository).save(any(Parcel.class));
    }

    @Test
    @DisplayName("getOrCreateParcel - новая посылка должна иметь правильные поля")
    void getOrCreateParcel_NewParcel_ShouldHaveCorrectFields() {
        ArgumentCaptor<Parcel> parcelCaptor = ArgumentCaptor.forClass(Parcel.class);

        when(parcelRepository.findByTrackingNumber(TEST_TRACKING_NUMBER)).thenReturn(Optional.empty());
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Parcel result = parcelService.getOrCreateParcel(TEST_TRACKING_NUMBER, TEST_SERVICE_NAME);

        verify(parcelRepository).findByTrackingNumber(TEST_TRACKING_NUMBER);
        verify(parcelRepository).save(parcelCaptor.capture());

        Parcel savedParcel = parcelCaptor.getValue();
        assertThat(savedParcel.getTrackingNumber()).isEqualTo(TEST_TRACKING_NUMBER);
        assertThat(savedParcel.getServiceName()).isEqualTo(TEST_SERVICE_NAME);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ updateParcelStatus
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - новый вес должен обновить посылку")
    void updateParcelStatus_NewWeight_ShouldUpdateParcel() {
        testParcel.setWeight(null);
        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(0);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, testTrackingInfo);

        assertThat(hasUpdates).isTrue();
        assertThat(testParcel.getWeight()).isEqualTo(BigDecimal.valueOf(1.0));
        verify(parcelRepository).save(testParcel);
        verify(trackingCacheService).updateCache(TEST_TRACKING_NUMBER, testTrackingInfo);
    }

    @Test
    @DisplayName("updateParcelStatus - вес не изменился не должен обновлять вес")
    void updateParcelStatus_SameWeight_ShouldNotUpdateWeight() {
        testParcel.setWeight(BigDecimal.valueOf(1.0));
        testParcel.setDescription("Посылка в пути");
        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(0);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, testTrackingInfo);

        assertThat(hasUpdates).isFalse();
        assertThat(testParcel.getWeight()).isEqualTo(BigDecimal.valueOf(1.0));
        verify(parcelRepository).save(testParcel);
        verify(trackingCacheService, never()).updateCache(anyString(), any(TrackingInfo.class));
    }

    @Test
    @DisplayName("updateParcelStatus - новое описание должно обновить посылку")
    void updateParcelStatus_NewDescription_ShouldUpdateParcel() {
        testParcel.setDescription(null);
        testParcel.setWeight(BigDecimal.valueOf(1.0));
        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(0);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, testTrackingInfo);

        assertThat(hasUpdates).isTrue();
        assertThat(testParcel.getDescription()).isEqualTo("Посылка в пути");
        verify(parcelRepository).save(testParcel);
        verify(trackingCacheService).updateCache(TEST_TRACKING_NUMBER, testTrackingInfo);
    }

    @Test
    @DisplayName("updateParcelStatus - новые статусы должны быть сохранены")
    void updateParcelStatus_NewStatuses_ShouldSaveStatuses() {
        testParcel.setWeight(BigDecimal.valueOf(1.0));
        testParcel.setDescription("Посылка в пути");
        when(statusHistoryService.saveOnlyNewStatuses(eq(testParcel), anyList()))
                .thenReturn(2);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, testTrackingInfo);

        assertThat(hasUpdates).isTrue();
        verify(statusHistoryService).saveOnlyNewStatuses(eq(testParcel), anyList());
        verify(trackingCacheService).updateCache(TEST_TRACKING_NUMBER, testTrackingInfo);
    }

    @Test
    @DisplayName("updateParcelStatus - без изменений должно вернуть false")
    void updateParcelStatus_NoChanges_ShouldReturnFalse() {
        testParcel.setWeight(BigDecimal.valueOf(1.0));
        testParcel.setDescription("Посылка в пути");
        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(0);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, testTrackingInfo);

        assertThat(hasUpdates).isFalse();
        verify(trackingCacheService, never()).updateCache(anyString(), any(TrackingInfo.class));
    }

    @Test
    @DisplayName("updateParcelStatus - null операции не должны вызывать сохранение истории")
    void updateParcelStatus_NullOperations_ShouldNotSaveHistory() {
        TrackingInfo infoWithoutOperations = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .success(true)
                .status("В пути")
                .weight(1.0)
                .statusDescription("Посылка в пути")
                .allOperations(null)
                .build();

        testParcel.setWeight(null);
        testParcel.setDescription(null);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, infoWithoutOperations);

        assertThat(hasUpdates).isTrue();
        verify(statusHistoryService, never()).saveOnlyNewStatuses(any(Parcel.class), anyList());
    }

    @Test
    @DisplayName("updateParcelStatus - пустой список операций не должен вызывать сохранение истории")
    void updateParcelStatus_EmptyOperations_ShouldNotSaveHistory() {
        TrackingInfo infoWithEmptyOperations = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .success(true)
                .status("В пути")
                .weight(1.0)
                .statusDescription("Посылка в пути")
                .allOperations(new ArrayList<>())
                .build();

        testParcel.setWeight(null);
        testParcel.setDescription(null);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, infoWithEmptyOperations);

        assertThat(hasUpdates).isTrue();
        verify(statusHistoryService, never()).saveOnlyNewStatuses(any(Parcel.class), anyList());
    }

    @Test
    @DisplayName("updateParcelStatus - null вес не должен обновлять посылку")
    void updateParcelStatus_NullWeight_ShouldNotUpdateWeight() {
        TrackingInfo infoWithoutWeight = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .success(true)
                .status("В пути")
                .weight(null)
                .statusDescription("Посылка в пути")
                .allOperations(testOperations)
                .build();

        testParcel.setWeight(null);
        testParcel.setDescription(null);
        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(2);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, infoWithoutWeight);

        assertThat(hasUpdates).isTrue();
        assertThat(testParcel.getWeight()).isNull();
    }

    @Test
    @DisplayName("updateParcelStatus - вес 0 не должен обновлять посылку")
    void updateParcelStatus_ZeroWeight_ShouldNotUpdateWeight() {
        TrackingInfo infoWithZeroWeight = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .success(true)
                .status("В пути")
                .weight(0.0)
                .statusDescription("Посылка в пути")
                .allOperations(testOperations)
                .build();

        testParcel.setWeight(null);
        testParcel.setDescription(null);
        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(2);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, infoWithZeroWeight);

        assertThat(hasUpdates).isTrue();
        assertThat(testParcel.getWeight()).isNull();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ findByTrackingNumber
    // =====================================================

    @Test
    @DisplayName("findByTrackingNumber - существующая посылка должна быть найдена")
    void findByTrackingNumber_ExistingParcel_ShouldReturnParcel() {
        when(parcelRepository.findByTrackingNumber(TEST_TRACKING_NUMBER)).thenReturn(Optional.of(testParcel));

        Optional<Parcel> result = parcelService.findByTrackingNumber(TEST_TRACKING_NUMBER);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testParcel);
        verify(parcelRepository).findByTrackingNumber(TEST_TRACKING_NUMBER);
    }

    @Test
    @DisplayName("findByTrackingNumber - несуществующая посылка должна вернуть пустой Optional")
    void findByTrackingNumber_NonExistingParcel_ShouldReturnEmpty() {

        Optional<Parcel> result = parcelService.findByTrackingNumber(TEST_TRACKING_NUMBER);

        assertThat(result).isEmpty();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getParcelHistory
    // =====================================================

    @Test
    @DisplayName("getParcelHistory - должен вернуть историю статусов")
    void getParcelHistory_ShouldReturnHistory() {
        List<ParcelStatusHistory> history = new ArrayList<>();
        when(statusHistoryService.getHistoryByParcel(testParcel)).thenReturn(history);

        List<ParcelStatusHistory> result = parcelService.getParcelHistory(testParcel);

        assertThat(result).isEqualTo(history);
        verify(statusHistoryService).getHistoryByParcel(testParcel);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getLastParcelStatus
    // =====================================================

    @Test
    @DisplayName("getLastParcelStatus - должен вернуть последний статус")
    void getLastParcelStatus_ShouldReturnLastStatus() {
        ParcelStatusHistory lastStatus =
                ParcelStatusHistory.builder().id(1L).statusName("В пути").build();
        when(statusHistoryService.getLastStatus(testParcel)).thenReturn(lastStatus);

        ParcelStatusHistory result = parcelService.getLastParcelStatus(testParcel);

        assertThat(result).isEqualTo(lastStatus);
        verify(statusHistoryService).getLastStatus(testParcel);
    }

    @Test
    @DisplayName("getLastParcelStatus - без истории должен вернуть null")
    void getLastParcelStatus_NoHistory_ShouldReturnNull() {
        when(statusHistoryService.getLastStatus(testParcel)).thenReturn(null);

        ParcelStatusHistory result = parcelService.getLastParcelStatus(testParcel);

        assertThat(result).isNull();
        verify(statusHistoryService).getLastStatus(testParcel);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ КОНВЕРТАЦИИ ОПЕРАЦИЙ
    // =====================================================
    @Test
    @DisplayName("convertOperationsToStatusHistory - должен корректно конвертировать операции")
    void convertOperationsToStatusHistory_ShouldConvertCorrectly() throws Exception {
        java.lang.reflect.Method method = ParcelService.class.getDeclaredMethod(
                "convertOperationsToStatusHistory", Parcel.class, TrackingInfo.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<ParcelStatusHistory> result =
                (List<ParcelStatusHistory>) method.invoke(parcelService, testParcel, testTrackingInfo);

        assertThat(result).hasSize(2);

        ParcelStatusHistory firstStatus = result.get(0);
        assertThat(firstStatus.getParcel()).isEqualTo(testParcel);
        assertThat(firstStatus.getStatusCode()).isEqualTo("1");
        assertThat(firstStatus.getStatusName()).isEqualTo("Принято в отделении связи");
        assertThat(firstStatus.getStatusDescription()).isEqualTo("Посылка в пути");
        assertThat(firstStatus.getOperationPlace()).isEqualTo("Москва");
        assertThat(firstStatus.getWeight()).isEqualTo(1000);
        // isCurrent устанавливается позже в updateCurrentStatusFlag
        assertThat(firstStatus.getIsCurrent()).isFalse();

        ParcelStatusHistory secondStatus = result.get(1);
        assertThat(secondStatus.getStatusCode()).isEqualTo("2");
        assertThat(secondStatus.getStatusName()).isEqualTo("Прибыло в сортировочный центр");
        // isCurrent устанавливается позже в updateCurrentStatusFlag
        assertThat(secondStatus.getIsCurrent()).isFalse();
    }

    // =====================================================
    // ДОПОЛНИТЕЛЬНЫЕ ТЕСТЫ
    // =====================================================

    @Test
    @DisplayName("updateParcelStatus - обновление веса с BigDecimal на Double")
    void updateParcelStatus_WeightConversion_ShouldWorkCorrectly() {
        testParcel.setWeight(BigDecimal.valueOf(0.5));
        testParcel.setDescription("Посылка в пути");
        TrackingInfo infoWithNewWeight = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .success(true)
                .status("В пути")
                .statusDescription("Посылка в пути")
                .weight(1.5)
                .allOperations(testOperations)
                .build();

        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(0);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, infoWithNewWeight);

        assertThat(hasUpdates).isTrue();
        assertThat(testParcel.getWeight()).isEqualTo(BigDecimal.valueOf(1.5));
        verify(trackingCacheService).updateCache(TEST_TRACKING_NUMBER, infoWithNewWeight);
    }

    @Test
    @DisplayName("updateParcelStatus - множество изменений одновременно")
    void updateParcelStatus_MultipleUpdates_ShouldHandleAll() {
        testParcel.setWeight(BigDecimal.valueOf(0.5));
        testParcel.setDescription("Старое описание");
        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(2);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, testTrackingInfo);

        assertThat(hasUpdates).isTrue();
        assertThat(testParcel.getWeight()).isEqualTo(BigDecimal.valueOf(1.0));
        assertThat(testParcel.getDescription()).isEqualTo("Посылка в пути");
        verify(statusHistoryService).saveOnlyNewStatuses(eq(testParcel), anyList());
        verify(trackingCacheService).updateCache(TEST_TRACKING_NUMBER, testTrackingInfo);
    }

    @Test
    @DisplayName("updateParcelStatus - только обновление lastUpdated без других изменений")
    void updateParcelStatus_OnlyLastUpdated_ShouldReturnFalse() {
        testParcel.setWeight(BigDecimal.valueOf(1.0));
        testParcel.setDescription("Посылка в пути");
        TrackingInfo sameInfo = TrackingInfo.builder()
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName(TEST_SERVICE_NAME)
                .success(true)
                .status("В пути")
                .statusDescription("Посылка в пути")
                .weight(1.0)
                .allOperations(testOperations)
                .build();

        when(statusHistoryService.saveOnlyNewStatuses(any(Parcel.class), anyList()))
                .thenReturn(0);

        boolean hasUpdates = parcelService.updateParcelStatus(testParcel, sameInfo);

        assertThat(hasUpdates).isFalse();
        verify(trackingCacheService, never()).updateCache(anyString(), any(TrackingInfo.class));
    }
}
