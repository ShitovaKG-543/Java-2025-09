package ru.otus.trackingbot.service.impl;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.otus.trackingbot.model.Operation;
import ru.otus.trackingbot.model.TrackingInfo;
import ru.otus.trackingbot.service.AbstractTrackingService;

@Service
@Slf4j
public class RussianPostTrackingService extends AbstractTrackingService {

    @Value("${russianpost.tracking.login}")
    private String login;

    @Value("${russianpost.tracking.password}")
    private String password;

    private static final String TRACKING_API_URL = "https://tracking.russianpost.ru/rtm34";
    private static final String SOAP_NAMESPACE = "http://www.w3.org/2003/05/soap-envelope";
    private static final String OPER_NAMESPACE = "http://russianpost.org/operationhistory";
    private static final String DATA_NAMESPACE = "http://russianpost.org/operationhistory/data";
    private static final String SOAPENV_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";

    private static final String TRACKING_PATTERN = "^([A-Z]{2}\\d{9}[A-Z]{2}|\\d{14})$";

    private static final Map<String, String> STATUS_MAP = new HashMap<>() {
        {
            put("1", "Принято в отделении связи");
            put("2", "Прибыло в сортировочный центр");
            put("3", "Покинуло сортировочный центр");
            put("4", "Прибыло в место вручения");
            put("5", "Вручение адресату");
            put("6", "Временное хранение");
            put("7", "Возврат");
            put("8", "Неудачная попытка вручения");
            put("9", "Таможенное оформление");
            put("10", "Импорт международной почты");
            put("11", "Экспорт международной почты");
            put("12", "Регистрация отправления");
            put("13", "Покинуло место международного обмена");
            put("14", "Прибыло в место международного обмена");
        }
    };

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
        return STATUS_MAP.getOrDefault(statusCode, "Неизвестный статус");
    }

    @Override
    public TrackingInfo trackParcel(String trackingNumber) {
        String cleanNumber = cleanTrackingNumber(trackingNumber);
        logRequest(cleanNumber);

        log.debug(
                "Используем логин: {}, пароль: {}",
                login != null ? "задан" : "не задан",
                password != null ? "задан" : "не задан");

        if (!isValidTrackingNumber(cleanNumber)) {
            return createErrorResponse(cleanNumber, "Неверный формат трек-номера для Почты России");
        }

        if (login == null
                || login.trim().isEmpty()
                || password == null
                || password.trim().isEmpty()) {
            log.error("Не заданы логин или пароль для API Почты России");
            return createErrorResponse(
                    cleanNumber, "Ошибка конфигурации: не заданы учетные данные для API Почты России");
        }

        SOAPConnection connection = null;
        try {
            // Создаем SOAP соединение
            SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
            connection = soapConnFactory.createConnection();

            // Создаем сообщение с SOAP 1.2
            MessageFactory messageFactory = MessageFactory.newInstance("SOAP 1.2 Protocol");
            SOAPMessage message = messageFactory.createMessage();

            // Получаем части сообщения
            SOAPPart soapPart = message.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPBody body = envelope.getBody();

            // Очищаем заголовок (он не нужен)
            message.getSOAPHeader().detachNode();

            // Добавляем пространства имен как в примере
            envelope.addNamespaceDeclaration("soap", SOAP_NAMESPACE);
            envelope.addNamespaceDeclaration("oper", OPER_NAMESPACE);
            envelope.addNamespaceDeclaration("data", DATA_NAMESPACE);
            envelope.addNamespaceDeclaration("soapenv", SOAPENV_NAMESPACE);

            // Создаем элемент getOperationHistory
            SOAPElement operElement = body.addChildElement("getOperationHistory", "oper");

            // Создаем OperationHistoryRequest
            SOAPElement dataElement = operElement.addChildElement("OperationHistoryRequest", "data");

            SOAPElement barcode = dataElement.addChildElement("Barcode", "data");
            barcode.addTextNode(cleanNumber);

            SOAPElement messageType = dataElement.addChildElement("MessageType", "data");
            messageType.addTextNode("0");

            SOAPElement language = dataElement.addChildElement("Language", "data");
            language.addTextNode("RUS");

            // Создаем AuthorizationHeader
            SOAPElement dataAuth = operElement.addChildElement("AuthorizationHeader", "data");

            // Добавляем атрибут mustUnderstand как в примере
            SOAPFactory sf = SOAPFactory.newInstance();
            Name must = sf.createName("mustUnderstand", "soapenv", SOAPENV_NAMESPACE);
            dataAuth.addAttribute(must, "1");

            SOAPElement loginElement = dataAuth.addChildElement("login", "data");
            loginElement.addTextNode(login);

            SOAPElement passwordElement = dataAuth.addChildElement("password", "data");
            passwordElement.addTextNode(password);

            // Сохраняем сообщение
            message.saveChanges();

            // Логируем запрос для отладки
            log.debug("SOAP Request: {}", soapMessageToString(message));

            // Отправляем запрос и получаем ответ
            SOAPMessage soapResponse = connection.call(message, TRACKING_API_URL);

            // Логируем ответ
            log.debug("SOAP Response: {}", soapMessageToString(soapResponse));

            // Парсим ответ
            TrackingInfo info = parseSoapResponse(soapResponse, cleanNumber);
            logResponse(cleanNumber, info);

            return info;

        } catch (SOAPException e) {
            log.error("Ошибка SOAP при отслеживании посылки {}", cleanNumber, e);
            return createErrorResponse(cleanNumber, "Ошибка SOAP соединения: " + e.getMessage());
        } catch (Exception e) {
            log.error("Неизвестная ошибка при отслеживании посылки {}", cleanNumber, e);
            return createErrorResponse(cleanNumber, "Внутренняя ошибка сервера: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SOAPException e) {
                    log.error("Ошибка при закрытии SOAP соединения", e);
                }
            }
        }
    }

    /**
     * Конвертация SOAPMessage в строку для логирования
     */
    private String soapMessageToString(SOAPMessage message) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            Source source = message.getSOAPPart().getContent();
            StreamResult result = new StreamResult(sw);
            transformer.transform(source, result);

            return sw.toString();
        } catch (Exception e) {
            return "Error converting SOAP message to string: " + e.getMessage();
        }
    }

    /**
     * Парсинг SOAP ответа
     */
    private TrackingInfo parseSoapResponse(SOAPMessage response, String trackingNumber) {
        try {
            TrackingInfo.TrackingInfoBuilder builder = TrackingInfo.builder()
                    .trackingNumber(trackingNumber)
                    .serviceName(getServiceName())
                    .success(true)
                    .lastCheck(LocalDateTime.now());

            SOAPBody body = response.getSOAPBody();

            // Проверяем наличие SOAP Fault
            if (body.hasFault()) {
                SOAPFault fault = body.getFault();
                String faultString = fault.getFaultString();
                String faultCode = fault.getFaultCode();

                log.error("SOAP Fault: {} - {}", faultCode, faultString);

                if (faultString.contains("Authorization") || faultString.contains("auth")) {
                    return createErrorResponse(
                            trackingNumber, "Ошибка авторизации API Почты России. Проверьте логин и пароль.");
                } else {
                    return createErrorResponse(trackingNumber, "Ошибка API: " + faultString);
                }
            }

            // Получаем все элементы operationHistoryData
            List<Operation> operations = new ArrayList<>();

            // Ищем элементы с ответом
            Iterator<Node> elements = body.getChildElements();
            while (elements.hasNext()) {
                Node node = elements.next();
                if (node instanceof SOAPElement) {
                    SOAPElement element = (SOAPElement) node;
                    parseOperationHistoryResponse(element, operations);
                }
            }

            if (!operations.isEmpty()) {
                // Сортируем по дате (от новых к старым)
                operations.sort((a, b) -> b.getDate().compareTo(a.getDate()));

                Operation lastOp = operations.get(0);
                String statusDesc = getStatusDescription(lastOp.getOperationId());

                builder.status(lastOp.getOperationName())
                        .statusDescription(statusDesc)
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

        } catch (SOAPException e) {
            log.error("Ошибка парсинга SOAP ответа", e);
            return createErrorResponse(trackingNumber, "Ошибка обработки ответа от сервера");
        }
    }

    /**
     * Рекурсивный парсинг ответа operationHistoryResponse
     */
    private void parseOperationHistoryResponse(SOAPElement element, List<Operation> operations) {
        Iterator<Node> children = element.getChildElements();
        while (children.hasNext()) {
            Node child = children.next();
            if (child instanceof SOAPElement) {
                SOAPElement childElement = (SOAPElement) child;
                String localName = childElement.getLocalName();

                if ("OperationHistoryData".equals(localName)) {
                    parseOperationHistoryData(childElement, operations);
                } else {
                    parseOperationHistoryResponse(childElement, operations);
                }
            }
        }
    }

    /**
     * Парсинг OperationHistoryData
     */
    private void parseOperationHistoryData(SOAPElement element, List<Operation> operations) {
        Iterator<Node> records = element.getChildElements();
        while (records.hasNext()) {
            Node record = records.next();
            if (record instanceof SOAPElement) {
                SOAPElement recordElement = (SOAPElement) record;
                if ("historyRecord".equals(recordElement.getLocalName())) {
                    Operation op = parseHistoryRecord(recordElement);
                    if (op != null) {
                        operations.add(op);
                    }
                }
            }
        }
    }

    /**
     * Парсинг отдельной записи historyRecord
     */
    private Operation parseHistoryRecord(SOAPElement recordElement) {
        try {
            Operation.OperationBuilder builder = Operation.builder().serviceName(getServiceName());

            Iterator<Node> parameters = recordElement.getChildElements();
            while (parameters.hasNext()) {
                Node param = parameters.next();
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
                        case "FinanceParameters":
                            parseFinanceParameters(paramElement, builder);
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
     * Парсинг параметров операции
     */
    private void parseOperationParameters(SOAPElement element, Operation.OperationBuilder builder) {
        Iterator<Node> params = element.getChildElements();
        while (params.hasNext()) {
            Node param = params.next();
            if (param instanceof SOAPElement) {
                SOAPElement paramElement = (SOAPElement) param;
                String paramName = paramElement.getLocalName();

                if ("OperType".equals(paramName)) {
                    parseOperType(paramElement, builder);
                } else if ("OperDate".equals(paramName)) {
                    try {
                        String dateStr = paramElement.getValue();
                        if (dateStr != null) {
                            builder.date(parseDate(dateStr));
                        }
                    } catch (Exception e) {
                        log.warn("Ошибка парсинга даты операции", e);
                    }
                }
            }
        }
    }

    /**
     * Парсинг типа операции
     */
    private void parseOperType(SOAPElement element, Operation.OperationBuilder builder) {
        Iterator<Node> operTypeParams = element.getChildElements();
        while (operTypeParams.hasNext()) {
            Node param = operTypeParams.next();
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
    }

    /**
     * Парсинг адресных параметров
     */
    private void parseAddressParameters(SOAPElement element, Operation.OperationBuilder builder) {
        Iterator<Node> params = element.getChildElements();
        String description = null;
        String index = null;

        while (params.hasNext()) {
            Node param = params.next();
            if (param instanceof SOAPElement) {
                SOAPElement paramElement = (SOAPElement) param;
                String paramName = paramElement.getLocalName();

                if ("OperationAddress".equals(paramName)) {
                    Iterator<Node> addrParams = paramElement.getChildElements();
                    while (addrParams.hasNext()) {
                        Node addrParam = addrParams.next();
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
                }
            }
        }

        if (index != null && description != null) {
            builder.operationPlace(index + ", " + description);
            builder.operationIndex(index);
            builder.operationAddress(description);
        } else if (description != null) {
            builder.operationPlace(description);
            builder.operationAddress(description);
        }
    }

    /**
     * Парсинг финансовых параметров
     */
    private void parseFinanceParameters(SOAPElement element, Operation.OperationBuilder builder) {
        Iterator<Node> params = element.getChildElements();
        while (params.hasNext()) {
            Node param = params.next();
            if (param instanceof SOAPElement) {
                SOAPElement paramElement = (SOAPElement) param;
                String paramName = paramElement.getLocalName();

                try {
                    String value = paramElement.getValue();
                    if (value != null) {
                        double amount = Double.parseDouble(value) / 100.0; // Переводим из копеек в рубли

                        if ("Value".equals(paramName)) {
                            builder.declaredValue(amount);
                        } else if ("Payment".equals(paramName)) {
                            builder.cashOnDelivery(amount);
                        } else if ("MassRate".equals(paramName)) {
                            builder.postage(amount);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.debug("Не удалось распарсить финансовый параметр: {}", paramName);
                }
            }
        }
    }

    /**
     * Парсинг параметров отправления
     */
    private void parseItemParameters(SOAPElement element, Operation.OperationBuilder builder) {
        Iterator<Node> params = element.getChildElements();
        while (params.hasNext()) {
            Node param = params.next();
            if (param instanceof SOAPElement) {
                SOAPElement paramElement = (SOAPElement) param;
                String paramName = paramElement.getLocalName();

                if ("Mass".equals(paramName)) {
                    try {
                        String massStr = paramElement.getValue();
                        if (massStr != null) {
                            builder.weight(Integer.parseInt(massStr));
                        }
                    } catch (NumberFormatException e) {
                        log.debug("Не удалось распарсить вес");
                    }
                } else if ("MailType".equals(paramName)) {
                    parseMailType(paramElement, builder);
                } else if ("MailCtg".equals(paramName)) {
                    parseMailCategory(paramElement, builder);
                }
            }
        }
    }

    /**
     * Парсинг типа отправления
     */
    private void parseMailType(SOAPElement element, Operation.OperationBuilder builder) {
        Iterator<Node> typeParams = element.getChildElements();
        while (typeParams.hasNext()) {
            Node param = typeParams.next();
            if (param instanceof SOAPElement) {
                SOAPElement paramElement = (SOAPElement) param;
                if ("Name".equals(paramElement.getLocalName())) {
                    builder.mailType(paramElement.getValue());
                }
            }
        }
    }

    /**
     * Парсинг категории отправления
     */
    private void parseMailCategory(SOAPElement element, Operation.OperationBuilder builder) {
        Iterator<Node> categoryParams = element.getChildElements();
        while (categoryParams.hasNext()) {
            Node param = categoryParams.next();
            if (param instanceof SOAPElement) {
                SOAPElement paramElement = (SOAPElement) param;
                if ("Name".equals(paramElement.getLocalName())) {
                    builder.mailCategory(paramElement.getValue());
                }
            }
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        try {
            // Формат: 2024-01-15T14:30:00.000+03:00
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Ошибка парсинга даты: {}", dateStr);
            return LocalDateTime.now();
        }
    }

    private boolean isDelivered(Operation lastOp) {
        if (lastOp == null || lastOp.getOperationName() == null) return false;

        String opName = lastOp.getOperationName().toLowerCase();
        return opName.contains("вручено")
                || opName.contains("получено")
                || opName.contains("доставлено")
                || "5".equals(lastOp.getOperationId());
    }

    @Override
    protected Map<String, String> getStatusMapping() {
        return STATUS_MAP;
    }
}
