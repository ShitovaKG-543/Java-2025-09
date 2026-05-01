package ru.otus.trackingbot.service.impl;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.Node;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.otus.trackingbot.model.Operation;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.AbstractTrackingService;

/**
 * Реализация сервиса отслеживания для Почты России.
 * <p>
 * Взаимодействует с SOAP API Почты России (https://tracking.russianpost.ru/rtm34)
 * для получения информации об отслеживании посылок.
 * </p>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *     <li>Поддерживает международные трек-номера формата RA644000001RU</li>
 *     <li>Поддерживает внутренние трек-номера формата 12345678901234</li>
 *     <li>Использует SOAP 1.2 протокол</li>
 *     <li>Требует аутентификации (логин/пароль)</li>
 * </ul>
 */
@Service
@Slf4j
public class RussianPostTrackingService extends AbstractTrackingService {

    @Value("${russianpost.tracking.login}")
    private String login;

    @Value("${russianpost.tracking.password}")
    private String password;

    @Value("${russianpost.tracking.api.url}")
    private String trackingApiUrl;

    private static final String TRACKING_PATTERN = "^([A-Z]{2}\\d{9}[A-Z]{2}|\\d{14})$";

    @Override
    public String getServiceName() {
        return "Почта России";
    }

    @Override
    public String getTrackingNumberPattern() {
        return TRACKING_PATTERN;
    }

    @Override
    public String getStatusDescription(String statusCode) {
        return getStatusMapping().getOrDefault(statusCode, "Неизвестный статус");
    }

    /**
     * Выполняет отслеживание посылки через API Почты России.
     *
     * @param trackingNumber трек-номер посылки
     * @return информация об отслеживании
     */
    @Override
    public TrackingInfo trackParcel(String trackingNumber) {
        String cleanNumber = cleanTrackingNumber(trackingNumber);
        logRequest(cleanNumber);

        if (!isValidTrackingNumber(cleanNumber)) {
            return createErrorResponse(cleanNumber, "Неверный формат трек-номера для Почты России");
        }

        if (login == null
                || login.trim().isEmpty()
                || password == null
                || password.trim().isEmpty()) {
            log.error("Не заданы логин или пароль для API Почты России");
            return createErrorResponse(cleanNumber, "Ошибка конфигурации: не заданы учетные данные");
        }

        try {
            SOAPConnectionFactory factory = SOAPConnectionFactory.newInstance();

            try (SOAPConnection connection = factory.createConnection()) {
                SOAPMessage request = createSoapRequest(cleanNumber);
                SOAPMessage response = connection.call(request, trackingApiUrl);
                return parseSoapResponse(response, cleanNumber);
            } // Автоматическое закрытие connection

        } catch (SOAPException e) {
            log.error("SOAP ошибка при отслеживании посылки {}", cleanNumber, e);
            return createErrorResponse(cleanNumber, "Ошибка SOAP: " + e.getMessage());
        } catch (Exception e) {
            log.error("Неожиданная ошибка при отслеживании посылки {}", cleanNumber, e);
            return createErrorResponse(cleanNumber, "Ошибка: " + e.getMessage());
        }
    }

    /**
     * Создает SOAP-запрос для API Почты России.
     *
     * @param trackingNumber трек-номер
     * @return подготовленный SOAPMessage
     * @throws Exception при ошибке создания запроса
     */
    private SOAPMessage createSoapRequest(String trackingNumber) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance("SOAP 1.2 Protocol");
        SOAPMessage message = messageFactory.createMessage();

        SOAPPart soapPart = message.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody body = envelope.getBody();

        message.getSOAPHeader().detachNode();

        envelope.addNamespaceDeclaration("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        envelope.addNamespaceDeclaration("oper", "http://russianpost.org/operationhistory");
        envelope.addNamespaceDeclaration("data", "http://russianpost.org/operationhistory/data");

        SOAPElement operElement = body.addChildElement("getOperationHistory", "oper");
        SOAPElement dataElement = operElement.addChildElement("OperationHistoryRequest", "data");

        SOAPElement barcode = dataElement.addChildElement("Barcode", "data");
        barcode.addTextNode(trackingNumber);

        SOAPElement messageType = dataElement.addChildElement("MessageType", "data");
        messageType.addTextNode("0");

        SOAPElement language = dataElement.addChildElement("Language", "data");
        language.addTextNode("RUS");

        SOAPElement authHeader = operElement.addChildElement("AuthorizationHeader", "data");

        SOAPElement loginElement = authHeader.addChildElement("login", "data");
        loginElement.addTextNode(login);

        SOAPElement passwordElement = authHeader.addChildElement("password", "data");
        passwordElement.addTextNode(password);

        message.saveChanges();
        return message;
    }

