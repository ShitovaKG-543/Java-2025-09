package ru.otus.trackingbot.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.otus.trackingbot.entity.Parcel;

/**
 * Репозиторий для работы с сущностью {@link Parcel}.
 * <p>
 * Предоставляет методы для поиска посылок по трек-номеру
 * и стандартные CRUD операции.
 * </p>
 */
@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

    /**
     * Находит посылку по трек-номеру.
     *
     * @param trackingNumber трек-номер посылки
     * @return Optional с найденной посылкой или пустой Optional
     */
    Optional<Parcel> findByTrackingNumber(String trackingNumber);
}
