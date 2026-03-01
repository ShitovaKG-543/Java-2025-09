package ru.otus.trackingbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Сущность, представляющая запись об изменении статуса посылки.
 * <p>
 * Хранит полную историю всех изменений статуса для каждой посылки.
 * Каждая запись содержит информацию об операции: дату, место, тип операции.
 * </p>
 */
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

    /**
     * Посылка, к которой относится данная запись статуса.
     * Связь Many-to-One с сущностью Parcel.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    /**
     * Код статуса (внутренний идентификатор операции).
     */
    private String statusCode;

    /**
     * Название статуса (человекочитаемое).
     */
    private String statusName;

    /**
     * Подробное описание статуса.
     */
    @Column(length = 1000)
    private String statusDescription;

    /**
     * Место совершения операции (город, отделение).
     */
    private String operationPlace;

    /**
     * Дата и время совершения операции.
     */
    private LocalDateTime operationDate;

    /**
     * Вес посылки на момент операции (в граммах).
     */
    private Integer weight;

    /**
     * Дата и время создания записи в системе.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Флаг, указывающий, является ли этот статус текущим для посылки.
     * Только одна запись для посылки может иметь значение true.
     */
    private Boolean isCurrent = false;

    /**
     * Инициализация полей перед сохранением.
     * Устанавливает дату создания.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isCurrent == null) isCurrent = false;
    }
}
