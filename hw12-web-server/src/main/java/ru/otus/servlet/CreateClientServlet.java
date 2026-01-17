package ru.otus.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import ru.otus.services.TemplateProcessor;

@SuppressWarnings({"java:S1989"})
public class CreateClientServlet extends HttpServlet {

    private static final String CREATE_CLIENT_PAGE_TEMPLATE = "createClient.html";

    private final transient TemplateProcessor templateProcessor;

    public CreateClientServlet(TemplateProcessor templateProcessor) {
        this.templateProcessor = templateProcessor;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
        showCreateForm(response);
    }

    private void showCreateForm(HttpServletResponse response) throws IOException {
        Map<String, Object> paramsMap = new HashMap<>();

        response.setContentType("text/html");
        response.getWriter().println(templateProcessor.getPage(CREATE_CLIENT_PAGE_TEMPLATE, paramsMap));
    }
}
