package ru.otus.trackingbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность, представляющая связь между пользователем и посылкой.
 * <p>
 * Хранит информацию, специфичную для пары пользователь-посылка:
 * пользовательское имя, активность отслеживания, количество уведомлений,
 * последний известный статус и т.д.
 * </p>
 *
 * <p>Это связующая таблица (many-to-many) с дополнительными атрибутами.</p>
 */
@Entity
@Table(name = "user_parcels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserParcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь, отслеживающий посылку.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Отслеживаемая посылка.
     * Ссылается на tracking_number как на первичный ключ.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_tracking_number", referencedColumnName = "tracking_number", nullable = false)
    private Parcel parcel;

    /**
     * Флаг активности отслеживания.
     * Если false - пользователь остановил отслеживание этой посылки.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Дата и время добавления посылки в отслеживание.
     */
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    /**
     * Дата и время последнего отправленного уведомления.
     */
    @Column(name = "last_notification")
    private LocalDateTime lastNotification;

    /**
     * Общее количество отправленных уведомлений по этой посылке.
     */
    @Column(name = "notification_count")
    private Integer notificationCount = 0;

    /**
     * Пользовательское имя для посылки (можно задать понятное название).
     */
    @Column(name = "custom_name", length = 200)
    private String customName;

    /**
     * Последний известный статус посылки (дублируется для быстрого доступа).
     */
    @Column(name = "last_status", length = 200)
    private String lastStatus;

    /**
     * Описание последнего статуса.
     */
    @Column(name = "last_status_description", length = 1000)
    private String lastStatusDescription;

    /**
     * Дата и время последней проверки статуса.
     */
    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    /**
     * Инициализация полей перед сохранением.
     * Устанавливает дату добавления и значения по умолчанию.
     */
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
        lastChecked = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (notificationCount == null) notificationCount = 0;
    }

    /**
     * Обновляет дату последней проверки.
     */
    public void updateLastChecked() {
        lastChecked = LocalDateTime.now();
    }
}
