package ru.otus.trackingbot.service;

import ru.otus.trackingbot.model.TrackingInfo;

public interface TrackingService {
    /**
     * Отследить посылку по трек-номеру
     * @param trackNumber трек-номер посылки
     * @return информация о посылке
     */
    TrackingInfo track(String trackNumber);

    /**
     * Получить название службы доставки
     * @return название службы
     */
    String getServiceName();

    /**
     * Проверить, может ли сервис обработать данный трек-номер
     * @param trackNumber трек-номер
     * @return true если может
     */
    boolean canHandle(String trackNumber);
}
