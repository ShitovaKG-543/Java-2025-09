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
     */
    @Transactional
    public void saveStatusHistory(Parcel parcel, ParcelStatusHistory statusHistory) {
        statusHistory.setParcel(parcel);
        statusHistoryRepository.save(statusHistory);
    }

    /**
     * Сохраняет только новые статусы, которых еще нет в базе.
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

        List<ParcelStatusHistory> existingStatuses =
                statusHistoryRepository.findByParcelOrderByOperationDateDesc(parcel);

        int addedCount = 0;

        for (ParcelStatusHistory newStatus : newStatuses) {
            boolean exists = existingStatuses.stream().anyMatch(existing -> isSameStatus(existing, newStatus));

            if (!exists) {
                // ЯВНО устанавливаем ВСЕ поля!
                newStatus.setParcel(parcel);
                newStatus.setIsCurrent(false);
                newStatus.setCreatedAt(LocalDateTime.now()); // ← Явно!

                statusHistoryRepository.save(newStatus);
                addedCount++;
            }
        }

        updateCurrentStatusFlag(parcel);
        return addedCount;
    }

    /**
     * Обновляет флаг is_current - только последний статус должен быть true.
     */
    @Transactional
    public void updateCurrentStatusFlag(Parcel parcel) {
        statusHistoryRepository.resetCurrentStatus(parcel);

        ParcelStatusHistory lastStatus = statusHistoryRepository
                .findFirstByParcelOrderByOperationDateDesc(parcel)
                .orElse(null);

        if (lastStatus != null) {
            lastStatus.setIsCurrent(true); // ← Явно!
            // created_at НЕ обновляем, это поле не должно меняться
            statusHistoryRepository.save(lastStatus);
        }
    }

    /**
     * Сравнивает два статуса на идентичность.
     */
    private boolean isSameStatus(ParcelStatusHistory existing, ParcelStatusHistory newStatus) {
        return Objects.equals(existing.getOperationDate(), newStatus.getOperationDate())
                && Objects.equals(existing.getStatusName(), newStatus.getStatusName())
                && Objects.equals(existing.getOperationPlace(), newStatus.getOperationPlace())
                && Objects.equals(existing.getStatusCode(), newStatus.getStatusCode());
    }

    /**
     * Проверяет, существует ли статус с указанным кодом для посылки.
     */
    @Transactional(readOnly = true)
    public boolean hasStatus(Parcel parcel, String statusCode) {
        return statusHistoryRepository.existsByParcelAndStatusCode(parcel, statusCode);
    }
}
