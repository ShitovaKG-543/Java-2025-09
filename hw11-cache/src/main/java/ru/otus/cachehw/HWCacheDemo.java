package ru.otus.cachehw;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.core.repository.executor.DbExecutorImpl;
import ru.otus.core.sessionmanager.TransactionRunnerJdbc;
import ru.otus.crm.datasource.DriverManagerDataSource;
import ru.otus.crm.model.Client;
import ru.otus.crm.repository.ClientDataTemplateJdbc;
import ru.otus.crm.service.DBServiceClient;
import ru.otus.crm.service.DbServiceClientFactory;

public class HWCacheDemo {

    private static final String URL = "jdbc:postgresql://localhost:5430/demoDB";
    private static final String USER = "usr";
    private static final String PASSWORD = "pwd";

    private static final Logger log = LoggerFactory.getLogger(HWCacheDemo.class);

    public static void main(String[] args) {
        new HWCacheDemo().demo();
    }

    private void demo() {

        var dataSource = new DriverManagerDataSource(URL, USER, PASSWORD);
        flywayMigrations(dataSource);
        TransactionRunnerJdbc transactionRunner = new TransactionRunnerJdbc(dataSource);

        log.info("\n=== ПОДГОТОВКА ТЕСТОВЫХ ДАННЫХ ===");

        long timeWithCache = testWithCache(transactionRunner);

        long timeWithoutCache = testWithoutCache(transactionRunner);

        log.info(
                "\nС кешем работа выполнялась в течение {} мс.\nБез кеша работа выполнялась в течение {} мс.\nРазница составляет {} мс",
                timeWithCache,
                timeWithoutCache,
                timeWithoutCache - timeWithCache);
    }

    private long testWithCache(TransactionRunnerJdbc transactionRunner) {
        var dbExecutor = new DbExecutorImpl();
        var clientTemplate = new ClientDataTemplateJdbc(dbExecutor);

        DBServiceClient serviceWithCache = DbServiceClientFactory.createWithCache(transactionRunner, clientTemplate);
        List<Long> clientIds = prepareTestData(transactionRunner, serviceWithCache, 100);
        log.info("Создан сервис с кэшем");
        long timeStart = System.currentTimeMillis();
        List<Client> clients = serviceWithCache.findAll();
        for (long id : clientIds) {
            serviceWithCache.getClient(id);
        }
        long timeFinish = System.currentTimeMillis();

        return timeFinish - timeStart;
    }

    private long testWithoutCache(TransactionRunnerJdbc transactionRunner) {
        var dbExecutor = new DbExecutorImpl();
        var clientTemplate = new ClientDataTemplateJdbc(dbExecutor);
        DBServiceClient serviceWithoutCache =
                DbServiceClientFactory.createWithoutCache(transactionRunner, clientTemplate);
        List<Long> clientIds = prepareTestData(transactionRunner, serviceWithoutCache, 100);
        log.info("Создан сервис без кэша");

        long timeStart = System.currentTimeMillis();
        List<Client> clients = serviceWithoutCache.findAll();
        for (long id : clientIds) {
            serviceWithoutCache.getClient(id);
        }
        long timeFinish = System.currentTimeMillis();

        return timeFinish - timeStart;
    }

    private List<Long> prepareTestData(TransactionRunnerJdbc transactionRunner, DBServiceClient service, int count) {
        log.info("Создание {} тестовых клиентов...", count);

        List<Long> clientIds = new ArrayList<>();

        // Очистка старых данных
        clearExistingData(transactionRunner);

        // Создание новых клиентов
        for (int i = 1; i <= count; i++) {
            Client client = new Client("Test Client " + i);
            Client saved = service.saveClient(client);
            clientIds.add(saved.getId());
        }
        log.info("Создано {} клиентов", count);
        log.info("Тестовые данные подготовлены");

        return clientIds;
    }

    private void clearExistingData(TransactionRunnerJdbc transactionRunner) {
        transactionRunner.doInTransaction(connection -> {
            try (var stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM client");
                log.info("Старые данные удалены");
                return null;
            } catch (SQLException e) {
                throw new RuntimeException("Ошибка при очистке данных", e);
            }
        });
    }

    private static void flywayMigrations(DataSource dataSource) {
        log.info("db migration started...");
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:/db/migration")
                .load();
        flyway.migrate();
        log.info("db migration finished.");
        log.info("***");
    }
}
