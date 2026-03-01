package ru.otus.trackingbot.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operation {
    private String serviceName;
    private String operationId;
    private String operationName;
    private String operationType;
    private LocalDateTime date;
    private String operationPlace;
    private String operationIndex;
    private String operationAddress;

    // Финансовые данные
    private Double declaredValue;
    private Double cashOnDelivery;
    private Double postage;

    // Данные отправления
    private Integer weight;
    private String mailType;
    private String mailCategory;
    private String mailRank;

    // Информация об отправителе/получателе
    private String recipientName;
    private String recipientAddress;
    private String senderName;
    private String senderAddress;

    // Дополнительные данные для разных служб
    private Map<String, Object> additionalData = new HashMap<>();

    public void addAdditionalData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        additionalData.put(key, value);
    }
}
