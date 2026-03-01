package ru.otus.trackingbot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import jakarta.xml.soap.SOAPMessage;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.otus.trackingbot.model.Operation;
import ru.otus.trackingbot.model.TrackingInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("RussianPostTrackingService тесты")
class RussianPostTrackingServiceTest {

    @InjectMocks
    private RussianPostTrackingService trackingService;

    @BeforeEach
    void setUp() {
        // Устанавливаем тестовые учетные данные
        ReflectionTestUtils.setField(trackingService, "login", "test_login");
        ReflectionTestUtils.setField(trackingService, "password", "test_password");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getServiceName
    // =====================================================

    @Test
    @DisplayName("getServiceName - должно возвращать 'Почта России'")
    void getServiceName_ShouldReturnRussianPost() {
        String serviceName = trackingService.getServiceName();

        assertThat(serviceName).isEqualTo("Почта России");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getTrackingNumberPattern
    // =====================================================

    @Test
    @DisplayName("getTrackingNumberPattern - должно возвращать корректный паттерн")
    void getTrackingNumberPattern_ShouldReturnValidPattern() {
        String pattern = trackingService.getTrackingNumberPattern();

        assertThat(pattern).isEqualTo("^([A-Z]{2}\\d{9}[A-Z]{2}|\\d{14})$");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ isValidTrackingNumber
    // =====================================================

    @Test
    @DisplayName("isValidTrackingNumber - международный трек-номер должен быть валидным")
    void isValidTrackingNumber_InternationalTrackingNumber_ShouldBeValid() {

        String trackingNumber = "RA644000001RU";

        boolean isValid = trackingService.isValidTrackingNumber(trackingNumber);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isValidTrackingNumber - внутренний трек-номер (14 цифр) должен быть валидным")
    void isValidTrackingNumber_DomesticTrackingNumber_ShouldBeValid() {

        String trackingNumber = "12345678901234";

        boolean isValid = trackingService.isValidTrackingNumber(trackingNumber);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isValidTrackingNumber - неверный формат должен быть невалидным")
    void isValidTrackingNumber_InvalidFormat_ShouldBeInvalid() {

        String trackingNumber = "INVALID123";

        boolean isValid = trackingService.isValidTrackingNumber(trackingNumber);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isValidTrackingNumber - пустой трек-номер должен быть невалидным")
    void isValidTrackingNumber_EmptyTrackingNumber_ShouldBeInvalid() {

        String trackingNumber = "";

        boolean isValid = trackingService.isValidTrackingNumber(trackingNumber);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isValidTrackingNumber - null трек-номер должен быть невалидным")
    void isValidTrackingNumber_NullTrackingNumber_ShouldBeInvalid() {
        boolean isValid = trackingService.isValidTrackingNumber(null);

        assertThat(isValid).isFalse();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ cleanTrackingNumber
    // =====================================================

    @Test
    @DisplayName("cleanTrackingNumber - должен очистить трек-номер от лишних символов")
    void cleanTrackingNumber_ShouldRemoveSpecialCharacters() {

        String trackingNumber = " RA-6440-0000-1RU ";

        String cleaned = trackingService.cleanTrackingNumber(trackingNumber);

        assertThat(cleaned).isEqualTo("RA644000001RU");
    }

    @Test
    @DisplayName("cleanTrackingNumber - null должен вернуть null")
    void cleanTrackingNumber_NullInput_ShouldReturnNull() {
        String cleaned = trackingService.cleanTrackingNumber(null);

        assertThat(cleaned).isNull();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getStatusDescription
    // =====================================================

    @Test
    @DisplayName("getStatusDescription - существующий код должен вернуть описание")
    void getStatusDescription_ExistingCode_ShouldReturnDescription() {
        String description = trackingService.getStatusDescription("5");

        assertThat(description).isEqualTo("Вручение адресату");
    }

    @Test
    @DisplayName("getStatusDescription - несуществующий код должен вернуть 'Неизвестный статус'")
    void getStatusDescription_NonExistingCode_ShouldReturnUnknown() {
        String description = trackingService.getStatusDescription("999");

        assertThat(description).isEqualTo("Неизвестный статус");
    }

    @Test
    @DisplayName("getStatusDescription - null код должен вернуть 'Неизвестный статус'")
    void getStatusDescription_NullCode_ShouldReturnUnknown() {
        String description = trackingService.getStatusDescription(null);

        assertThat(description).isEqualTo("Неизвестный статус");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ trackParcel
    // =====================================================

    @Test
    @DisplayName("trackParcel - неверный формат трек-номера должен вернуть ошибку")
    void trackParcel_InvalidFormat_ShouldReturnError() {

        String invalidNumber = "INVALID123";

        TrackingInfo result = trackingService.trackParcel(invalidNumber);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Неверный формат трек-номера");
        assertThat(result.getStatus()).isEqualTo("Ошибка");
        assertThat(result.getServiceName()).isEqualTo("Почта России");
    }

    @Test
    @DisplayName("trackParcel - пустые учетные данные должны вернуть ошибку")
    void trackParcel_EmptyCredentials_ShouldReturnError() {

        ReflectionTestUtils.setField(trackingService, "login", "");
        ReflectionTestUtils.setField(trackingService, "password", "");
        String trackingNumber = "RA644000001RU";

        TrackingInfo result = trackingService.trackParcel(trackingNumber);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Ошибка конфигурации");
        assertThat(result.getStatus()).isEqualTo("Ошибка");
    }

    @Test
    @DisplayName("trackParcel - null учетные данные должны вернуть ошибку")
    void trackParcel_NullCredentials_ShouldReturnError() {

        ReflectionTestUtils.setField(trackingService, "login", null);
        ReflectionTestUtils.setField(trackingService, "password", null);
        String trackingNumber = "RA644000001RU";

        TrackingInfo result = trackingService.trackParcel(trackingNumber);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Ошибка конфигурации");
        assertThat(result.getStatus()).isEqualTo("Ошибка");
    }

    @Test
    @DisplayName("trackParcel - валидный трек-номер с учетными данными")
    void trackParcel_ValidTrackingNumber_ShouldAttemptApiCall() {

        String trackingNumber = "RA644000001RU";

        TrackingInfo result = trackingService.trackParcel(trackingNumber);

        assertThat(result).isNotNull();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getStatusMapping (через рефлексию)
    // =====================================================

    @Test
    @DisplayName("getStatusMapping - должен содержать все коды статусов")
    void getStatusMapping_ShouldContainAllStatusCodes() throws Exception {

        Method method = RussianPostTrackingService.class.getDeclaredMethod("getStatusMapping");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> statusMap = (java.util.Map<String, String>) method.invoke(trackingService);

        assertThat(statusMap).isNotNull();
        assertThat(statusMap).hasSize(12);
        assertThat(statusMap).containsKeys("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
        assertThat(statusMap.get("1")).isEqualTo("Принято в отделении связи");
        assertThat(statusMap.get("5")).isEqualTo("Вручение адресату");
        assertThat(statusMap.get("11")).isEqualTo("Экспорт международной почты");
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ ПАРСИНГА ДАТ (метод в RussianPostTrackingService)
    // =====================================================

    @Test
    @DisplayName("parseDate - корректная дата должна быть распаршена")
    void parseDate_ValidDate_ShouldParse() throws Exception {

        String dateStr = "2024-01-15T10:30:00+03:00";
        Method method = RussianPostTrackingService.class.getDeclaredMethod("parseDate", String.class);
        method.setAccessible(true);

        LocalDateTime result = (LocalDateTime) method.invoke(trackingService, dateStr);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
    }

    @Test
    @DisplayName("parseDate - некорректная дата должна вернуть null")
    void parseDate_InvalidDate_ShouldReturnNull() throws Exception {

        String dateStr = "invalid-date";
        Method method = RussianPostTrackingService.class.getDeclaredMethod("parseDate", String.class);
        method.setAccessible(true);

        LocalDateTime result = (LocalDateTime) method.invoke(trackingService, dateStr);

        assertThat(result).isNull();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ isDelivered
    // =====================================================

    @Test
    @DisplayName("isDelivered - операция 'Вручение адресату' должна считаться доставленной")
    void isDelivered_DeliveredOperation_ShouldReturnTrue() throws Exception {

        Operation operation = Operation.builder()
                .operationName("Вручение адресату")
                .operationId("5")
                .build();

        Method method = RussianPostTrackingService.class.getDeclaredMethod("isDelivered", Operation.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(trackingService, operation);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isDelivered - операция 'В пути' не должна считаться доставленной")
    void isDelivered_TransitOperation_ShouldReturnFalse() throws Exception {

        Operation operation = Operation.builder().operationName("В пути").build();

        Method method = RussianPostTrackingService.class.getDeclaredMethod("isDelivered", Operation.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(trackingService, operation);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isDelivered - null операция должна вернуть false")
    void isDelivered_NullOperation_ShouldReturnFalse() throws Exception {

        Method method = RussianPostTrackingService.class.getDeclaredMethod("isDelivered", Operation.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(trackingService, new Object[] {null});

        assertThat(result).isFalse();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ createSoapRequest (проверка создания запроса)
    // =====================================================

    @Test
    @DisplayName("createSoapRequest - не должен выбрасывать исключения")
    void createSoapRequest_ShouldNotThrowException() throws Exception {

        String trackingNumber = "RA644000001RU";
        Method method = RussianPostTrackingService.class.getDeclaredMethod("createSoapRequest", String.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(trackingService, trackingNumber));
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ parseSoapResponse (проверка парсинга ответа)
    // =====================================================

    @Test
    @DisplayName("parseSoapResponse - null ответ должен обработаться корректно")
    void parseSoapResponse_NullResponse_ShouldHandleGracefully() throws Exception {

        String trackingNumber = "RA644000001RU";
        Method method = RussianPostTrackingService.class.getDeclaredMethod(
                "parseSoapResponse", SOAPMessage.class, String.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(trackingService, new Object[] {null, trackingNumber}));
    }
}
