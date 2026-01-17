package ru.otus.crm.service;

import ru.otus.cachehw.HwCache;
import ru.otus.cachehw.MyCache;
import ru.otus.core.repository.DataTemplate;
import ru.otus.core.sessionmanager.TransactionRunner;
import ru.otus.crm.model.Client;

public class DbServiceClientFactory {
    public static DBServiceClient createWithCache(
            TransactionRunner transactionRunner, DataTemplate<Client> dataTemplate) {
        HwCache<Long, Client> cache = new MyCache<>();
        return new DbServiceClientImpl(transactionRunner, dataTemplate, cache);
    }

    public static DBServiceClient createWithoutCache(
            TransactionRunner transactionRunner, DataTemplate<Client> dataTemplate) {
        return new DbServiceClientImpl(transactionRunner, dataTemplate);
    }
}
