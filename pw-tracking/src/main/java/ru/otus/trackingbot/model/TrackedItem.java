package ru.otus.trackingbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tracked_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String trackingNumber;

    @Column(nullable = false)
    private Long chatId;

    private String serviceName;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastChecked;

    private String lastStatus;

    @Column(length = 500)
    private String lastStatusDescription;

    private boolean active = true;

    private LocalDateTime lastNotification;

    private int notificationCount = 0;

    // Добавляем вычисляемое поле для проверки доставки (опционально)
    public boolean isDelivered() {
        if (lastStatus == null) return false;
        String lowerStatus = lastStatus.toLowerCase();
        return lowerStatus.contains("доставлен") || lowerStatus.contains("вручен") || lowerStatus.contains("получен");
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastChecked = LocalDateTime.now();
        notificationCount = 0;
    }
}
