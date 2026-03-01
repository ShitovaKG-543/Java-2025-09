package ru.otus.trackingbot.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.otus.trackingbot.model.Operation;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.AbstractTrackingService;

@Service
@Slf4j
public class CdekTrackingService extends AbstractTrackingService {

    @Value("${cdek.api.url}")
    private String apiUrl;

    @Value("${cdek.api.client-id}")
    private String clientId;

    @Value("${cdek.api.client-secret}")
    private String clientSecret;

    private static final String TRACKING_PATTERN = "^\\d{10,20}$";

    private final RestTemplate restTemplate;
    private String accessToken;
    private LocalDateTime tokenExpiry;

    public CdekTrackingService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String getServiceName() {
        return "СДЭК";
    }

    @Override
    public String getTrackingNumberPattern() {
        return TRACKING_PATTERN;
    }

    @Override
    public String getStatusDescription(String statusCode) {
        Map<String, String> statusMap = new HashMap<>() {
            {
                put("ACCEPTED", "Принято на склад");
                put("IN_TRANSIT", "В пути");
                put("DELIVERED", "Доставлено");
                put("RETURNED", "Возврат");
                put("AWAITING", "Ожидает вручения");
                put("CUSTOMS", "На таможне");
            }
        };
        return statusMap.getOrDefault(statusCode, "Неизвестный статус");
    }

    @Override
    public TrackingInfo trackParcel(String trackingNumber) {
        String cleanNumber = cleanTrackingNumber(trackingNumber);
        logRequest(cleanNumber);

        if (!isValidTrackingNumber(cleanNumber)) {
            return createErrorResponse(cleanNumber, "Неверный формат трек-номера для СДЭК");
        }

        try {
            // Получаем токен авторизации
            ensureAccessToken();

            // Делаем запрос к API СДЭК
            String url = apiUrl + "/orders?im_number=" + cleanNumber;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TrackingInfo info = parseResponse(response.getBody(), cleanNumber);
                logResponse(cleanNumber, info);
                return info;
            } else {
                return createErrorResponse(cleanNumber, "Ошибка при получении данных от СДЭК");
            }

        } catch (Exception e) {
            log.error("Ошибка при отслеживании посылки {} через СДЭК", cleanNumber, e);
            return createErrorResponse(cleanNumber, "Ошибка соединения с сервером СДЭК: " + e.getMessage());
        }
    }

    private void ensureAccessToken() {
        if (accessToken == null || tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry)) {
            refreshAccessToken();
        }
    }

    private void refreshAccessToken() {
        try {
            String url = apiUrl + "/oauth/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "client_credentials");
            requestBody.put("client_id", clientId);
            requestBody.put("client_secret", clientSecret);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                accessToken = (String) response.getBody().get("access_token");
                int expiresIn = (int) response.getBody().get("expires_in");
                tokenExpiry = LocalDateTime.now().plusSeconds(expiresIn - 300); // минус 5 минут запаса
            }
        } catch (Exception e) {
            log.error("Ошибка при получении токена СДЭК", e);
        }
    }

    private TrackingInfo parseResponse(Map<String, Object> response, String trackingNumber) {
        TrackingInfo.TrackingInfoBuilder builder = TrackingInfo.builder()
                .trackingNumber(trackingNumber)
                .serviceName(getServiceName())
                .success(true)
                .lastCheck(LocalDateTime.now());

        List<Map<String, Object>> orders = (List<Map<String, Object>>) response.get("orders");
        if (orders != null && !orders.isEmpty()) {
            Map<String, Object> order = orders.get(0);

            String status = (String) order.get("status");
            builder.status(status).statusDescription(getStatusDescription(status));

            // Парсим историю статусов
            List<Map<String, Object>> statuses = (List<Map<String, Object>>) order.get("statuses");
            if (statuses != null) {
                List<Operation> operations = new ArrayList<>();

                for (Map<String, Object> statusData : statuses) {
                    Operation op = parseStatus(statusData);
                    if (op != null) {
                        operations.add(op);
                    }
                }

                if (!operations.isEmpty()) {
                    operations.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                    builder.lastOperation(operations.get(0)).allOperations(operations);

                    // Проверяем доставку
                    builder.delivered("DELIVERED".equals(operations.get(0).getOperationId()));
                }
            }

            // Добавляем дополнительную информацию
            if (order.containsKey("weight")) {
                builder.weight(((Number) order.get("weight")).doubleValue());
            }
            if (order.containsKey("recipient")) {
                Map<String, String> recipient = (Map<String, String>) order.get("recipient");
                builder.destination(recipient.get("city") + ", " + recipient.get("address"));
            }
        }

        return builder.build();
    }

    private Operation parseStatus(Map<String, Object> statusData) {
        try {
            return Operation.builder()
                    .serviceName(getServiceName())
                    .operationId((String) statusData.get("code"))
                    .operationName((String) statusData.get("name"))
                    .date(parseDate((String) statusData.get("date")))
                    .operationPlace((String) statusData.get("city"))
                    .build();
        } catch (Exception e) {
            log.error("Ошибка парсинга статуса СДЭК", e);
            return null;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Ошибка парсинга даты СДЭК: {}", dateStr);
            return LocalDateTime.now();
        }
    }
}
