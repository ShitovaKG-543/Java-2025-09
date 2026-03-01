package ru.otus.trackingbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import ru.otus.trackingbot.repository.ParcelStatusHistoryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelStatusHistoryService тесты")
class ParcelStatusHistoryServiceTest {

    private static final Long TEST_PARCEL_ID = 1L;
    private static final String TEST_TRACKING_NUMBER = "RA644000001RU";

    @Mock
    private ParcelStatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private ParcelStatusHistoryService statusHistoryService;

    private Parcel testParcel;
    private ParcelStatusHistory testStatus1;
    private ParcelStatusHistory testStatus2;
    private List<ParcelStatusHistory> testHistory;

    @BeforeEach
    void setUp() {
        LocalDateTime fixedDate = createFixedDate();
        testParcel = Parcel.builder()
                .id(TEST_PARCEL_ID)
                .trackingNumber(TEST_TRACKING_NUMBER)
                .serviceName("Почта России")
                .build();

        testStatus1 = ParcelStatusHistory.builder()
                .id(1L)
                .parcel(testParcel)
                .statusCode("1")
                .statusName("Принято в отделении связи")
                .operationPlace("Москва")
                .operationDate(fixedDate.minusDays(2))
                .weight(1000)
                .isCurrent(false)
                .build();

        testStatus2 = ParcelStatusHistory.builder()
                .id(2L)
                .parcel(testParcel)
                .statusCode("2")
                .statusName("Прибыло в сортировочный центр")
                .operationPlace("Санкт-Петербург")
                .operationDate(fixedDate.minusDays(1))
                .weight(1000)
                .isCurrent(true)
                .build();

        testHistory = Arrays.asList(testStatus2, testStatus1);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getHistoryByParcel
    // =====================================================

    @Test
    @DisplayName("getHistoryByParcel - должен вернуть историю статусов")
    void getHistoryByParcel_ShouldReturnHistory() {

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(testHistory);

        List<ParcelStatusHistory> result = statusHistoryService.getHistoryByParcel(testParcel);

        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(testHistory);
        verify(statusHistoryRepository).findByParcelOrderByOperationDateDesc(testParcel);
    }

    @Test
    @DisplayName("getHistoryByParcel - пустая история должна вернуть пустой список")
    void getHistoryByParcel_EmptyHistory_ShouldReturnEmptyList() {

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(new ArrayList<>());

        List<ParcelStatusHistory> result = statusHistoryService.getHistoryByParcel(testParcel);

        assertThat(result).isEmpty();
        verify(statusHistoryRepository).findByParcelOrderByOperationDateDesc(testParcel);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getLastStatus
    // =====================================================

    @Test
    @DisplayName("getLastStatus - должен вернуть последний статус")
    void getLastStatus_ShouldReturnLastStatus() {

        when(statusHistoryRepository.findFirstByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(Optional.of(testStatus2));

        ParcelStatusHistory result = statusHistoryService.getLastStatus(testParcel);

        assertThat(result).isEqualTo(testStatus2);
        verify(statusHistoryRepository).findFirstByParcelOrderByOperationDateDesc(testParcel);
    }

    @Test
    @DisplayName("getLastStatus - без истории должен вернуть null")
    void getLastStatus_NoHistory_ShouldReturnNull() {

        when(statusHistoryRepository.findFirstByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(Optional.empty());

        ParcelStatusHistory result = statusHistoryService.getLastStatus(testParcel);

        assertThat(result).isNull();
        verify(statusHistoryRepository).findFirstByParcelOrderByOperationDateDesc(testParcel);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ saveStatusHistory
    // =====================================================

    @Test
    @DisplayName("saveStatusHistory - должен сохранить статус")
    void saveStatusHistory_ShouldSaveStatus() {

        ParcelStatusHistory newStatus = ParcelStatusHistory.builder()
                .statusCode("3")
                .statusName("Покинуло сортировочный центр")
                .build();

        statusHistoryService.saveStatusHistory(testParcel, newStatus);

        assertThat(newStatus.getParcel()).isEqualTo(testParcel);
        verify(statusHistoryRepository).save(newStatus);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ saveOnlyNewStatuses
    // =====================================================

    @Test
    @DisplayName("saveOnlyNewStatuses - null список должен вернуть 0")
    void saveOnlyNewStatuses_NullList_ShouldReturnZero() {
        int result = statusHistoryService.saveOnlyNewStatuses(testParcel, null);

        assertThat(result).isZero();
        verify(statusHistoryRepository, never()).findByParcelOrderByOperationDateDesc(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveOnlyNewStatuses - пустой список должен вернуть 0")
    void saveOnlyNewStatuses_EmptyList_ShouldReturnZero() {
        int result = statusHistoryService.saveOnlyNewStatuses(testParcel, new ArrayList<>());

        assertThat(result).isZero();
        verify(statusHistoryRepository, never()).findByParcelOrderByOperationDateDesc(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveOnlyNewStatuses - новые статусы должны быть сохранены")
    void saveOnlyNewStatuses_NewStatuses_ShouldSaveOnlyNewOnes() {

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(testHistory);

        ParcelStatusHistory newStatus = ParcelStatusHistory.builder()
                .statusCode("3")
                .statusName("Покинуло сортировочный центр")
                .operationPlace("Казань")
                .operationDate(LocalDateTime.now())
                .build();

        List<ParcelStatusHistory> newStatuses = Collections.singletonList(newStatus);

        int result = statusHistoryService.saveOnlyNewStatuses(testParcel, newStatuses);

        assertThat(result).isEqualTo(1);
        verify(statusHistoryRepository).save(newStatus);
        verify(statusHistoryRepository).resetCurrentStatus(testParcel);
        verify(statusHistoryRepository).findFirstByParcelOrderByOperationDateDesc(testParcel);
    }

    @Test
    @DisplayName("saveOnlyNewStatuses - дубликаты не должны сохраняться")
    void saveOnlyNewStatuses_DuplicateStatuses_ShouldNotSave() {

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(testHistory);

        // Статус, который уже существует
        ParcelStatusHistory duplicateStatus = ParcelStatusHistory.builder()
                .statusCode("1")
                .statusName("Принято в отделении связи")
                .operationPlace("Москва")
                .operationDate(createFixedDate().minusDays(2))
                .build();

        List<ParcelStatusHistory> newStatuses = Collections.singletonList(duplicateStatus);

        int result = statusHistoryService.saveOnlyNewStatuses(testParcel, newStatuses);

        assertThat(result).isZero();
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveOnlyNewStatuses - смесь новых и существующих статусов")
    void saveOnlyNewStatuses_MixedStatuses_ShouldSaveOnlyNew() {

        LocalDateTime fixedDate = createFixedDate();

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(testHistory);

        ParcelStatusHistory existingStatus = ParcelStatusHistory.builder()
                .statusCode("1")
                .statusName("Принято в отделении связи")
                .operationPlace("Москва")
                .operationDate(fixedDate.minusDays(2))
                .build();

        ParcelStatusHistory newStatus1 = ParcelStatusHistory.builder()
                .statusCode("3")
                .statusName("Покинуло сортировочный центр")
                .operationPlace("Казань")
                .operationDate(fixedDate)
                .build();

        ParcelStatusHistory newStatus2 = ParcelStatusHistory.builder()
                .statusCode("4")
                .statusName("Прибыло в место вручения")
                .operationPlace("Екатеринбург")
                .operationDate(fixedDate)
                .build();

        List<ParcelStatusHistory> newStatuses = Arrays.asList(existingStatus, newStatus1, newStatus2);

        int result = statusHistoryService.saveOnlyNewStatuses(testParcel, newStatuses);

        assertThat(result).isEqualTo(2);
        verify(statusHistoryRepository, times(2)).save(any(ParcelStatusHistory.class));
        verify(statusHistoryRepository).resetCurrentStatus(testParcel);
    }

    @Test
    @DisplayName("saveOnlyNewStatuses - статус с null датой не должен считаться дубликатом")
    void saveOnlyNewStatuses_StatusWithNullDate_ShouldBeSaved() {

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(testHistory);

        ParcelStatusHistory statusWithNullDate = ParcelStatusHistory.builder()
                .statusCode("3")
                .statusName("Новый статус")
                .operationPlace("Москва")
                .operationDate(null)
                .build();

        List<ParcelStatusHistory> newStatuses = Collections.singletonList(statusWithNullDate);

        int result = statusHistoryService.saveOnlyNewStatuses(testParcel, newStatuses);

        assertThat(result).isEqualTo(1);
        verify(statusHistoryRepository).save(statusWithNullDate);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ isSameStatus (через рефлексию)
    // =====================================================

    @Test
    @DisplayName("isSameStatus - одинаковые статусы должны считаться идентичными")
    void isSameStatus_IdenticalStatuses_ShouldReturnTrue() throws Exception {

        java.lang.reflect.Method method = ParcelStatusHistoryService.class.getDeclaredMethod(
                "isSameStatus", ParcelStatusHistory.class, ParcelStatusHistory.class);
        method.setAccessible(true);

        ParcelStatusHistory status1 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        ParcelStatusHistory status2 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        boolean result = (boolean) method.invoke(statusHistoryService, status1, status2);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isSameStatus - разные даты должны считаться разными")
    void isSameStatus_DifferentDates_ShouldReturnFalse() throws Exception {

        java.lang.reflect.Method method = ParcelStatusHistoryService.class.getDeclaredMethod(
                "isSameStatus", ParcelStatusHistory.class, ParcelStatusHistory.class);
        method.setAccessible(true);

        ParcelStatusHistory status1 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        ParcelStatusHistory status2 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(LocalDateTime.of(2024, 1, 16, 10, 30))
                .build();

        boolean result = (boolean) method.invoke(statusHistoryService, status1, status2);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isSameStatus - разные названия должны считаться разными")
    void isSameStatus_DifferentNames_ShouldReturnFalse() throws Exception {

        java.lang.reflect.Method method = ParcelStatusHistoryService.class.getDeclaredMethod(
                "isSameStatus", ParcelStatusHistory.class, ParcelStatusHistory.class);
        method.setAccessible(true);

        ParcelStatusHistory status1 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        ParcelStatusHistory status2 = ParcelStatusHistory.builder()
                .statusName("Доставлен")
                .operationPlace("Москва")
                .operationDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        boolean result = (boolean) method.invoke(statusHistoryService, status1, status2);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isSameStatus - разные места должны считаться разными")
    void isSameStatus_DifferentPlaces_ShouldReturnFalse() throws Exception {

        java.lang.reflect.Method method = ParcelStatusHistoryService.class.getDeclaredMethod(
                "isSameStatus", ParcelStatusHistory.class, ParcelStatusHistory.class);
        method.setAccessible(true);

        ParcelStatusHistory status1 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        ParcelStatusHistory status2 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Санкт-Петербург")
                .operationDate(LocalDateTime.of(2024, 1, 15, 10, 30))
                .build();

        boolean result = (boolean) method.invoke(statusHistoryService, status1, status2);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isSameStatus - null даты с обеих сторон должны считаться одинаковыми")
    void isSameStatus_BothNullDates_ShouldReturnTrue() throws Exception {

        java.lang.reflect.Method method = ParcelStatusHistoryService.class.getDeclaredMethod(
                "isSameStatus", ParcelStatusHistory.class, ParcelStatusHistory.class);
        method.setAccessible(true);

        ParcelStatusHistory status1 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(null)
                .build();

        ParcelStatusHistory status2 = ParcelStatusHistory.builder()
                .statusName("В пути")
                .operationPlace("Москва")
                .operationDate(null)
                .build();

        boolean result = (boolean) method.invoke(statusHistoryService, status1, status2);

        assertThat(result).isTrue();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ updateCurrentStatusFlag
    // =====================================================

    @Test
    @DisplayName("updateCurrentStatusFlag - должен обновить флаг текущего статуса")
    void updateCurrentStatusFlag_ShouldUpdateCurrentFlag() {

        when(statusHistoryRepository.findFirstByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(Optional.of(testStatus2));

        statusHistoryService.updateCurrentStatusFlag(testParcel);

        verify(statusHistoryRepository).resetCurrentStatus(testParcel);
        verify(statusHistoryRepository).findFirstByParcelOrderByOperationDateDesc(testParcel);
        verify(statusHistoryRepository).save(testStatus2);
        assertThat(testStatus2.getIsCurrent()).isTrue();
    }

    @Test
    @DisplayName("updateCurrentStatusFlag - без истории не должен сохранять")
    void updateCurrentStatusFlag_NoHistory_ShouldNotSave() {

        when(statusHistoryRepository.findFirstByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(Optional.empty());

        statusHistoryService.updateCurrentStatusFlag(testParcel);

        verify(statusHistoryRepository).resetCurrentStatus(testParcel);
        verify(statusHistoryRepository).findFirstByParcelOrderByOperationDateDesc(testParcel);
        verify(statusHistoryRepository, never()).save(any(ParcelStatusHistory.class));
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ saveAllStatuses
    // =====================================================

    @Test
    @DisplayName("saveAllStatuses - должен сохранить все статусы")
    void saveAllStatuses_ShouldSaveAllStatuses() {

        List<ParcelStatusHistory> statuses = Arrays.asList(testStatus1, testStatus2);

        statusHistoryService.saveAllStatuses(testParcel, statuses);

        verify(statusHistoryRepository).resetCurrentStatus(testParcel);
        verify(statusHistoryRepository, times(2)).save(any(ParcelStatusHistory.class));

        ArgumentCaptor<ParcelStatusHistory> captor = ArgumentCaptor.forClass(ParcelStatusHistory.class);
        verify(statusHistoryRepository, times(2)).save(captor.capture());

        List<ParcelStatusHistory> savedStatuses = captor.getAllValues();
        assertThat(savedStatuses).hasSize(2);
        assertThat(savedStatuses.get(0).getParcel()).isEqualTo(testParcel);
        assertThat(savedStatuses.get(1).getParcel()).isEqualTo(testParcel);
    }

    @Test
    @DisplayName("saveAllStatuses - пустой список не должен вызывать сохранение")
    void saveAllStatuses_EmptyList_ShouldNotSave() {
        statusHistoryService.saveAllStatuses(testParcel, new ArrayList<>());

        verify(statusHistoryRepository).resetCurrentStatus(testParcel);
        verify(statusHistoryRepository, never()).save(any(ParcelStatusHistory.class));
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ hasStatus
    // =====================================================

    @Test
    @DisplayName("hasStatus - существующий статус должен вернуть true")
    void hasStatus_ExistingStatus_ShouldReturnTrue() {

        when(statusHistoryRepository.existsByParcelAndStatusCode(testParcel, "1"))
                .thenReturn(true);

        boolean result = statusHistoryService.hasStatus(testParcel, "1");

        assertThat(result).isTrue();
        verify(statusHistoryRepository).existsByParcelAndStatusCode(testParcel, "1");
    }

    @Test
    @DisplayName("hasStatus - несуществующий статус должен вернуть false")
    void hasStatus_NonExistingStatus_ShouldReturnFalse() {

        when(statusHistoryRepository.existsByParcelAndStatusCode(testParcel, "999"))
                .thenReturn(false);

        boolean result = statusHistoryService.hasStatus(testParcel, "999");

        assertThat(result).isFalse();
        verify(statusHistoryRepository).existsByParcelAndStatusCode(testParcel, "999");
    }

    @Test
    @DisplayName("hasStatus - null код должен вернуть false")
    void hasStatus_NullCode_ShouldReturnFalse() {

        when(statusHistoryRepository.existsByParcelAndStatusCode(eq(testParcel), isNull()))
                .thenReturn(false);

        boolean result = statusHistoryService.hasStatus(testParcel, null);

        assertThat(result).isFalse();
        verify(statusHistoryRepository).existsByParcelAndStatusCode(testParcel, null);
    }

    // =====================================================
    // ДОПОЛНИТЕЛЬНЫЕ ТЕСТЫ
    // =====================================================

    @Test
    @DisplayName("saveOnlyNewStatuses - должен установить createdAt для новых статусов")
    void saveOnlyNewStatuses_ShouldSetCreatedAt() {

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(testHistory);

        ParcelStatusHistory newStatus = ParcelStatusHistory.builder()
                .statusCode("3")
                .statusName("Покинуло сортировочный центр")
                .build();

        List<ParcelStatusHistory> newStatuses = Collections.singletonList(newStatus);

        statusHistoryService.saveOnlyNewStatuses(testParcel, newStatuses);

        ArgumentCaptor<ParcelStatusHistory> captor = ArgumentCaptor.forClass(ParcelStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());

        ParcelStatusHistory savedStatus = captor.getValue();
        assertThat(savedStatus.getCreatedAt()).isNotNull();
        assertThat(savedStatus.getParcel()).isEqualTo(testParcel);
    }

    @Test
    @DisplayName("saveOnlyNewStatuses - не должен сохранять статусы с одинаковыми полями")
    void saveOnlyNewStatuses_ShouldNotSaveDuplicateWithSameFields() {

        LocalDateTime fixedDate = createFixedDate();

        ParcelStatusHistory existingStatus = ParcelStatusHistory.builder()
                .id(1L)
                .statusCode("1")
                .statusName("Принято в отделении связи")
                .operationPlace("Москва")
                .operationDate(fixedDate)
                .build();

        List<ParcelStatusHistory> testHistory = Collections.singletonList(existingStatus);

        when(statusHistoryRepository.findByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(testHistory);

        ParcelStatusHistory duplicateStatus = ParcelStatusHistory.builder()
                .statusCode("1")
                .statusName("Принято в отделении связи")
                .operationPlace("Москва")
                .operationDate(fixedDate)
                .build();

        List<ParcelStatusHistory> newStatuses = Collections.singletonList(duplicateStatus);

        int result = statusHistoryService.saveOnlyNewStatuses(testParcel, newStatuses);

        assertThat(result).isZero();
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCurrentStatusFlag - должен сбросить старый флаг и установить новый")
    void updateCurrentStatusFlag_ShouldResetOldAndSetNewFlag() {

        ParcelStatusHistory newCurrentStatus = ParcelStatusHistory.builder()
                .id(2L)
                .statusName("Новый статус")
                .isCurrent(false)
                .build();

        when(statusHistoryRepository.findFirstByParcelOrderByOperationDateDesc(testParcel))
                .thenReturn(Optional.of(newCurrentStatus));

        statusHistoryService.updateCurrentStatusFlag(testParcel);

        verify(statusHistoryRepository).resetCurrentStatus(testParcel);
        verify(statusHistoryRepository).save(newCurrentStatus);
        assertThat(newCurrentStatus.getIsCurrent()).isTrue();
    }

    private LocalDateTime createFixedDate() {
        return LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    }
}
