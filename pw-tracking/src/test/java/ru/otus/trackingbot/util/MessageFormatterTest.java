package ru.otus.trackingbot.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.otus.trackingbot.entity.Parcel;
import ru.otus.trackingbot.entity.ParcelStatusHistory;
import ru.otus.trackingbot.entity.User;
import ru.otus.trackingbot.entity.UserParcel;
import ru.otus.trackingbot.model.Operation;
import ru.otus.trackingbot.model.TrackingInfo;

@DisplayName("MessageFormatter тесты")
class MessageFormatterTest {

    private static final String TRACKING_NUMBER = "TRK123456789";
    private static final String SERVICE_NAME = "Почта России";
    private static final String DESCRIPTION = "Смартфон";
    private static final BigDecimal WEIGHT = new BigDecimal("0.75");
    private static final String STATUS = "IN_TRANSIT";
    private static final String STATUS_DESCRIPTION = "Посылка в пути";
    private static final String OLD_STATUS = "ACCEPTED";
    private static final String NEW_STATUS = "IN_TRANSIT";
    private static final LocalDateTime TEST_DATE = LocalDateTime.of(2024, Month.DECEMBER, 25, 14, 30, 45);
    private static final LocalDateTime OPERATION_DATE = LocalDateTime.of(2024, Month.DECEMBER, 24, 10, 15, 0);
    private static final String OPERATION_PLACE = "Москва, Главпочтамт";
    private static final String OPERATION_NAME = "Прибытие в сортировочный центр";
    private static final LocalDate ESTIMATED_DELIVERY = LocalDate.of(2024, Month.DECEMBER, 28);
    private static final String TITLE = "📦 Информация об отслеживании";

    private User testUser;
    private Parcel testParcel;
    private UserParcel testUserParcel;
    private ParcelStatusHistory testStatusHistory;
    private TrackingInfo testTrackingInfo;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .chatId(123456789L)
                .username("testuser")
                .firstName("Тест")
                .lastName("Пользователь")
                .isActive(true)
                .notificationEnabled(true)
                .build();

        testParcel = Parcel.builder()
                .id(1L)
                .trackingNumber(TRACKING_NUMBER)
                .serviceName(SERVICE_NAME)
                .description(DESCRIPTION)
                .weight(WEIGHT)
                .estimatedDelivery(ESTIMATED_DELIVERY)
                .build();

        testUserParcel = UserParcel.builder()
                .id(1L)
                .user(testUser)
                .parcel(testParcel)
                .isActive(true)
                .notificationCount(5)
                .lastStatus(STATUS)
                .lastStatusDescription(STATUS_DESCRIPTION)
                .lastChecked(TEST_DATE)
                .customName("Моя посылка")
                .build();

        testUserParcel.setAddedAt(TEST_DATE.minusDays(7));

        testStatusHistory = ParcelStatusHistory.builder()
                .id(1L)
                .parcel(testParcel)
                .statusCode("ARRIVED")
                .statusName(OPERATION_NAME)
                .statusDescription("Посылка прибыла в сортировочный центр")
                .operationDate(OPERATION_DATE)
                .operationPlace(OPERATION_PLACE)
                .weight(750)
                .isCurrent(true)
                .build();

        Operation lastOperation = Operation.builder()
                .date(OPERATION_DATE)
                .operationPlace(OPERATION_PLACE)
                .operationName(OPERATION_NAME)
                .build();

