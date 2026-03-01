package ru.otus.trackingbot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность, представляющая посылку в системе.
 * <p>
 * Хранит основную информацию о посылке, не зависящую от конкретного пользователя:
 * трек-номер, службу доставки, вес, описание и т.д.
 * </p>
 *
 * <p>Связи:</p>
 * <ul>
 *     <li>OneToMany с UserParcel - связь с пользователями, отслеживающими эту посылку</li>
 *     <li>OneToMany с ParcelStatusHistory - история статусов посылки</li>
 * </ul>
 */
@Entity
@Table(name = "parcels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный трек-номер посылки.
     * Не может быть null, уникален в системе.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String trackingNumber;

    /**
     * Название службы доставки (например, "Почта России").
     */
    @Column(nullable = false, length = 50)
    private String serviceName;

    /**
     * Описание содержимого посылки.
     */
    @Column(length = 500)
    private String description;

    /**
     * Вес посылки в килограммах.
     * Используется BigDecimal для точного хранения десятичных значений.
     */
    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal weight;

    /**
     * Ожидаемая дата доставки.
     */
    private LocalDate estimatedDelivery;

    /**
     * Дата и время создания записи в системе.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления информации.
     */
    private LocalDateTime lastUpdated;

    /**
     * Последняя ошибка, возникшая при попытке получить информацию.
     */
    private String lastError;

    /**
     * Количество попыток получения информации после ошибки.
     */
    private Integer retryCount;

    /**
     * Список связей с пользователями, отслеживающими эту посылку.
     */
    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserParcel> userParcels = new ArrayList<>();

    /**
     * История статусов посылки.
     */
    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParcelStatusHistory> statusHistory = new ArrayList<>();

    /**
     * Инициализация полей перед сохранением в БД.
     * Устанавливает даты создания и последнего обновления.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
    }

    /**
     * Обновляет дату последнего изменения.
     */
    public void updateLastUpdated() {
        lastUpdated = LocalDateTime.now();
    }

    /**
     * Устанавливает вес из Double значения.
     *
     * @param weight вес в килограммах
     */
    public void setWeightFromDouble(Double weight) {
        if (weight != null) {
            this.weight = java.math.BigDecimal.valueOf(weight);
        }
    }

    /**
     * Возвращает вес как Double.
     *
     * @return вес в килограммах или null
     */
    public Double getWeightAsDouble() {
        return weight != null ? weight.doubleValue() : null;
    }
}
