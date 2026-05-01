package ru.otus.trackingbot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Сущность, представляющая пользователя бота.
 * <p>
 * Хранит информацию о пользователе Telegram: идентификатор чата,
 * имя, настройки уведомлений, дату регистрации и т.д.
 * </p>
 *
 * <p>Связи:</p>
 * <ul>
 *     <li>OneToMany с UserParcel - список отслеживаемых посылок пользователя</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный идентификатор чата в Telegram.
     * Используется для отправки сообщений конкретному пользователю.
     */
    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    /**
     * Username пользователя в Telegram.
     */
    @Column(length = 100)
    private String username;

    /**
     * Имя пользователя.
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * Фамилия пользователя.
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * Код языка пользователя.
     */
    @Column(name = "language_code", length = 10)
    private String languageCode;

    /**
     * Статус активности пользователя (не забанен/активен).
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Дата и время регистрации пользователя.
     */
    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    /**
     * Дата и время последней активности пользователя.
     */
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    /**
     * Флаг включения уведомлений для пользователя.
     * Если false - пользователь не получает уведомлений об изменениях статусов.
     */
    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    /**
     * Список связей пользователя с посылками, которые он отслеживает.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserParcel> userParcels = new ArrayList<>();

    /**
     * Инициализация полей перед сохранением.
     * Устанавливает дату регистрации и значения по умолчанию.
     */
    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (notificationEnabled == null) notificationEnabled = true;
    }
}
