package ru.otus.trackingbot.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import ru.otus.trackingbot.model.TrackingInfo;

/**
 * Абстрактный базовый класс для всех сервисов отслеживания посылок.
 * <p>
 * Определяет общий контракт для всех служб доставки:
 * валидация трек-номера, получение информации, маппинг статусов.
 * Конкретные реализации должны наследовать этот класс.
 * </p>
 *
 * <p>Поддерживаемые службы доставки:</p>
 * <ul>
 *     <li>Почта России (RussianPostTrackingService)</li>
 *     <li>Другие службы могут быть добавлены в будущем</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractTrackingService {

    private Pattern trackingPattern;

    /**
     * Возвращает название службы доставки.
     *
     * @return название службы (например, "Почта России")
     */
    public abstract String getServiceName();

    /**
     * Возвращает регулярное выражение для валидации трек-номера.
     *
     * @return паттерн трек-номера
     */
    public abstract String getTrackingNumberPattern();

    /**
     * Отслеживает посылку по трек-номеру.
     *
     * @param trackingNumber трек-номер посылки
     * @return объект TrackingInfo с информацией о посылке
     */
    public abstract TrackingInfo trackParcel(String trackingNumber);

    /**
     * Возвращает человекочитаемое описание статуса по его коду.
     *
     * @param statusCode код статуса
     * @return описание статуса
     */
    public abstract String getStatusDescription(String statusCode);

    /**
     * Проверяет, соответствует ли трек-номер формату службы доставки.
     *
     * @param trackingNumber трек-номер для проверки
     * @return true если формат корректный
     */
    public boolean isValidTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }
        if (trackingPattern == null) {
            trackingPattern = Pattern.compile(getTrackingNumberPattern());
        }
        return trackingPattern.matcher(trackingNumber).matches();
    }

    /**
     * Очищает трек-номер от лишних символов (пробелы, дефисы и т.д.).
     *
     * @param trackingNumber исходный трек-номер
     * @return очищенный трек-номер в верхнем регистре
     */
    public String cleanTrackingNumber(String trackingNumber) {
        if (trackingNumber == null) return null;
        return trackingNumber.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Возвращает маппинг кодов статусов в человекочитаемые описания.
     * Переопределяется в наследниках.
     *
     * @return Map с маппингом статусов
     */
    protected Map<String, String> getStatusMapping() {
        return new HashMap<>();
    }

    /**
     * Создает объект TrackingInfo с информацией об ошибке.
     *
     * @param trackingNumber трек-номер
     * @param error сообщение об ошибке
     * @return объект TrackingInfo с флагом success = false
     */
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

    /**
     * Логирует запрос к API отслеживания.
     *
     * @param trackingNumber трек-номер
     */
    protected void logRequest(String trackingNumber) {
        log.debug("[{}] Запрос информации для трек-номера: {}", getServiceName(), trackingNumber);
    }
}