        testTrackingInfo = TrackingInfo.builder()
                .trackingNumber(TRACKING_NUMBER)
                .serviceName(SERVICE_NAME)
                .success(true)
                .status(NEW_STATUS)
                .statusDescription(STATUS_DESCRIPTION)
                .delivered(false)
                .weight(0.75)
                .lastCheck(TEST_DATE)
                .lastOperation(lastOperation)
                .estimatedDelivery("28.12.2024")
                .build();
    }

    @Test
    @DisplayName("Должен форматировать полную информацию о посылке со всеми заполненными полями")
    void shouldFormatCompleteParcelInfo() {

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, testStatusHistory);

        assertThat(result).contains("<b>📦 Полная информация о посылке</b>");
        assertThat(result).contains(TRACKING_NUMBER);
        assertThat(result).contains(SERVICE_NAME);
        assertThat(result).contains(DESCRIPTION);
        assertThat(result).contains("0,75 кг");
        assertThat(result).contains(ESTIMATED_DELIVERY.toString());
        assertThat(result).contains(STATUS);
        assertThat(result).contains(STATUS_DESCRIPTION);
        assertThat(result).contains("Последняя проверка:");
        assertThat(result).contains(DateUtils.format(TEST_DATE));
        assertThat(result).contains("🔔 <b>Уведомлений отправлено:</b> 5");
        assertThat(result).contains(DateUtils.format(testUserParcel.getAddedAt()));
        assertThat(result).contains("✅ <b>Отслеживание:</b> активно");
        assertThat(result).contains(OPERATION_NAME);
        assertThat(result).contains(OPERATION_PLACE);
        assertThat(result).contains(DateUtils.format(OPERATION_DATE));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие опциональных полей")
    void shouldHandleMissingOptionalFields() {

        testParcel.setDescription(null);
        testParcel.setWeight(null);
        testParcel.setEstimatedDelivery(null);
        testUserParcel.setLastStatusDescription(null);
        testUserParcel.setCustomName(null);

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).doesNotContain("📝 <b>Описание:</b>");
        assertThat(result).doesNotContain("⚖️ <b>Вес:</b>");
        assertThat(result).doesNotContain("📅 <b>Ожидаемая дата доставки:</b>");
        assertThat(result).doesNotContain("📝 <i>");
        assertThat(result).contains(TRACKING_NUMBER);
        assertThat(result).contains(SERVICE_NAME);
        assertThat(result).contains(STATUS);
    }

    @Test
    @DisplayName("Должен показывать 'Статус неизвестен', когда lastStatus равен null или пустой строке")
    void shouldShowUnknownStatusWhenLastStatusIsNull() {

        testUserParcel.setLastStatus(null);

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).contains("Статус неизвестен");
    }

    @Test
    @DisplayName("Должен показывать 'Статус неизвестен', когда lastStatus является пустой строкой")
    void shouldShowUnknownStatusWhenLastStatusIsEmpty() {

        testUserParcel.setLastStatus("");

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).contains("Статус неизвестен");
    }

    @Test
    @DisplayName("Должен показывать 'отслеживание остановлено', когда isActive равен false")
    void shouldShowTrackingStoppedWhenInactive() {

        testUserParcel.setIsActive(false);

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).contains("⏸ <b>Отслеживание:</b> остановлено");
        assertThat(result).doesNotContain("✅ <b>Отслеживание:</b> активно");
    }

    @Test
    @DisplayName("Должен включать эмодзи статуса для известных статусов")
    void shouldIncludeStatusEmojiForKnownStatuses() {

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).contains(StatusEmojiUtils.getEmoji(STATUS));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать null lastStatusHistory")
    void shouldHandleNullLastStatusHistory() {

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).doesNotContain("📍 Последняя операция в истории:");
        assertThat(result).contains(TRACKING_NUMBER);
        assertThat(result).contains(STATUS);
    }

    @Test
    @DisplayName("Должен корректно форматировать вес с двумя десятичными знаками")
    void shouldFormatWeightCorrectly() {

        testParcel.setWeight(new BigDecimal("1.5"));

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).contains("1,50 кг");
    }

    @Test
    @DisplayName("Не должен показывать поле веса, когда вес равен null")
    void shouldNotShowWeightWhenNull() {

        testParcel.setWeight(null);

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).doesNotContain("⚖️ <b>Вес:</b>");
    }

    @Test
    @DisplayName("Не должен показывать поле веса, когда вес равен нулю")
    void shouldNotShowWeightWhenZero() {

        testParcel.setWeight(BigDecimal.ZERO);

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, null);

        assertThat(result).doesNotContain("⚖️ <b>Вес:</b>");
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие места операции в истории статусов")
    void shouldHandleNullOperationPlace() {

        testStatusHistory.setOperationPlace(null);

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, testStatusHistory);

        assertThat(result).doesNotContain("🏢 <b>Место:</b>");
        assertThat(result).contains(OPERATION_NAME);
        assertThat(result).contains(DateUtils.format(OPERATION_DATE));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие даты операции в истории статусов")
    void shouldHandleNullOperationDate() {

        testStatusHistory.setOperationDate(null);

        String result = MessageFormatter.formatParcelInfoFromDB(testUserParcel, testParcel, testStatusHistory);

        assertThat(result).doesNotContain("🕐 <b>Дата:</b>");
        assertThat(result).contains(OPERATION_NAME);
    }

    @Test
    @DisplayName("Должен форматировать полную информацию об отслеживании со всеми полями")
    void shouldFormatCompleteTrackingInfo() {

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).startsWith(TITLE);
        assertThat(result).contains(StatusEmojiUtils.getEmoji(NEW_STATUS));
        assertThat(result).contains(NEW_STATUS);
        assertThat(result).contains(STATUS_DESCRIPTION);
        assertThat(result).contains(TRACKING_NUMBER);
        assertThat(result).contains(SERVICE_NAME);
        assertThat(result).contains("0,75 кг");
        assertThat(result).contains(DateUtils.format(OPERATION_DATE));
        assertThat(result).contains(OPERATION_PLACE);
        assertThat(result).contains(DateUtils.format(TEST_DATE));
    }

    @Test
    @DisplayName("Должен показывать сообщение о доставке, когда delivered равен true")
    void shouldShowDeliveredMessage() {

        testTrackingInfo.setDelivered(true);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).contains("✅ <b>Посылка доставлена!</b> 🎉");
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие описания статуса")
    void shouldHandleMissingStatusDescription() {

        testTrackingInfo.setStatusDescription(null);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).doesNotContain("📝");
        assertThat(result).contains(NEW_STATUS);
        assertThat(result).contains(TRACKING_NUMBER);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие веса")
    void shouldHandleMissingWeight() {

        testTrackingInfo.setWeight(null);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).doesNotContain("⚖️ <b>Вес:</b>");
    }

    @Test
    @DisplayName("Должен корректно обрабатывать вес равный нулю")
    void shouldHandleWeightZero() {

        testTrackingInfo.setWeight(0.0);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).doesNotContain("⚖️ <b>Вес:</b>");
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие последней операции")
    void shouldHandleMissingLastOperation() {

        testTrackingInfo.setLastOperation(null);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).doesNotContain("📍 Последняя операция:");
        assertThat(result).contains(NEW_STATUS);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие места в последней операции")
    void shouldHandleMissingOperationPlace() {

        testTrackingInfo.getLastOperation().setOperationPlace(null);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).doesNotContain("🏢");
        assertThat(result).contains(DateUtils.format(OPERATION_DATE));
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие даты в последней операции")
    void shouldHandleMissingDateInLastOperation() {

        testTrackingInfo.getLastOperation().setDate(null);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).doesNotContain("🕐");
        assertThat(result).doesNotContain("📍 Последняя операция:");
    }

    @Test
    @DisplayName("Должен форматировать вес с двумя десятичными знаками")
    void shouldFormatWeightWithTwoDecimalPlaces() {

        testTrackingInfo.setWeight(1.5);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        assertThat(result).contains("1,50 кг");
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отрицательный вес - вес не отображается")
    void shouldHandleNegativeWeight() {

        testTrackingInfo.setWeight(-1.0);

        String result = MessageFormatter.formatTrackingInfo(testTrackingInfo, TITLE);

        // Отрицательный вес не отображается в сообщении
        assertThat(result).doesNotContain("⚖️ <b>Вес:</b>");
    }

    @Test
    @DisplayName("Должен форматировать полное уведомление об изменении статуса")
    void shouldFormatCompleteStatusChangeNotification() {

        String result = MessageFormatter.formatStatusChangeNotification(
                testUserParcel, testTrackingInfo, OLD_STATUS, NEW_STATUS);

        assertThat(result).contains("🔔 <b>Изменение статуса посылки!</b>");
        assertThat(result).contains(TRACKING_NUMBER);
        assertThat(result).contains("📌 <b>Было:</b> " + OLD_STATUS);
        assertThat(result).contains("📌 <b>Стало:</b> " + NEW_STATUS);
        assertThat(result).contains(STATUS_DESCRIPTION);
    }

    @Test
    @DisplayName("Должен показывать 'неизвестно', когда oldStatus равен null")
    void shouldShowUnknownWhenOldStatusIsNull() {

        String result =
                MessageFormatter.formatStatusChangeNotification(testUserParcel, testTrackingInfo, null, NEW_STATUS);

        assertThat(result).contains("📌 <b>Было:</b> неизвестно");
        assertThat(result).contains("📌 <b>Стало:</b> " + NEW_STATUS);
    }

    @Test
    @DisplayName("Должен корректно обрабатывать отсутствие описания статуса в уведомлении")
    void shouldHandleMissingStatusDescriptionInNotification() {

        testTrackingInfo.setStatusDescription(null);

        String result = MessageFormatter.formatStatusChangeNotification(
                testUserParcel, testTrackingInfo, OLD_STATUS, NEW_STATUS);

        assertThat(result).doesNotContain("📝");
        assertThat(result).contains(TRACKING_NUMBER);
        assertThat(result).contains(OLD_STATUS);
        assertThat(result).contains(NEW_STATUS);
    }

    @Test
    @DisplayName("Должен показывать поздравление с доставкой, когда delivered равен true")
    void shouldShowDeliveryCelebration() {

        testTrackingInfo.setDelivered(true);

        String result = MessageFormatter.formatStatusChangeNotification(
                testUserParcel, testTrackingInfo, OLD_STATUS, "DELIVERED");

        assertThat(result).contains("🎉 <b>ПОСЫЛКА ДОСТАВЛЕНА!</b> 🎉");
    }

    @Test
    @DisplayName("Не должен показывать поздравление с доставкой, когда посылка не доставлена")
    void shouldNotShowDeliveryCelebrationWhenNotDelivered() {

        testTrackingInfo.setDelivered(false);

        String result = MessageFormatter.formatStatusChangeNotification(
                testUserParcel, testTrackingInfo, OLD_STATUS, NEW_STATUS);

        assertThat(result).doesNotContain("ПОСЫЛКА ДОСТАВЛЕНА");
    }

    @Test
    @DisplayName("Должен корректно обрабатывать пустую строку старого статуса")
    void shouldHandleEmptyOldStatus() {

        String result =
                MessageFormatter.formatStatusChangeNotification(testUserParcel, testTrackingInfo, "", NEW_STATUS);

        assertThat(result).contains("📌 <b>Было:</b> ");
        // Проверяем, что после двоеточия нет текста до перевода строки
        assertThat(result).contains("📌 <b>Было:</b> \n📌 <b>Стало:</b>");
    }

    @Test
    @DisplayName("Должен иметь приватный конструктор")
    void shouldHavePrivateConstructor() throws Exception {

        Class<MessageFormatter> clazz = MessageFormatter.class;

        java.lang.reflect.Constructor<MessageFormatter> constructor = clazz.getDeclaredConstructor();

        assertThat(constructor.isAccessible()).isFalse();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()))
                .isTrue();

        // Делаем конструктор доступным для тестирования
        constructor.setAccessible(true);
        MessageFormatter instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
