package ru.otus.trackingbot.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель, представляющая одну операцию (статус) в истории отслеживания посылки.
 * <p>
 * Содержит детальную информацию об отдельной операции: тип операции,
 * дату, место, вес, финансовые данные и другую информацию.
 * </p>
 *
 * <p>Используется для обмена данными между сервисами отслеживания и бизнес-логикой.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operation {

    /**
     * Название службы доставки, предоставившей эту операцию.
     */
    private String serviceName;

    /**
     * Уникальный идентификатор типа операции (внутренний код).
     */
    private String operationId;

    /**
     * Название операции (человекочитаемое).
     */
    private String operationName;

    /**
     * Тип операции (например, "arrival", "departure").
     */
    private String operationType;

    /**
     * Дата и время совершения операции.
     */
    private LocalDateTime date;

    /**
     * Место совершения операции (город, индекс, адрес отделения).
     */
    private String operationPlace;

    /**
     * Почтовый индекс места операции.
     */
    private String operationIndex;

    /**
     * Полный адрес места операции.
     */
    private String operationAddress;

    // Финансовые данные
    /**
     * Объявленная стоимость посылки.
     */
    private Double declaredValue;

    /**
     * Наложенный платеж.
     */
    private Double cashOnDelivery;

    /**
     * Стоимость пересылки.
     */
    private Double postage;

    // Данные отправления
    /**
     * Вес посылки в граммах.
     */
    private Integer weight;

    /**
     * Тип почтового отправления.
     */
    private String mailType;

    /**
     * Категория отправления.
     */
    private String mailCategory;

    /**
     * Разряд отправления.
     */
    private String mailRank;

    // Информация об отправителе/получателе
    /**
     * Имя получателя.
     */
    private String recipientName;

    /**
     * Адрес получателя.
     */
    private String recipientAddress;

    /**
     * Имя отправителя.
     */
    private String senderName;

    /**
     * Адрес отправителя.
     */
    private String senderAddress;

    /**
     * Дополнительные данные, специфичные для конкретной службы доставки.
     * Используется для расширения функциональности без изменения модели.
     */
    private Map<String, Object> additionalData = new HashMap<>();

    /**
     * Добавляет дополнительные данные.
     *
     * @param key ключ
     * @param value значение
     */
    public void addAdditionalData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        additionalData.put(key, value);
    }
}
