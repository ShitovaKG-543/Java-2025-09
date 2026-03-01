package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import ru.otus.trackingbot.model.TrackingInfo;

@Slf4j
public abstract class AbstractTrackingService {

    private final Pattern trackingPattern;

    public AbstractTrackingService() {
        this.trackingPattern = Pattern.compile(getTrackingNumberPattern());
    }

    // Название службы доставки
    public abstract String getServiceName();

    // Поддерживаемый формат трек-номера (регулярное выражение)
    public abstract String getTrackingNumberPattern();

    // Метод для отслеживания посылки
    public abstract TrackingInfo trackParcel(String trackingNumber);

    // Получение статуса в понятном виде
    public abstract String getStatusDescription(String statusCode);

    // Проверка формата трек-номера
    public boolean isValidTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }
        return trackingPattern.matcher(trackingNumber).matches();
    }

    // Очистка трек-номера от лишних символов
    public String cleanTrackingNumber(String trackingNumber) {
        if (trackingNumber == null) return null;
        return trackingNumber.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    // Маппинг статусов (переопределяется в наследниках)
    protected Map<String, String> getStatusMapping() {
        return new HashMap<>();
    }

    // Форматирование даты
    protected String formatDate(LocalDateTime date) {
        if (date == null) {
            return "не указана";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return date.format(formatter);
    }

    // Создание ответа с ошибкой
    protected TrackingInfo createErrorResponse(String trackingNumber, String error) {
        return TrackingInfo.builder()
                .trackingNumber(trackingNumber)
                .serviceName(getServiceName())
                .success(false)
                .error(error)
                .status("Ошибка")
                .lastCheck(LocalDateTime.now())
                .build();
    }

    // Создание успешного ответа
    protected TrackingInfo createSuccessResponse(String trackingNumber) {
        return TrackingInfo.builder()
                .trackingNumber(trackingNumber)
                .serviceName(getServiceName())
                .success(true)
                .lastCheck(LocalDateTime.now())
                .build();
    }

    // Логирование запросов
    protected void logRequest(String trackingNumber) {
        log.debug("[{}] Запрос информации для трек-номера: {}", getServiceName(), trackingNumber);
    }

    // Логирование ответов
    protected void logResponse(String trackingNumber, TrackingInfo info) {
        if (info.isSuccess()) {
            log.info(
                    "[{}] Успешно получена информация для {}: статус '{}'",
                    getServiceName(),
                    trackingNumber,
                    info.getStatus());
        } else {
            log.warn("[{}] Ошибка получения информации для {}: {}", getServiceName(), trackingNumber, info.getError());
        }
    }

    // Проверка доставлена ли посылка
    public boolean isDelivered(TrackingInfo info) {
        return info != null && info.isDelivered();
    }

    // Получение цвета статуса (для отображения)
    public String getStatusColor(String status) {
        if (status == null) return "⚪";

        String lowerStatus = status.toLowerCase();
        if (lowerStatus.contains("доставлен") || lowerStatus.contains("вручен")) {
            return "🟢"; // Зеленый - доставлено
        } else if (lowerStatus.contains("пути") || lowerStatus.contains("транзит")) {
            return "🟡"; // Желтый - в пути
        } else if (lowerStatus.contains("принят") || lowerStatus.contains("сортировк")) {
            return "🔵"; // Синий - обработка
        } else if (lowerStatus.contains("возврат") || lowerStatus.contains("ошибк")) {
            return "🔴"; // Красный - проблема
        }
        return "⚪"; // Серый - неизвестно
    }
}
