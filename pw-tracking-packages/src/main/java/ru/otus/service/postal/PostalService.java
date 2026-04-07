package ru.otus.service.postal;

public interface PostalService {
    String getServiceName();
    String getTrackingStatus(String trackingNumber) throws Exception;
    boolean isValidTrackingNumber(String trackingNumber);
}
