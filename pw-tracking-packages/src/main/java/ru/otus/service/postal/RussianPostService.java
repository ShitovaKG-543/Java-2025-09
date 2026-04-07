package ru.otus.service.postal;

public class RussianPostService implements PostalService {

    @Override
    public String getServiceName() {
        return "RussianPost";
    }

    @Override
    public String getTrackingStatus(String trackingNumber) throws Exception {
        // SOAP запрос к Russian Post API
        String soapRequest = buildSoapRequest(trackingNumber);
        String response = sendSoapRequest(soapRequest);
        return parseStatus(response);
    }

    private String buildSoapRequest(String trackingNumber) {
        return """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                <soap:Header/>
                <soap:Body>
                    <getOperationHistory xmlns="http://russianpost.org/operationhistory/data">
                        <OperationHistoryRequest>
                            <Barcode>%s</Barcode>
                            <MessageType>0</MessageType>
                            <Language>RUS</Language>
                        </OperationHistoryRequest>
                    </getOperationHistory>
                </soap:Body>
            </soap:Envelope>
        """.formatted(trackingNumber);
    }

    private String sendSoapRequest(String soapRequest) throws Exception {
        // Реальная реализация с HTTP клиентом
        // Здесь упрощённый пример
        return "<response><status>В пути</status></response>";
    }

    private String parseStatus(String soapResponse) {
        // Парсинг SOAP ответа
        return "В пути (Москва)";
    }

    @Override
    public boolean isValidTrackingNumber(String trackingNumber) {
        return trackingNumber != null && trackingNumber.matches("\\d{14}");
    }
}