    /**
     * Парсит SOAP-ответ от API Почты России.
     *
     * @param response SOAP-ответ
     * @param trackingNumber трек-номер
     * @ @return информация об отслеживании
     */
    private TrackingInfo parseSoapResponse(SOAPMessage response, String trackingNumber) {
        try {
            TrackingInfo.TrackingInfoBuilder builder = TrackingInfo.builder()
                    .trackingNumber(trackingNumber)
                    .serviceName(getServiceName())
                    .success(true)
                    .lastCheck(LocalDateTime.now());

            SOAPBody body = response.getSOAPBody();

            if (body.hasFault()) {
                SOAPFault fault = body.getFault();
                return createErrorResponse(trackingNumber, "Ошибка API: " + fault.getFaultString());
            }

            List<Operation> operations = extractOperations(body);

            if (!operations.isEmpty()) {
                operations.sort((a, b) -> {
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    return b.getDate().compareTo(a.getDate());
                });
                Operation lastOp = operations.get(0);

                builder.status(lastOp.getOperationName())
                        .statusDescription(getStatusDescription(lastOp.getOperationId()))
                        .lastOperation(lastOp)
                        .allOperations(operations)
                        .delivered(isDelivered(lastOp));

                if (lastOp.getWeight() != null && lastOp.getWeight() > 0) {
                    builder.weight(lastOp.getWeight() / 1000.0);
                }
            } else {
                builder.status("Информация отсутствует")
                        .statusDescription("По данному трек-номеру информация пока не найдена");
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Ошибка парсинга SOAP ответа", e);
            return createErrorResponse(trackingNumber, "Ошибка обработки ответа от сервера");
        }
    }

    /**
     * Извлекает операции из SOAP-ответа.
     *
     * @param body SOAPBody
     * @return список операций
     */
    private List<Operation> extractOperations(SOAPBody body) {
        List<Operation> operations = new ArrayList<>();

        try {
            Iterator<?> elements = body.getChildElements();
            while (elements.hasNext()) {
                Node node = (Node) elements.next();
                if (node instanceof SOAPElement) {
                    findOperationsInElement((SOAPElement) node, operations);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при извлечении операций", e);
        }

        return operations;
    }

    /**
     * Рекурсивно ищет операции в SOAP-элементах.
     *
     * @param element SOAP-элемент
     * @param operations список для добавления операций
     */
    private void findOperationsInElement(SOAPElement element, List<Operation> operations) {
        try {
            Iterator<?> children = element.getChildElements();
            while (children.hasNext()) {
                Node child = (Node) children.next();
                if (child instanceof SOAPElement) {
                    SOAPElement childElement = (SOAPElement) child;

                    if ("OperationHistoryData".equals(childElement.getLocalName())) {
                        parseOperationData(childElement, operations);
                    } else {
                        findOperationsInElement(childElement, operations);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при поиске операций", e);
        }
    }

    /**
     * Парсит данные операции из SOAP-элемента.
     *
     * @param element SOAP-элемент с данными
     * @param operations список для добавления операций
     */
    private void parseOperationData(SOAPElement element, List<Operation> operations) {
        try {
            Iterator<?> records = element.getChildElements();
            while (records.hasNext()) {
                Node record = (Node) records.next();
                if (record instanceof SOAPElement && "historyRecord".equals(((SOAPElement) record).getLocalName())) {
                    Operation op = parseHistoryRecord((SOAPElement) record);
                    if (op != null && op.getDate() != null) {
                        operations.add(op);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка парсинга данных операции", e);
        }
    }

    /**
     * Парсит отдельную запись истории.
     *
     * @param recordElement SOAP-элемент записи
     * @ @return объект Operation
     */
    private Operation parseHistoryRecord(SOAPElement recordElement) {
        try {
            Operation.OperationBuilder builder = Operation.builder().serviceName(getServiceName());

            Iterator<?> params = recordElement.getChildElements();
            while (params.hasNext()) {
                Node param = (Node) params.next();
                if (param instanceof SOAPElement) {
                    SOAPElement paramElement = (SOAPElement) param;
                    String paramName = paramElement.getLocalName();

                    switch (paramName) {
                        case "OperationParameters":
                            parseOperationParameters(paramElement, builder);
                            break;
                        case "AddressParameters":
                            parseAddressParameters(paramElement, builder);
                            break;
                        case "ItemParameters":
                            parseItemParameters(paramElement, builder);
                            break;
                    }
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Ошибка парсинга historyRecord", e);
            return null;
        }
    }

    /**
     * Парсит параметры операции.
     */
    private void parseOperationParameters(SOAPElement element, Operation.OperationBuilder builder) {
        try {
            Iterator<?> params = element.getChildElements();
            while (params.hasNext()) {
                Node param = (Node) params.next();
                if (param instanceof SOAPElement) {
                    SOAPElement paramElement = (SOAPElement) param;

                    if ("OperType".equals(paramElement.getLocalName())) {
                        parseOperType(paramElement, builder);
                    } else if ("OperDate".equals(paramElement.getLocalName())) {
                        String dateStr = paramElement.getValue();
                        if (dateStr != null && !dateStr.isEmpty()) {
                            builder.date(parseDate(dateStr));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка парсинга параметров операции", e);
        }
    }

    /**
     * Парсит тип операции.
     */
    private void parseOperType(SOAPElement element, Operation.OperationBuilder builder) {
        try {
            Iterator<?> params = element.getChildElements();
            while (params.hasNext()) {
                Node param = (Node) params.next();
                if (param instanceof SOAPElement) {
                    SOAPElement paramElement = (SOAPElement) param;
                    String name = paramElement.getLocalName();

                    if ("Id".equals(name)) {
                        builder.operationId(paramElement.getValue());
                    } else if ("Name".equals(name)) {
                        builder.operationName(paramElement.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка парсинга типа операции", e);
        }
    }

    /**
     * Парсит адресные параметры.
     */
    private void parseAddressParameters(SOAPElement element, Operation.OperationBuilder builder) {
        try {
            Iterator<?> params = element.getChildElements();
            while (params.hasNext()) {
                Node param = (Node) params.next();
                if (param instanceof SOAPElement && "OperationAddress".equals(((SOAPElement) param).getLocalName())) {
                    SOAPElement addressElement = (SOAPElement) param;
                    Iterator<?> addrParams = addressElement.getChildElements();

                    String description = null;
                    String index = null;

                    while (addrParams.hasNext()) {
                        Node addrParam = (Node) addrParams.next();
                        if (addrParam instanceof SOAPElement) {
                            SOAPElement addrElement = (SOAPElement) addrParam;
                            String addrName = addrElement.getLocalName();

                            if ("Description".equals(addrName)) {
                                description = addrElement.getValue();
                            } else if ("Index".equals(addrName)) {
                                index = addrElement.getValue();
                            }
                        }
                    }

                    if (index != null && description != null) {
                        builder.operationPlace(index + ", " + description);
                    } else if (description != null) {
                        builder.operationPlace(description);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка парсинга адресных параметров", e);
        }
    }

    /**
     * Парсит параметры отправления (вес и т.д.).
     */
    private void parseItemParameters(SOAPElement element, Operation.OperationBuilder builder) {
        try {
            Iterator<?> params = element.getChildElements();
            while (params.hasNext()) {
                Node param = (Node) params.next();
                if (param instanceof SOAPElement) {
                    SOAPElement paramElement = (SOAPElement) param;

                    if ("Mass".equals(paramElement.getLocalName())) {
                        String massStr = paramElement.getValue();
                        if (massStr != null && !massStr.isEmpty()) {
                            try {
                                builder.weight(Integer.parseInt(massStr));
                            } catch (NumberFormatException e) {
                                log.debug("Не удалось распарсить вес: {}", massStr);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка парсинга параметров отправления", e);
        }
    }

    /**
     * Парсит строку даты в LocalDateTime.
     *
     * @param dateStr строка даты в формате ISO
     * @ @return LocalDateTime или null при ошибке
     */
    private LocalDateTime parseDate(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Ошибка парсинга даты: {}", dateStr);
            return null;
        }
    }

    /**
     * Определяет, доставлена ли посылка по операции.
     *
     * @param operation операция
     * @return true если посылка доставлена
     */
    private boolean isDelivered(Operation operation) {
        if (operation == null || operation.getOperationName() == null) return false;
        String opName = operation.getOperationName().toLowerCase();
        return opName.contains("вручено")
                || opName.contains("получено")
                || opName.contains("доставлено")
                || "5".equals(operation.getOperationId());
    }

    /**
     * Возвращает маппинг кодов статусов Почты России.
     *
     * @return Map с маппингом статусов
     */
    @Override
    protected Map<String, String> getStatusMapping() {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("1", "Принято в отделении связи");
        statusMap.put("2", "Прибыло в сортировочный центр");
        statusMap.put("3", "Покинуло сортировочный центр");
        statusMap.put("4", "Прибыло в место вручения");
        statusMap.put("5", "Вручение адресату");
        statusMap.put("6", "Временное хранение");
        statusMap.put("7", "Возврат");
        statusMap.put("8", "Неудачная попытка вручения");
        statusMap.put("9", "Таможенное оформление");
        statusMap.put("10", "Импорт международной почты");
        statusMap.put("11", "Экспорт международной почты");
        statusMap.put("12", "Регистрация отправления");
        return statusMap;
    }
}
