package ru.otus.trackingbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parcel_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class ParcelStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_tracking_number", referencedColumnName = "tracking_number", nullable = false)
    private Parcel parcel;

    private String statusCode;

    private String statusName;

    @Column(length = 1000)
    private String statusDescription;

    private String operationPlace;

    private LocalDateTime operationDate;

    private Integer weight;

    // Без DEFAULT! Приложение должно явно установить
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Без DEFAULT! Приложение должно явно установить
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    @PrePersist
    protected void onCreate() {
        // Приложение должно установить ВСЕ поля!
        // Ничего не устанавливаем автоматически
    }
}
