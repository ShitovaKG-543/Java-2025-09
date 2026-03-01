package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.ParcelStatusHistory;
import ru.otus.trackingbot.repository.ParcelStatusHistoryRepository;

/**
 * Сервис для работы с историей статусов посылок.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *     <li>Получения истории статусов</li>
 *     <li>Сохранения новых статусов с проверкой дубликатов</li>
 *     <li>Управления флагом текущего статуса</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
public class ParcelStatusHistoryService {

    @Autowired
    private ParcelStatusHistoryRepository statusHistoryRepository;

    /**
     * Возвращает историю статусов для указанной посылки.
     *
     * @param parcel посылка
     * @return список статусов, отсортированный по дате (от новых к старым)
     */
    @Transactional(readOnly = true)
    public List<ParcelStatusHistory> getHistoryByParcel(Parcel parcel) {
        return statusHistoryRepository.findByParcelOrderByOperationDateDesc(parcel);
    }

    /**
     * Возвращает последний (самый новый) статус посылки.
     *
     * @param parcel посылка
     * @return последний статус или null
     */
    @Transactional(readOnly = true)
    public ParcelStatusHistory getLastStatus(Parcel parcel) {
        return statusHistoryRepository
                .findFirstByParcelOrderByOperationDateDesc(parcel)
                .orElse(null);
    }

    /**
     * Сохраняет запись об изменении статуса.
     *
     * @param parcel посылка
     * @param statusHistory запись статуса
     */
    @Transactional
    public void saveStatusHistory(Parcel parcel, ParcelStatusHistory statusHistory) {
        statusHistory.setParcel(parcel);
        statusHistoryRepository.save(statusHistory);
    }

    /**
     * Сохраняет только новые статусы, которых еще нет в базе.
     * <p>
     * Для каждого нового статуса проверяется, существует ли уже такой же
     * (по дате, названию и месту операции). После сохранения новых статусов
     * обновляется флаг is_current.
     * </p>
     *
     * @param parcel посылка
     * @param newStatuses список новых статусов для сохранения
     * @return количество добавленных новых статусов
     */
    @Transactional
    public int saveOnlyNewStatuses(Parcel parcel, List<ParcelStatusHistory> newStatuses) {
        if (newStatuses == null || newStatuses.isEmpty()) {
            return 0;
        }

        // Получаем существующие статусы для этой посылки
        List<ParcelStatusHistory> existingStatuses =
                statusHistoryRepository.findByParcelOrderByOperationDateDesc(parcel);

        int addedCount = 0;

        for (ParcelStatusHistory newStatus : newStatuses) {
            // Проверяем, есть ли уже такой статус
            boolean exists = existingStatuses.stream().anyMatch(existing -> isSameStatus(existing, newStatus));

            if (!exists) {
                newStatus.setParcel(parcel);
                newStatus.setCreatedAt(LocalDateTime.now());
                statusHistoryRepository.save(newStatus);
                addedCount++;
                log.debug(
                        "Добавлен новый статус для посылки {}: {}",
                        parcel.getTrackingNumber(),
                        newStatus.getStatusName());
            } else {
                log.debug(
                        "Статус уже существует для посылки {}: {}",
                        parcel.getTrackingNumber(),
                        newStatus.getStatusName());
            }
        }

        // Обновляем флаг is_current - только последний статус должен быть текущим
        updateCurrentStatusFlag(parcel);

        if (addedCount > 0) {
            log.info("Для посылки {} добавлено {} новых статусов", parcel.getTrackingNumber(), addedCount);
        }

        return addedCount;
    }

    /**
     * Сравнивает два статуса на идентичность.
     * <p>
     * Сравнение происходит по дате операции, названию статуса и месту операции.
     * </p>
     *
     * @param existing существующий статус
     * @param newStatus новый статус для сравнения
     * @return true если статусы идентичны
     */
    private boolean isSameStatus(ParcelStatusHistory existing, ParcelStatusHistory newStatus) {
        return Objects.equals(existing.getOperationDate(), newStatus.getOperationDate())
                && Objects.equals(existing.getStatusName(), newStatus.getStatusName())
                && Objects.equals(existing.getOperationPlace(), newStatus.getOperationPlace())
                && Objects.equals(existing.getStatusCode(), newStatus.getStatusCode());
    }

    /**
     * Обновляет флаг is_current - только последний статус должен быть true.
     * <p>
     * Сначала сбрасывает все флаги для посылки, затем устанавливает
     * is_current = true для самого свежего статуса.
     * </p>
     *
     * @param parcel посылка
     */
    @Transactional
    public void updateCurrentStatusFlag(Parcel parcel) {
        // Сначала сбрасываем все флаги
        statusHistoryRepository.resetCurrentStatus(parcel);

        // Находим последний статус и устанавливаем ему is_current = true
        ParcelStatusHistory lastStatus = statusHistoryRepository
                .findFirstByParcelOrderByOperationDateDesc(parcel)
                .orElse(null);
        if (lastStatus != null) {
            lastStatus.setIsCurrent(true);
            statusHistoryRepository.save(lastStatus);
            log.debug(
                    "Установлен текущий статус для посылки {}: {}",
                    parcel.getTrackingNumber(),
                    lastStatus.getStatusName());
        }
    }

    /**
     * Сохраняет все статусы (полная замена истории).
     * <p>
     * Используется для полного обновления истории, например, при синхронизации.
     * Сначала сбрасывает флаг is_current для всех старых статусов,
     * затем сохраняет новые.
     * </p>
     *
     * @param parcel посылка
     * @param statuses список статусов для сохранения
     */
    @Transactional
    public void saveAllStatuses(Parcel parcel, List<ParcelStatusHistory> statuses) {
        // Сначала сбрасываем флаг is_current для всех старых статусов
        statusHistoryRepository.resetCurrentStatus(parcel);

        // Сохраняем новые статусы
        for (ParcelStatusHistory status : statuses) {
            status.setParcel(parcel);
            statusHistoryRepository.save(status);
        }
    }

    /**
     * Проверяет, существует ли статус с указанным кодом для посылки.
     *
     * @param parcel посылка
     * @param statusCode код статуса
     * @return true если статус существует
     */
    @Transactional(readOnly = true)
    public boolean hasStatus(Parcel parcel, String statusCode) {
        return statusHistoryRepository.existsByParcelAndStatusCode(parcel, statusCode);
    }
}
