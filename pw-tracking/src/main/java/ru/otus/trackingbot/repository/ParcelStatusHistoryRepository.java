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
    default List<ParcelStatusHistory> findByParcelOrderByOperationDateDesc(Parcel parcel) {
        return findByParcelTrackingNumberOrderByOperationDateDesc(parcel.getTrackingNumber());
    }

    /**
     * Возвращает историю статусов по трек-номеру.
     *
     * @param trackingNumber трек-номер посылки
     * @return список статусов в порядке убывания даты
     */
    @Query("SELECT psh FROM ParcelStatusHistory psh " + "JOIN FETCH psh.parcel "
            + "WHERE psh.parcel.trackingNumber = :trackingNumber "
            + "ORDER BY psh.operationDate DESC")
    List<ParcelStatusHistory> findByParcelTrackingNumberOrderByOperationDateDesc(
            @Param("trackingNumber") String trackingNumber);

    /**
     * Возвращает последний (самый новый) статус по трек-номеру.
     * Spring Data JPA автоматически добавит LIMIT 1.
     *
     * @param trackingNumber трек-номер посылки
     * @return Optional с последним статусом
     */
    Optional<ParcelStatusHistory> findFirstByParcelTrackingNumberOrderByOperationDateDesc(String trackingNumber);

    /**
     * Возвращает последний (самый новый) статус для указанной посылки.
     *
     * @param parcel посылка
     * @return Optional с последним статусом
     */
    default Optional<ParcelStatusHistory> findFirstByParcelOrderByOperationDateDesc(Parcel parcel) {
        return findFirstByParcelTrackingNumberOrderByOperationDateDesc(parcel.getTrackingNumber());
    }

    /**
     * Сбрасывает флаг isCurrent для всех статусов указанной посылки.
     * Используется перед установкой нового текущего статуса.
     *
     * @param parcel посылка
     */
    default void resetCurrentStatus(Parcel parcel) {
        resetCurrentStatusByTrackingNumber(parcel.getTrackingNumber());
    }

    /**
     * Сбрасывает флаг isCurrent для всех статусов по трек-номеру.
     *
     * @param trackingNumber трек-номер посылки
     */
    @Modifying
    @Transactional
    @Query("UPDATE ParcelStatusHistory psh SET psh.isCurrent = false WHERE psh.parcel.trackingNumber = :trackingNumber")
    void resetCurrentStatusByTrackingNumber(@Param("trackingNumber") String trackingNumber);

    /**
     * Проверяет, существует ли статус с указанным кодом для посылки.
     *
     * @param parcel посылка
     * @param statusCode код статуса
     * @return true если статус существует
     */
    default boolean existsByParcelAndStatusCode(Parcel parcel, String statusCode) {
        return existsByParcelTrackingNumberAndStatusCode(parcel.getTrackingNumber(), statusCode);
    }

    /**
     * Проверяет, существует ли статус по трек-номеру и коду.
     *
     * @param trackingNumber трек-номер посылки
     * @param statusCode код статуса
     * @return true если статус существует
     */
    @Query("SELECT CASE WHEN COUNT(psh) > 0 THEN true ELSE false END FROM ParcelStatusHistory psh "
            + "WHERE psh.parcel.trackingNumber = :trackingNumber AND psh.statusCode = :statusCode")
    boolean existsByParcelTrackingNumberAndStatusCode(
            @Param("trackingNumber") String trackingNumber, @Param("statusCode") String statusCode);
}
