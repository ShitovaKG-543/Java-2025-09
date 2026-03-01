package ru.otus.trackingbot.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingInfo {
    private String trackingNumber;
    private String serviceName;
    private boolean success;
    private String error;
    private String status;
    private String statusDescription;
    private boolean delivered;
    private LocalDateTime lastCheck;
    private Operation lastOperation;
    private List<Operation> allOperations;
    private String destination;
    private String sender;
    private Double weight;
    private String estimatedDelivery;
}
