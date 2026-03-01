package ru.otus.trackingbot.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.ParcelStatusHistory;

/**
 * Репозиторий для работы с сущностью {@link ParcelStatusHistory}.
 * <p>
 * Предоставляет методы для работы с историей статусов посылок:
 * получение истории, поиск последнего статуса, обновление флага текущего статуса.
 * </p>
 */
@Repository
public interface ParcelStatusHistoryRepository extends JpaRepository<ParcelStatusHistory, Long> {

    /**
     * Возвращает историю статусов для указанной посылки,
     * отсортированную по дате операции от новых к старым.
     *
     * @param parcel посылка
     * @return список статусов в порядке убывания даты
     */
    List<ParcelStatusHistory> findByParcelOrderByOperationDateDesc(Parcel parcel);

    /**
     * Возвращает последний (самый новый) статус для указанной посылки.
     *
     * @param parcel посылка
     * @return Optional с последним статусом
     */
    Optional<ParcelStatusHistory> findFirstByParcelOrderByOperationDateDesc(Parcel parcel);

    /**
     * Сбрасывает флаг isCurrent для всех статусов указанной посылки.
     * Используется перед установкой нового текущего статуса.
     *
     * @param parcel посылка
     */
    @Modifying
    @Transactional
    @Query("UPDATE ParcelStatusHistory psh SET psh.isCurrent = false WHERE psh.parcel = :parcel")
    void resetCurrentStatus(@Param("parcel") Parcel parcel);

    /**
     * Проверяет, существует ли статус с указанным кодом для посылки.
     *
     * @param parcel посылка
     * @param statusCode код статуса
     * @return true если статус существует
     */
    boolean existsByParcelAndStatusCode(Parcel parcel, String statusCode);
}
