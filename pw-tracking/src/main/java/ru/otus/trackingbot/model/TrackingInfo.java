package ru.otus.trackingbot.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Основная модель информации об отслеживании посылки.
 * <p>
 * Агрегирует полную информацию о посылке: текущий статус,
 * историю операций, вес, статус доставки и т.д.
 * Используется для обмена данными между сервисами и UI.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingInfo {

    /**
     * Трек-номер посылки.
     */
    private String trackingNumber;

    /**
     * Название службы доставки.
     */
    private String serviceName;

    /**
     * Флаг успешности получения информации.
     */
    private boolean success;

    /**
     * Сообщение об ошибке (если success == false).
     */
    private String error;

    /**
     * Текущий статус посылки.
     */
    private String status;

    /**
     * Подробное описание текущего статуса.
     */
    private String statusDescription;

    /**
     * Флаг, указывающий, что посылка доставлена.
     */
    private boolean delivered;

    /**
     * Дата и время последней проверки статуса.
     */
    private LocalDateTime lastCheck;

    /**
     * Последняя операция (статус) в истории.
     */
    private Operation lastOperation;

    /**
     * Полная история всех операций с посылкой.
     */
    private List<Operation> allOperations;

    /**
     * Пункт назначения посылки.
     */
    private String destination;

    /**
     * Отправитель посылки.
     */
    private String sender;

    /**
     * Вес посылки в килограммах.
     */
    private Double weight;

    /**
     * Ожидаемая дата доставки.
     */
    private String estimatedDelivery;
}
