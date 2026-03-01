package ru.otus.trackingbot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;

/**
 * Репозиторий для работы с сущностью {@link UserParcel}.
 * <p>
 * Предоставляет методы для управления связями пользователей с посылками:
 * получение активных посылок пользователя, поиск посылок для обновления,
 * деактивация отслеживания и т.д.
 * </p>
 */
@Repository
public interface UserParcelRepository extends JpaRepository<UserParcel, Long> {

    /**
     * Возвращает все активные посылки пользователя с подгруженными связями.
     * Использует JOIN FETCH для предотвращения N+1 проблемы.
     *
     * @param user пользователь
     * @return список активных посылок пользователя
     */
    @Query("SELECT up FROM UserParcel up " + "JOIN FETCH up.user "
            + "JOIN FETCH up.parcel "
            + "WHERE up.user = :user AND up.isActive = true")
    List<UserParcel> findByUserAndIsActiveTrueWithDetails(@Param("user") User user);

    /**
     * Возвращает все активные посылки, которые требуют обновления статуса.
     * Отбираются посылки, у которых lastChecked раньше указанной даты.
     *
     * @param date дата, ранее которой должна быть последняя проверка
     * @return список посылок для обновления
     */
    @Query("SELECT up FROM UserParcel up " + "JOIN FETCH up.user "
            + "JOIN FETCH up.parcel "
            + "WHERE up.isActive = true AND up.lastChecked < :date")
    List<UserParcel> findUserParcelsToUpdateWithDetails(@Param("date") LocalDateTime date);

    /**
     * Возвращает все активные посылки пользователя (без подгрузки связей).
     *
     * @param user пользователь
     * @return список активных посылок
     */
    List<UserParcel> findByUserAndIsActiveTrue(User user);

    /**
     * Находит связь пользователя с посылкой по трек-номеру.
     *
     * @param user пользователь
     * @param trackingNumber трек-номер посылки
     * @return Optional со связью или пустой Optional
     */
    Optional<UserParcel> findByUserAndParcelTrackingNumber(User user, String trackingNumber);

    /**
     * Деактивирует отслеживание посылки для пользователя.
     * Устанавливает isActive = false.
     *
     * @param user пользователь
     * @param trackingNumber трек-номер посылки
     * @return количество обновленных записей
     */
    @Modifying
    @Transactional
    @Query(
            "UPDATE UserParcel up SET up.isActive = false WHERE up.user = :user AND up.parcel.trackingNumber = :trackingNumber")
    int deactivateUserParcel(@Param("user") User user, @Param("trackingNumber") String trackingNumber);

    /**
     * Находит связь пользователя с посылкой по ID с подгрузкой всех связей.
     *
     * @param id идентификатор связи
     * @param user пользователь
     * @return Optional со связью
     */
    @Query("SELECT up FROM UserParcel up " + "JOIN FETCH up.user "
            + "JOIN FETCH up.parcel "
            + "WHERE up.id = :id AND up.user = :user")
    Optional<UserParcel> findByIdWithDetails(@Param("id") Long id, @Param("user") User user);

    @Query("SELECT up FROM UserParcel up " + "JOIN FETCH up.user "
            + "JOIN FETCH up.parcel "
            + "WHERE up.user = :user "
            + "ORDER BY up.isActive DESC, up.addedAt DESC")
    List<UserParcel> findAllByUserWithDetails(@Param("user") User user);
}
