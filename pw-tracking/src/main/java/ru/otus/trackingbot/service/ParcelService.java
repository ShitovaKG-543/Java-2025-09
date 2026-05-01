package ru.otus.trackingbot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.ParcelStatusHistory;
import ru.otus.trackingbot.model.Operation;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.repository.ParcelRepository;

/**
 * Сервис для управления посылками.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *     <li>Поиска и создания посылок</li>
 *     <li>Обновления статусов посылок</li>
 *     <li>Получения истории статусов</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
public class ParcelService {

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private ParcelStatusHistoryService statusHistoryService;

    @Autowired
    private TrackingCacheService trackingCacheService;

    /**
     * Находит посылку по трек-номеру или создает новую, если не найдена.
     *
     * @param trackingNumber трек-номер посылки
     * @param serviceName название службы доставки
     * @return найденная или созданная посылка
     */
    @Transactional
    public Parcel getOrCreateParcel(String trackingNumber, String serviceName) {
        Optional<Parcel> existingParcel = parcelRepository.findByTrackingNumber(trackingNumber);

        if (existingParcel.isPresent()) {
            return existingParcel.get();
        } else {
            Parcel newParcel = Parcel.builder()
                    .trackingNumber(trackingNumber)
                    .serviceName(serviceName)
                    .build();
            return parcelRepository.save(newParcel);
        }
    }

    /**
     * Обновляет статус посылки, добавляя только новые операции из истории.
     *
     * @param parcel посылка для обновления
     * @param trackingInfo информация об отслеживании
     * @return true если были добавлены новые статусы или обновлена информация
     */
    @Transactional
    public boolean updateParcelStatus(Parcel parcel, TrackingInfo trackingInfo) {
        boolean hasUpdates = false;

        // Обновляем основную информацию о посылке
        if (trackingInfo.getWeight() != null && trackingInfo.getWeight() > 0) {
            BigDecimal newWeight = BigDecimal.valueOf(trackingInfo.getWeight());
            if (parcel.getWeight() == null || parcel.getWeight().compareTo(newWeight) != 0) {
                parcel.setWeight(newWeight);
                hasUpdates = true;
            }
        }

        // Обновляем описание, если оно изменилось
        if (trackingInfo.getStatusDescription() != null) {
            String newDescription = trackingInfo.getStatusDescription();
            if (parcel.getDescription() == null || !parcel.getDescription().equals(newDescription)) {
                parcel.setDescription(newDescription);
                hasUpdates = true;
            }
        }

        parcel.updateLastUpdated();
        parcelRepository.save(parcel);

        // Сохраняем только новые статусы из истории
        if (trackingInfo.getAllOperations() != null
                && !trackingInfo.getAllOperations().isEmpty()) {
            List<ParcelStatusHistory> statuses = convertOperationsToStatusHistory(parcel, trackingInfo);
            int addedCount = statusHistoryService.saveOnlyNewStatuses(parcel, statuses);
            hasUpdates = hasUpdates || (addedCount > 0);

            if (addedCount > 0) {
                log.info("Для посылки {} добавлено {} новых статусов", parcel.getTrackingNumber(), addedCount);
            }
        }

        // Обновляем кеш после сохранения в БД
        if (hasUpdates) {
            trackingCacheService.updateCache(parcel.getTrackingNumber(), trackingInfo);
            log.info("✅ Кеш обновлен для посылки {} после сохранения в БД", parcel.getTrackingNumber());
        }

        return hasUpdates;
    }

    /**
     * Конвертирует операции из TrackingInfo в список ParcelStatusHistory.
     */
    private List<ParcelStatusHistory> convertOperationsToStatusHistory(Parcel parcel, TrackingInfo trackingInfo) {
        List<ParcelStatusHistory> statuses = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(); // Одно время для всех операций

        for (Operation operation : trackingInfo.getAllOperations()) {
            ParcelStatusHistory statusHistory = ParcelStatusHistory.builder()
                    .parcel(parcel)
                    .statusCode(operation.getOperationId())
                    .statusName(operation.getOperationName())
                    .statusDescription(trackingInfo.getStatusDescription())
                    .operationPlace(operation.getOperationPlace())
                    .operationDate(operation.getDate())
                    .weight(operation.getWeight())
                    .isCurrent(false) // ← Явно!
                    .createdAt(now) // ← Явно! Одно время для всех
                    .build();
            statuses.add(statusHistory);
        }

        return statuses;
    }

    /**
     * Находит посылку по трек-номеру.
     *
     * @param trackingNumber трек-номер
     * @return Optional с посылкой
     */
    @Transactional(readOnly = true)
    public Optional<Parcel> findByTrackingNumber(String trackingNumber) {
        return parcelRepository.findByTrackingNumber(trackingNumber);
    }

    /**
     * Возвращает полную историю статусов посылки.
     *
     * @param parcel посылка
     * @return список статусов в хронологическом порядке
     */
    @Transactional(readOnly = true)
    public List<ParcelStatusHistory> getParcelHistory(Parcel parcel) {
        return statusHistoryService.getHistoryByParcel(parcel);
    }

    /**
     * Возвращает последний статус посылки.
     *
     * @param parcel посылка
     * @return последний статус или null
     */
    @Transactional(readOnly = true)
    public ParcelStatusHistory getLastParcelStatus(Parcel parcel) {
        return statusHistoryService.getLastStatus(parcel);
    }
}
