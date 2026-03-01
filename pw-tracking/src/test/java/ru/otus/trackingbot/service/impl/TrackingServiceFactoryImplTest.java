package ru.otus.trackingbot.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.otus.trackingbot.service.AbstractTrackingService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingServiceFactoryImpl тесты")
class TrackingServiceFactoryImplTest {

    @Mock
    private AbstractTrackingService russianPostService;

    @Mock
    private AbstractTrackingService anotherService;

    private TrackingServiceFactoryImpl factory;

    @BeforeEach
    void setUp() {

        when(russianPostService.getServiceName()).thenReturn("Почта России");
        when(anotherService.getServiceName()).thenReturn("DHL");

        List<AbstractTrackingService> services = Arrays.asList(russianPostService, anotherService);
        factory = new TrackingServiceFactoryImpl(services);
        factory.init();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ КОНСТРУКТОРА И INIT
    // =====================================================

    @Test
    @DisplayName("init - должен зарегистрировать все сервисы в кеше")
    void init_ShouldRegisterAllServicesInCache() {
        Optional<AbstractTrackingService> russianPost = factory.getService("Почта России");
        Optional<AbstractTrackingService> dhl = factory.getService("DHL");

        assertThat(russianPost).isPresent();
        assertThat(russianPost.get()).isEqualTo(russianPostService);

        assertThat(dhl).isPresent();
        assertThat(dhl.get()).isEqualTo(anotherService);
    }

    @Test
    @DisplayName("init - должен инициализировать статистику для всех сервисов")
    void init_ShouldInitializeStatsForAllServices() {
        Map<String, Integer> stats = factory.getServicesStatistics();
        assertThat(stats).containsKeys("Почта России", "DHL");
        assertThat(stats.get("Почта России")).isEqualTo(0);
        assertThat(stats.get("DHL")).isEqualTo(0);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getService
    // =====================================================

    @Test
    @DisplayName("getService - существующий сервис должен быть найден")
    void getService_ExistingService_ShouldReturnService() {
        Optional<AbstractTrackingService> result = factory.getService("Почта России");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(russianPostService);
    }

    @Test
    @DisplayName("getService - поиск должен быть регистронезависимым")
    void getService_ShouldBeCaseInsensitive() {
        Optional<AbstractTrackingService> result1 = factory.getService("почта россии");
        Optional<AbstractTrackingService> result2 = factory.getService("ПОЧТА РОССИИ");

        assertThat(result1).isPresent();
        assertThat(result2).isPresent();
        assertThat(result1.get()).isEqualTo(russianPostService);
        assertThat(result2.get()).isEqualTo(russianPostService);
    }

    @Test
    @DisplayName("getService - несуществующий сервис должен вернуть пустой Optional")
    void getService_NonExistingService_ShouldReturnEmpty() {
        Optional<AbstractTrackingService> result = factory.getService("UPS");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getService - null аргумент должен вернуть пустой Optional")
    void getService_NullArgument_ShouldReturnEmpty() {
        Optional<AbstractTrackingService> result = factory.getService(null);

        assertThat(result).isEmpty();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ detectService
    // =====================================================

    @Test
    @DisplayName("detectService - должен определить Почту России по международному трек-номеру")
    void detectService_InternationalTrackingNumber_ShouldDetectRussianPost() {

        when(russianPostService.isValidTrackingNumber("RA644000001RU")).thenReturn(true);

        String trackingNumber = "RA644000001RU";

        Optional<AbstractTrackingService> result = factory.detectService(trackingNumber);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(russianPostService);
        // Проверяем, что второй сервис не вызывался
        verify(anotherService, never()).isValidTrackingNumber(anyString());
    }

    @Test
    @DisplayName("detectService - должен определить DHL по цифровому трек-номеру")
    void detectService_DigitalTrackingNumber_ShouldDetectDHL() {

        when(russianPostService.isValidTrackingNumber("1234567890")).thenReturn(false);
        when(anotherService.isValidTrackingNumber("1234567890")).thenReturn(true);

        String trackingNumber = "1234567890";

        Optional<AbstractTrackingService> result = factory.detectService(trackingNumber);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(anotherService);
    }

    @Test
    @DisplayName("detectService - должен очищать трек-номер от лишних символов")
    void detectService_ShouldCleanTrackingNumber() {

        when(russianPostService.isValidTrackingNumber("RA644000001RU")).thenReturn(true);
        String trackingNumber = " RA-6440-0000-1RU ";

        Optional<AbstractTrackingService> result = factory.detectService(trackingNumber);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(russianPostService);
        verify(russianPostService).isValidTrackingNumber("RA644000001RU");
    }

    @Test
    @DisplayName("detectService - null трек-номер должен вернуть пустой Optional")
    void detectService_NullTrackingNumber_ShouldReturnEmpty() {
        Optional<AbstractTrackingService> result = factory.detectService(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("detectService - пустой трек-номер должен вернуть пустой Optional")
    void detectService_EmptyTrackingNumber_ShouldReturnEmpty() {
        Optional<AbstractTrackingService> result = factory.detectService("");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("detectService - неподдерживаемый формат должен вернуть пустой Optional")
    void detectService_UnsupportedFormat_ShouldReturnEmpty() {

        when(russianPostService.isValidTrackingNumber(anyString())).thenReturn(false);
        when(anotherService.isValidTrackingNumber(anyString())).thenReturn(false);

        String trackingNumber = "UNSUPPORTED123";

        Optional<AbstractTrackingService> result = factory.detectService(trackingNumber);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("detectService - должен увеличивать статистику при успешном определении")
    void detectService_ShouldIncrementStatsOnSuccessfulDetection() {

        when(russianPostService.isValidTrackingNumber("RA644000001RU")).thenReturn(true);
        String trackingNumber = "RA644000001RU";

        factory.detectService(trackingNumber);
        Map<String, Integer> stats = factory.getServicesStatistics();

        assertThat(stats.get("Почта России")).isEqualTo(1);
        assertThat(stats.get("DHL")).isEqualTo(0);
    }

    @Test
    @DisplayName("detectService - при неудачном определении статистика не должна увеличиваться")
    void detectService_UnsuccessfulDetection_ShouldNotIncrementStats() {

        when(russianPostService.isValidTrackingNumber(anyString())).thenReturn(false);
        when(anotherService.isValidTrackingNumber(anyString())).thenReturn(false);
        String trackingNumber = "UNSUPPORTED123";

        factory.detectService(trackingNumber);
        Map<String, Integer> stats = factory.getServicesStatistics();

        assertThat(stats.get("Почта России")).isEqualTo(0);
        assertThat(stats.get("DHL")).isEqualTo(0);
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getAllServices
    // =====================================================

    @Test
    @DisplayName("getAllServices - должен вернуть неизменяемый список всех сервисов")
    void getAllServices_ShouldReturnUnmodifiableList() {
        List<AbstractTrackingService> allServices = factory.getAllServices();

        assertThat(allServices).hasSize(2);
        assertThat(allServices).containsExactly(russianPostService, anotherService);
        assertThat(allServices).isUnmodifiable();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ isSupported
    // =====================================================

    @Test
    @DisplayName("isSupported - поддерживаемый трек-номер должен вернуть true")
    void isSupported_SupportedTrackingNumber_ShouldReturnTrue() {

        when(russianPostService.isValidTrackingNumber("RA644000001RU")).thenReturn(true);
        String trackingNumber = "RA644000001RU";

        boolean supported = factory.isSupported(trackingNumber);

        assertThat(supported).isTrue();
    }

    @Test
    @DisplayName("isSupported - неподдерживаемый трек-номер должен вернуть false")
    void isSupported_UnsupportedTrackingNumber_ShouldReturnFalse() {

        when(russianPostService.isValidTrackingNumber(anyString())).thenReturn(false);
        when(anotherService.isValidTrackingNumber(anyString())).thenReturn(false);
        String trackingNumber = "UNSUPPORTED123";

        boolean supported = factory.isSupported(trackingNumber);

        assertThat(supported).isFalse();
    }

    @Test
    @DisplayName("isSupported - null трек-номер должен вернуть false")
    void isSupported_NullTrackingNumber_ShouldReturnFalse() {
        boolean supported = factory.isSupported(null);

        assertThat(supported).isFalse();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ getServicesStatistics
    // =====================================================

    @Test
    @DisplayName("getServicesStatistics - должен вернуть неизменяемую карту статистики")
    void getServicesStatistics_ShouldReturnUnmodifiableMap() {
        Map<String, Integer> stats = factory.getServicesStatistics();

        assertThat(stats).containsKeys("Почта России", "DHL");
        assertThat(stats).isUnmodifiable();
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ ПОРЯДКА ОПРЕДЕЛЕНИЯ СЕРВИСОВ
    // =====================================================

    @Test
    @DisplayName("detectService - должен проверять сервисы в порядке их регистрации")
    void detectService_ShouldCheckServicesInRegistrationOrder() {

        AbstractTrackingService service1 = mock(AbstractTrackingService.class);
        AbstractTrackingService service2 = mock(AbstractTrackingService.class);

        when(service1.getServiceName()).thenReturn("Service1");
        when(service2.getServiceName()).thenReturn("Service2");
        when(service1.isValidTrackingNumber(anyString())).thenReturn(true);

        TrackingServiceFactoryImpl testFactory = new TrackingServiceFactoryImpl(Arrays.asList(service1, service2));
        testFactory.init();

        Optional<AbstractTrackingService> result = testFactory.detectService("ANY123");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(service1);

        // Проверяем, что второй сервис не проверялся
        verify(service2, never()).isValidTrackingNumber(anyString());
    }

    // =====================================================
    // ТЕСТЫ ДЛЯ РАБОТЫ С ПУСТЫМ СПИСКОМ СЕРВИСОВ
    // =====================================================

    @Test
    @DisplayName("конструктор - с пустым списком сервисов должен работать корректно")
    void constructor_WithEmptyServiceList_ShouldWorkCorrectly() {

        TrackingServiceFactoryImpl emptyFactory = new TrackingServiceFactoryImpl(Collections.emptyList());
        emptyFactory.init();

        assertThat(emptyFactory.getAllServices()).isEmpty();
        assertThat(emptyFactory.detectService("RA644000001RU")).isEmpty();
        assertThat(emptyFactory.isSupported("RA644000001RU")).isFalse();
        assertThat(emptyFactory.getServicesStatistics()).isEmpty();
    }
}
