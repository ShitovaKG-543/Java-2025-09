package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.repository.UserParcelRepository;

/**
 * Сервис для управления связями пользователей с посылками.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *     <li>Добавления посылок в отслеживание</li>
 *     <li>Получения списка посылок пользователя</li>
 *     <li>Обновления статусов отслеживания</li>
 *     <li>Остановки/возобновления отслеживания</li>
 *     <li>Удаления посылок из отслеживания</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
public class UserParcelService {

    @Autowired
    private UserParcelRepository userParcelRepository;

    /**
     * Добавляет посылку для отслеживания пользователю.
     * <p>
     * Если посылка уже была в отслеживании, но деактивирована,
     * реактивирует её и сбрасывает счетчик уведомлений.
     * </p>
     *
     * @param user пользователь
     * @param parcel посылка
     * @param customName пользовательское имя для посылки (может быть null)
     * @return созданная или обновленная связь
     */
    @Transactional
    public UserParcel addParcelForUser(User user, Parcel parcel, String customName) {
        Optional<UserParcel> existing =
                userParcelRepository.findByUserAndParcelTrackingNumber(user, parcel.getTrackingNumber());

        if (existing.isPresent()) {
            UserParcel userParcel = existing.get();
            if (!userParcel.getIsActive()) {
                userParcel.setIsActive(true);
                userParcel.setAddedAt(LocalDateTime.now());
                if (customName != null) userParcel.setCustomName(customName);
                // Сбрасываем счетчик уведомлений при реактивации
                userParcel.setNotificationCount(0);
                userParcel.setLastNotification(null);
                return userParcelRepository.save(userParcel);
            }
            return userParcel;
        }

        UserParcel userParcel = UserParcel.builder()
                .user(user)
                .parcel(parcel)
                .isActive(true)
                .customName(customName)
                .notificationCount(0)
                .build();

        return userParcelRepository.save(userParcel);
    }

    /**
     * Обновляет статус посылки для пользователя.
     *
     * @param userParcel связь пользователя с посылкой
     * @param trackingInfo информация об отслеживании
     */
    @Transactional
    public void updateUserParcelStatus(UserParcel userParcel, TrackingInfo trackingInfo) {
        if (trackingInfo != null) {
            if (trackingInfo.getStatus() != null) {
                userParcel.setLastStatus(trackingInfo.getStatus());
            }
            if (trackingInfo.getStatusDescription() != null) {
                userParcel.setLastStatusDescription(trackingInfo.getStatusDescription());
            }
        }
        userParcel.setLastChecked(LocalDateTime.now());
        userParcelRepository.save(userParcel);
    }

    /**
     * Возвращает все активные посылки пользователя с полной информацией.
     * <p>
     * Использует JOIN FETCH для загрузки связанных сущностей
     * и предотвращения N+1 проблемы.
     * </p>
     *
     * @param user пользователь
     * @return список активных посылок с подгруженными данными
     */
    @Transactional(readOnly = true)
    public List<UserParcel> getActiveUserParcelsWithDetails(User user) {
        return userParcelRepository.findByUserAndIsActiveTrueWithDetails(user);
    }

    /**
     * Возвращает все активные посылки пользователя (без подгрузки связей).
     *
     * @param user пользователь
     * @return список активных посылок
     */
    @Transactional(readOnly = true)
    public List<UserParcel> getActiveUserParcels(User user) {
        return userParcelRepository.findByUserAndIsActiveTrue(user);
    }

    /**
     * Находит связь пользователя с посылкой по трек-номеру.
     *
     * @param user пользователь
     * @param trackingNumber трек-номер
     * @return Optional со связью
     */
    @Transactional(readOnly = true)
    public Optional<UserParcel> findByUserAndTrackingNumber(User user, String trackingNumber) {
        return userParcelRepository.findByUserAndParcelTrackingNumber(user, trackingNumber);
    }

    /**
     * Возвращает список посылок, требующих обновления статуса.
     * <p>
     * Отбираются активные посылки, у которых lastChecked
     * был более 5 минут назад.
     * </p>
     *
     * @return список посылок для обновления
     */
    @Transactional(readOnly = true)
    public List<UserParcel> getParcelsToUpdateWithDetails() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        return userParcelRepository.findUserParcelsToUpdateWithDetails(cutoffTime);
    }

    /**
     * Фиксирует отправку уведомления для посылки.
     * <p>
     * Обновляет дату последнего уведомления и увеличивает счетчик.
     * </p>
     *
     * @param userParcel связь пользователя с посылкой
     * @param message текст отправленного уведомления (не используется, но может пригодиться для логирования)
     */
    @Transactional
    public void sendNotification(UserParcel userParcel, String message) {
        userParcel.setLastNotification(LocalDateTime.now());
        userParcel.setNotificationCount(userParcel.getNotificationCount() + 1);
        userParcelRepository.save(userParcel);
    }

    /**
     * Находит связь пользователя с посылкой по ID с подгрузкой всех связей.
     *
     * @param id идентификатор связи
     * @param user пользователь
     * @return Optional со связью
     */
    @Transactional(readOnly = true)
    public Optional<UserParcel> findByIdWithDetails(Long id, User user) {
        return userParcelRepository.findByIdWithDetails(id, user);
    }

    /**
     * Полностью удаляет посылку из отслеживания пользователя.
     * <p>
     * В отличие от stopTracking, этот метод удаляет запись из базы данных.
     * История статусов посылки при этом сохраняется (сама посылка не удаляется).
     * </p>
     *
     * @param userParcel связь пользователя с посылкой
     */
    @Transactional
    public void deleteUserParcel(UserParcel userParcel) {
        userParcelRepository.delete(userParcel);
    }

    /**
     * Останавливает отслеживание посылки (без удаления).
     * <p>
     * Устанавливает флаг isActive = false, но сохраняет запись в базе.
     * Отслеживание может быть возобновлено позже.
     * </p>
     *
     * @param user пользователь
     * @param trackingNumber трек-номер посылки
     * @return true если отслеживание успешно остановлено
     */
    @Transactional
    public boolean stopTracking(User user, String trackingNumber) {
        int updated = userParcelRepository.deactivateUserParcel(user, trackingNumber);
        if (updated > 0) {
            log.info("Остановлено отслеживание {} для пользователя {}", trackingNumber, user.getChatId());
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<UserParcel> getAllUserParcelsWithDetails(User user) {
        return userParcelRepository.findAllByUserWithDetails(user);
    }
}
