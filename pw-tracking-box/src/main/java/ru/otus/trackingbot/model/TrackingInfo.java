package ru.otus.trackingbot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TrackingInfo {
    private String trackNumber;
    private String carrier;
    private String status;
    private String description;
    private LocalDateTime lastUpdate;
    private String fromLocation;
    private String toLocation;
    private List<TrackingHistory> history;
    private boolean success;
    private String errorMessage;

    public TrackingInfo() {
        this.history = new ArrayList<>();
        this.success = true;
    }

    // Вложенный класс для истории отслеживания
    public static class TrackingHistory {
        private LocalDateTime dateTime;
        private String status;
        private String location;
        private String description;

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public void setDateTime(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            return String.format("[%s] %s - %s (%s)", dateTime.format(formatter), status, location, description);
        }
    }

    // Геттеры и сеттеры
    public String getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(String trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public List<TrackingHistory> getHistory() {
        return history;
    }

    public void setHistory(List<TrackingHistory> history) {
        this.history = history;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addHistory(LocalDateTime dateTime, String status, String location, String description) {
        TrackingHistory history = new TrackingHistory();
        history.setDateTime(dateTime);
        history.setStatus(status);
        history.setLocation(location);
        history.setDescription(description);
        this.history.add(history);
    }

    public String getFormattedLastUpdate() {
        if (lastUpdate == null) return "Неизвестно";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return lastUpdate.format(formatter);
    }
}
