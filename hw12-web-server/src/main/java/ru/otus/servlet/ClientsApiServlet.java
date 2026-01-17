package ru.otus.servlet;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.crm.dto.ClientDto;
import ru.otus.crm.dto.CreateClientRequest;
import ru.otus.crm.mapper.ClientMapper;
import ru.otus.crm.model.Client;
import ru.otus.crm.service.DBServiceClient;

@SuppressWarnings({"java:S1989"})
public class ClientsApiServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ClientsApiServlet.class);

    private final transient DBServiceClient dbServiceClient;
    private final Gson gson;
    private final ClientMapper clientMapper;

    public ClientsApiServlet(DBServiceClient dbServiceClient, Gson gson, ClientMapper clientMapper) {
        this.dbServiceClient = dbServiceClient;
        this.gson = gson;
        this.clientMapper = clientMapper;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException {
        // POST /api/clients - создать нового клиента
        createClient(req, response);
    }

    private void createClient(HttpServletRequest req, HttpServletResponse response) throws IOException {
        try {
            CreateClientRequest request = getRequestFromJson(req);

            // Валидация
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Имя клиента обязательно");
                return;
            }

            Client client = clientMapper.requestToClient(request);

            // Сохраняем в БД
            Client savedClient = dbServiceClient.saveClient(client);
            mapResponse(response, savedClient);

        } catch (Exception e) {
            log.error(e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Ошибка при создании клиента");
        }
    }

    private CreateClientRequest getRequestFromJson(HttpServletRequest req) throws IOException {
        // Читаем JSON из тела запроса
        BufferedReader reader = req.getReader();
        String jsonBody = reader.lines().collect(Collectors.joining());

        // Парсим JSON в объект ClientDto
        return gson.fromJson(jsonBody, CreateClientRequest.class);
    }

    private void mapResponse(HttpServletResponse response, Client savedClient) throws IOException {
        // Возвращаем созданного клиента
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_CREATED);

        ClientDto dto = clientMapper.clientToDto(savedClient);
        response.getWriter().write(gson.toJson(dto));
    }
}
