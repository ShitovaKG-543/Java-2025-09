package ru.otus.controller;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.otus.crm.dto.ClientDto;
import ru.otus.crm.mapper.ClientMapper;
import ru.otus.crm.model.Client;
import ru.otus.crm.service.DbServiceClient;

@Slf4j
@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientsController {

    private final DbServiceClient dbServiceClient;
    private final ClientMapper clientMapper;

    @GetMapping
    public String clientsList(Model model) {
        log.info("GET /clients - получение списка клиентов");

        List<ClientDto> clients =
                dbServiceClient.findAll().stream().map(clientMapper::toDto).collect(Collectors.toList());

        model.addAttribute("clients", clients);
        return "clients";
    }

    @GetMapping("/new")
    public String newClientForm(Model model) {
        log.info("GET /clients/new - форма создания клиента");

        model.addAttribute("clientDto", ClientDto.builder().build());
        return "client-form";
    }

    @PostMapping
    public String saveClient(@ModelAttribute ClientDto clientDto) {
        log.info("POST /clients - сохранение клиента: {}", clientDto.getName());

        Client client = clientMapper.toEntity(clientDto);
        dbServiceClient.saveClient(client);
        return "redirect:/clients";
    }
}
