package ru.otus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.security.LoginService;
import org.hibernate.cfg.Configuration;
import ru.otus.core.repository.DataTemplateHibernate;
import ru.otus.core.repository.HibernateUtils;
import ru.otus.core.sessionmanager.TransactionManagerHibernate;
import ru.otus.crm.dbmigrations.MigrationsExecutorFlyway;
import ru.otus.crm.mapper.ClientMapper;
import ru.otus.crm.model.Address;
import ru.otus.crm.model.Client;
import ru.otus.crm.model.Phone;
import ru.otus.crm.model.User;
import ru.otus.crm.service.DatabaseLoginService;
import ru.otus.crm.service.DbServiceClientImpl;
import ru.otus.crm.service.DbServiceUserImpl;
import ru.otus.server.ClientsWebServer;
import ru.otus.server.ClientsWebServerWithBasicSecurity;
import ru.otus.services.TemplateProcessor;
import ru.otus.services.TemplateProcessorImpl;

/*
    Полезные для демо ссылки

    // Стартовая страница
    http://localhost:8080

    // Страница клиентов
    http://localhost:8080/clients

    // Страница добавления клиента
    http://localhost:8080/clients/new

*/
public class WebServerWithBasicSecurityDemo {

    public static final String HIBERNATE_CFG_FILE = "hibernate.cfg.xml";
    private static final int WEB_SERVER_PORT = 8080;
    private static final String TEMPLATES_DIR = "/templates/";

    public static void main(String[] args) throws Exception {
        var configuration = new Configuration().configure(HIBERNATE_CFG_FILE);

        var dbUrl = configuration.getProperty("hibernate.connection.url");
        var dbUserName = configuration.getProperty("hibernate.connection.username");
        var dbPassword = configuration.getProperty("hibernate.connection.password");

        new MigrationsExecutorFlyway(dbUrl, dbUserName, dbPassword).executeMigrations();

        var sessionFactory =
                HibernateUtils.buildSessionFactory(configuration, User.class, Client.class, Address.class, Phone.class);

        var transactionManager = new TransactionManagerHibernate(sessionFactory);

        var clientTemplate = new DataTemplateHibernate<>(Client.class);
        var userTemplate = new DataTemplateHibernate<>(User.class);

        var dbServiceClient = new DbServiceClientImpl(transactionManager, clientTemplate);
        var dbServiceUser = new DbServiceUserImpl(transactionManager, userTemplate);

        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
        ClientMapper clientMapper = new ClientMapper();
        TemplateProcessor templateProcessor = new TemplateProcessorImpl(TEMPLATES_DIR);

        LoginService loginService = new DatabaseLoginService(dbServiceUser);

        ClientsWebServer usersWebServer = new ClientsWebServerWithBasicSecurity(
                WEB_SERVER_PORT, loginService, dbServiceClient, gson, clientMapper, templateProcessor);

        usersWebServer.start();
        usersWebServer.join();
    }
}
