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
 */
@Service
@Slf4j
public class UserParcelService {

    @Autowired
    private UserParcelRepository userParcelRepository;

    /**
     * Добавляет посылку для отслеживания пользователю.
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
     */
    @Transactional(readOnly = true)
    public List<UserParcel> getActiveUserParcelsWithDetails(User user) {
        return userParcelRepository.findByUserAndIsActiveTrueWithDetails(user);
    }

    /**
     * Возвращает все посылки пользователя (активные и неактивные) с полной информацией.
     */
    @Transactional(readOnly = true)
    public List<UserParcel> getAllUserParcelsWithDetails(User user) {
        return userParcelRepository.findAllByUserWithDetails(user);
    }

    /**
     * Возвращает все активные посылки пользователя (без подгрузки связей).
     */
    @Transactional(readOnly = true)
    public List<UserParcel> getActiveUserParcels(User user) {
        return userParcelRepository.findByUserAndIsActiveTrue(user);
    }

    /**
     * Находит связь пользователя с посылкой по трек-номеру.
     */
    @Transactional(readOnly = true)
    public Optional<UserParcel> findByUserAndTrackingNumber(User user, String trackingNumber) {
        return userParcelRepository.findByUserAndParcelTrackingNumber(user, trackingNumber);
    }

    /**
     * Возвращает список посылок, требующих обновления статуса.
     */
    @Transactional(readOnly = true)
    public List<UserParcel> getParcelsToUpdateWithDetails() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        return userParcelRepository.findUserParcelsToUpdateWithDetails(cutoffTime);
    }

    /**
     * Фиксирует отправку уведомления для посылки.
     */
    @Transactional
    public void sendNotification(UserParcel userParcel, String message) {
        userParcel.setLastNotification(LocalDateTime.now());
        userParcel.setNotificationCount(userParcel.getNotificationCount() + 1);
        userParcelRepository.save(userParcel);
    }

    /**
     * Находит связь пользователя с посылкой по ID с подгрузкой всех связей.
     */
    @Transactional(readOnly = true)
    public Optional<UserParcel> findByIdWithDetails(Long id, User user) {
        return userParcelRepository.findByIdWithDetails(id, user);
    }

    /**
     * Полностью удаляет посылку из отслеживания пользователя.
     */
    @Transactional
    public void deleteUserParcel(UserParcel userParcel) {
        userParcelRepository.delete(userParcel);
    }

    /**
     * Останавливает отслеживание посылки (без удаления).
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
}
