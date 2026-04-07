package ru.otus.service;

import ru.otus.cache.StatusCache;
import ru.otus.db.DatabaseManager;
import ru.otus.model.Parcel;
import ru.otus.service.postal.PostalService;

import java.sql.*;
import java.util.*;

public class TrackingService {
    private final Map<String, PostalService> services = new HashMap<>();
    private final StatusCache cache;

    public TrackingService(StatusCache cache, List<PostalService> postalServices) {
        this.cache = cache;
        for (PostalService service : postalServices) {
            services.put(service.getServiceName(), service);
        }
    }

    public String getTrackingStatus(Long userId, String trackingNumber) {
        // Определяем сервис по номеру
        PostalService service = detectService(trackingNumber);
        if (service == null) {
            return "❌ Неизвестный почтовый сервис для номера: " + trackingNumber;
        }

        // Проверяем кэш
        StatusCache.CachedStatus cached = cache.get(trackingNumber);
        if (cached != null) {
            return cached.status;
        }

        // Запрашиваем реальный статус
        try {
            String status = service.getTrackingStatus(trackingNumber);
            cache.put(trackingNumber, status);

            // Сохраняем в БД
            saveStatusToDb(userId, trackingNumber, service.getServiceName(), status);

            return status;
        } catch (Exception e) {
            return "⚠️ Ошибка получения статуса: " + e.getMessage();
        }
    }

    private PostalService detectService(String trackingNumber) {
        for (PostalService service : services.values()) {
            if (service.isValidTrackingNumber(trackingNumber)) {
                return service;
            }
        }
        return services.get("RussianPost"); // fallback
    }

    private void saveStatusToDb(Long userId, String trackingNumber, String serviceName, String status) {
        String sql = """
            INSERT INTO parcels (user_id, tracking_number, service_name, last_status, last_status_update)
            VALUES (?, ?, ?, ?, NOW())
            ON CONFLICT (user_id, tracking_number)
            DO UPDATE SET last_status = EXCLUDED.last_status, last_status_update = NOW()
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, trackingNumber);
            stmt.setString(3, serviceName);
            stmt.setString(4, status);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Parcel> getAllUserParcels(Long userId) {
        List<Parcel> parcels = new ArrayList<>();
        String sql = "SELECT * FROM parcels WHERE user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Parcel p = new Parcel();
                p.setId(rs.getLong("id"));
                p.setUserId(rs.getLong("user_id"));
                p.setTrackingNumber(rs.getString("tracking_number"));
                p.setServiceName(rs.getString("service_name"));
                p.setLastStatus(rs.getString("last_status"));
                p.setLastStatusUpdate(rs.getTimestamp("last_status_update").toLocalDateTime());
                parcels.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return parcels;
    }
}