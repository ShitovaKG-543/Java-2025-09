package ru.otus.crm.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.cachehw.HwCache;
import ru.otus.cachehw.HwListener;
import ru.otus.core.repository.DataTemplate;
import ru.otus.core.sessionmanager.TransactionRunner;
import ru.otus.crm.model.Client;

public class DbServiceClientImpl implements DBServiceClient {
    private static final Logger log = LoggerFactory.getLogger(DbServiceClientImpl.class);

    private final DataTemplate<Client> dataTemplate;
    private final TransactionRunner transactionRunner;
    private final HwCache<Long, Client> cache;
    private final boolean cacheEnabled;

    // Конструктор без кэша
    public DbServiceClientImpl(TransactionRunner transactionRunner, DataTemplate<Client> dataTemplate) {
        this.transactionRunner = transactionRunner;
        this.dataTemplate = dataTemplate;
        this.cache = null;
        this.cacheEnabled = false;
        log.info("Создан DbServiceClientImpl без кэша");
    }

    // Конструктор с кэшем
    public DbServiceClientImpl(
            TransactionRunner transactionRunner, DataTemplate<Client> dataTemplate, HwCache<Long, Client> cache) {
        this.transactionRunner = transactionRunner;
        this.dataTemplate = dataTemplate;
        this.cache = cache;
        this.cacheEnabled = cache != null;

        if (cacheEnabled) {
            setupCacheListener();
            log.info("Создан DbServiceClientImpl с кэшем");
        } else {
            log.info("Создан DbServiceClientImpl без кэша");
        }
    }

    @Override
    public Client saveClient(Client client) {
        Client savedClient = transactionRunner.doInTransaction(connection -> {
            if (client.getId() == null) {
                // Вставка нового клиента
                var clientId = dataTemplate.insert(connection, client);
                Client createdClient = new Client(clientId, client.getName());
                log.info("Создан клиент: {}", createdClient);
                return createdClient;
            } else {
                // Обновление существующего клиента
                dataTemplate.update(connection, client);
                log.info("Обновлен клиент: {}", client);
                return client;
            }
        });

        // Обновляем кэш после сохранения
        if (cacheEnabled && savedClient.getId() != null) {
            cache.put(savedClient.getId(), savedClient);
            log.debug("Кэш обновлен для клиента с id: {}", savedClient.getId());
        }

        return savedClient;
    }

    @Override
    public Optional<Client> getClient(long id) {
        // 1. Пытаемся получить из кэша (если включен)
        if (cacheEnabled) {
            Client cachedClient = cache.get(id);
            if (cachedClient != null) {
                log.info("Извлеченный клиент из кэша, id: {}", id);
                return Optional.of(cachedClient);
            }
            log.debug("Клиент не найден в кеш, id: {}", id);
        }

        // 2. Если нет в кэше или кэш отключен, идем в БД
        Optional<Client> clientOptional =
                transactionRunner.doInTransaction(connection -> dataTemplate.findById(connection, id));

        // 3. Сохраняем в кэш если нашли
        if (cacheEnabled && clientOptional.isPresent()) {
            Client client = clientOptional.get();
            cache.put(id, client);
            log.debug("Добавлен в кэш клиент с id: {}", id);
        }

        log.info("client: {}", clientOptional);
        return clientOptional;
    }

    @Override
    public List<Client> findAll() {
        List<Client> clientList = transactionRunner.doInTransaction(dataTemplate::findAll);

        // Добавляем всех найденных клиентов в кэш
        if (cacheEnabled) {
            for (Client client : clientList) {
                if (client.getId() != null) {
                    cache.put(client.getId(), client);
                }
            }
            log.debug("Добавлено {} клиентов в кеш методом findAll", clientList.size());
        }

        log.info("Список клиентов: {}", clientList);
        return clientList;
    }

    // Настройка слушателя для логирования операций с кэшем
    private void setupCacheListener() {
        HwListener<Long, Client> cacheListener = (key, value, action) -> {
            switch (action) {
                case "PUT":
                    log.debug("Cache PUT - Key: {}, Value: {}", key, value);
                    break;
                case "GET":
                    log.debug("Cache GET - Key: {}, Value: {}", key, value);
                    break;
                case "REMOVE":
                    log.debug("Cache REMOVE - Key: {}", key);
                    break;
                default:
                    log.debug("Cache {} - Key: {}, Value: {}", action, key, value);
            }
        };

        cache.addListener(cacheListener);
        log.debug("Настройка прослушивателя кэша завершена");
    }
}
