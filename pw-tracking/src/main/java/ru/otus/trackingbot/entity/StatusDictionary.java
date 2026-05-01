package ru.otus.trackingbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Справочник статусов для служб доставки.
 */
@Entity
@Table(name = "status_dictionary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusDictionary {

    @Id
    @Column(name = "status_code", length = 20)
    private String statusCode;

    @Column(name = "status_name", nullable = false, length = 200)
    private String statusName;

    @Column(name = "status_description", columnDefinition = "TEXT")
    private String statusDescription;

    @Column(name = "service_name", length = 50)
    private String serviceName;

    @Column(length = 10)
    private String emoji;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
