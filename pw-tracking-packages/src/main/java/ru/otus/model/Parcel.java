package ru.otus.model;

import java.time.LocalDateTime;

@Getter
@Setter
public class Parcel {
    private Long id;
    private Long userId;
    private String trackingNumber;
    private String serviceName; // "RussianPost", "DHL", etc.
    private String lastStatus;
    private LocalDateTime lastStatusUpdate;
    private LocalDateTime createdAt;
}
