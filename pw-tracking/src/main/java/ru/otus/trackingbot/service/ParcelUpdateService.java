package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.otus.trackingbot.entity.ParcelStatusHistory;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.TrackingInfo;

/**
 * Сервис для обновления статусов посылок.
 * <p>
 * Предоставляет единый метод для обновления статуса посылки
 * с проверкой изменений и возвратом подробного результата.
 * Используется как при ручном обновлении пользователем,
 * так и при автоматическом обновлении по расписанию.
 * </p>
 */
@Service
@Slf4j
public class ParcelUpdateService {

    @Autowired
    private ParcelService parcelService;

    @Autowired
    private UserParcelService userParcelService;

    @Autowired
    private TrackingCacheService trackingCacheService;

    /**
     * Обновляет статус посылки и возвращает информацию об изменениях.
     * <p>
     * Выполняет следующие шаги:
     * <ol>
     *     <li>Получает актуальную информацию о посылке (с учетом кеша)</li>
     *     <li>Обновляет историю статусов в БД</li>
     *     <li>Обновляет поля lastStatus в UserParcel</li>
     *     <li>Сравнивает старый и новый статусы</li>
     *     <li>Возвращает результат с деталями изменений</li>
     * </ol>
     * </p>
     *
     * @param userParcel связь пользователя с посылкой
     * @param forceFresh если true - игнорирует кеш и запрашивает свежие данные
     * @return результат обновления с информацией об изменениях
     */
    @Transactional
    public ParcelUpdateResult updateParcelStatus(UserParcel userParcel, boolean forceFresh) {
        String trackingNumber = userParcel.getParcel().getTrackingNumber();
        String oldStatus = userParcel.getLastStatus();

        // Получаем информацию о посылке
        TrackingInfo info;
        if (forceFresh) {
            info = trackingCacheService.getFreshTrackingInfo(trackingNumber);
        } else {
            info = trackingCacheService.getTrackingInfoWithFreshnessCheck(trackingNumber, 3600);
        }

        if (!info.isSuccess()) {
            return ParcelUpdateResult.error("Ошибка получения информации: " + info.getError());
        }

        // Обновляем статусы в БД
        boolean hasNewStatuses = parcelService.updateParcelStatus(userParcel.getParcel(), info);

        // Получаем обновленный статус
        ParcelStatusHistory lastStatus = parcelService.getLastParcelStatus(userParcel.getParcel());
        String newStatus = lastStatus != null ? lastStatus.getStatusName() : info.getStatus();

        // Обновляем user_parcel
        userParcel.setLastStatus(newStatus);
        userParcel.setLastStatusDescription(info.getStatusDescription());
        userParcel.setLastChecked(LocalDateTime.now());
        userParcelService.updateUserParcelStatus(userParcel, info);

        // Проверяем, изменился ли статус
        boolean statusChanged =
                (oldStatus == null && newStatus != null) || (oldStatus != null && !oldStatus.equals(newStatus));

        return ParcelUpdateResult.builder()
                .success(true)
                .info(info)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .statusChanged(statusChanged)
                .hasNewStatuses(hasNewStatuses)
                .build();
    }

    /**
     * Результат обновления статуса посылки.
     * <p>
     * Содержит подробную информацию о том, что изменилось:
     * был ли успех, изменился ли статус, добавлены ли новые записи в историю.
     * </p>
     */
    @lombok.Data
    @lombok.Builder
    public static class ParcelUpdateResult {
        /** Флаг успешности операции */
        private boolean success;

        /** Сообщение об ошибке (если success == false) */
        private String error;

        /** Информация об отслеживании */
        private TrackingInfo info;

        /** Старый статус (до обновления) */
        private String oldStatus;

        /** Новый статус (после обновления) */
        private String newStatus;

        /** Флаг, изменился ли статус (oldStatus != newStatus) */
        private boolean statusChanged;

        /** Флаг, были ли добавлены новые записи в историю */
        private boolean hasNewStatuses;

        /**
         * Создает результат с ошибкой.
         *
         * @param error сообщение об ошибке
         * @return результат с success = false
         */
        public static ParcelUpdateResult error(String error) {
            return ParcelUpdateResult.builder().success(false).error(error).build();
        }
    }
}
