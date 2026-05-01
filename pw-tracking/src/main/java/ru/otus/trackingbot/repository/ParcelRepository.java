package ru.otus.trackingbot.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.otus.trackingbot.entity.Parcel;

/**
 * Репозиторий для работы с сущностью {@link Parcel}.
 * <p>
 * Использует tracking_number в качестве первичного ключа (String).
 * Предоставляет стандартные CRUD операции через JpaRepository.
 * </p>
 */
@Repository
public interface ParcelRepository extends JpaRepository<Parcel, String> {

    /**
     * Находит посылку по трек-номеру.
     * Теперь это просто обертка над findById для удобства.
     *
     * @param trackingNumber трек-номер посылки
     * @return Optional с найденной посылкой или пустой Optional
     */
    default Optional<Parcel> findByTrackingNumber(String trackingNumber) {
        return findById(trackingNumber);
    }
}
