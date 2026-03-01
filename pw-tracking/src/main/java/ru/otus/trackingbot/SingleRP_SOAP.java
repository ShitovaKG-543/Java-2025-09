package ru.otus.trackingbot;

import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

public class SingleRP_SOAP {

    /*Данный код создает запрос для получения информации о
    конкретном отправлении по Идентификатору отправления (barcode).
    Ответ на запрос выводится на экран в формате xml.
    Пример запроса:
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:oper="http://russianpost.org/operationhistory"
                       xmlns:data="http://russianpost.org/operationhistory/data"
                       xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
           <soap:Header/>
           <soap:Body>
              <oper:getOperationHistory>
                 <data:OperationHistoryRequest>
                    <data:Barcode>RA644000001RU</data:Barcode>
                    <data:MessageType>0</data:MessageType>
                    <data:Language>RUS</data:Language>
                 </data:OperationHistoryRequest>
                 <data:AuthorizationHeader soapenv:mustUnderstand="1">
                    <data:login>myLogin</data:login>
                    <data:password>myPassword</data:password>
                 </data:AuthorizationHeader>
              </oper:getOperationHistory>
           </soap:Body>
        </soap:Envelope>
    */

    public static void main(String args[]) throws Exception {

        // Cоздаем соединение
        SOAPConnectionFactory soapConnFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection connection = soapConnFactory.createConnection();
        String url = "https://tracking.russianpost.ru/rtm34";

        // Cоздаем сообщение
        MessageFactory messageFactory = MessageFactory.newInstance("SOAP 1.2 Protocol");
        SOAPMessage message = messageFactory.createMessage();

        // Создаем объекты, представляющие различные компоненты сообщения
        SOAPPart soapPart = message.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody body = envelope.getBody();
        envelope.addNamespaceDeclaration("soap", "http://www.w3.org/2003/05/soap-envelope");
        envelope.addNamespaceDeclaration("oper", "http://russianpost.org/operationhistory");
        envelope.addNamespaceDeclaration("data", "http://russianpost.org/operationhistory/data");
        envelope.addNamespaceDeclaration("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        SOAPElement operElement = body.addChildElement("getOperationHistory", "oper");
        SOAPElement dataElement = operElement.addChildElement("OperationHistoryRequest", "data");
        SOAPElement barcode = dataElement.addChildElement("Barcode", "data");
        SOAPElement messageType = dataElement.addChildElement("MessageType", "data");
        SOAPElement language = dataElement.addChildElement("Language", "data");
        SOAPElement dataAuth = operElement.addChildElement("AuthorizationHeader", "data");
        SOAPFactory sf = SOAPFactory.newInstance();
        Name must = sf.createName("mustUnderstand", "soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        dataAuth.addAttribute(must, "1");
        SOAPElement login = dataAuth.addChildElement("login", "data");
        SOAPElement password = dataAuth.addChildElement("password", "data");

        // Заполняем значения
        barcode.addTextNode("LC577151513CN");
        messageType.addTextNode("0");
        language.addTextNode("RUS");
        login.addTextNode("aIwSCRXlriXQpb");
        password.addTextNode("9TUUc1mG1qDc");

        // Сохранение сообщения
        message.saveChanges();

        // Отправляем запрос и выводим ответ на экран
        SOAPMessage soapResponse = connection.call(message, url);
        Source sourceContent = soapResponse.getSOAPPart().getContent();
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(System.out);
        t.transform(sourceContent, result);

        // Закрываем соединение
        connection.close();
    }
}
