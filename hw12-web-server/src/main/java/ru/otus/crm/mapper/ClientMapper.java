package ru.otus.crm.mapper;

import java.util.List;
import java.util.stream.Collectors;
import ru.otus.crm.dto.ClientDto;
import ru.otus.crm.dto.CreateClientRequest;
import ru.otus.crm.model.Address;
import ru.otus.crm.model.Client;
import ru.otus.crm.model.Phone;

public class ClientMapper {

    public Client requestToClient(CreateClientRequest request) {
        // Создаем объект Client из запроса
        Client client = new Client(request.getName().trim());
        mapAddress(client, request.getStreet());
        mapPhones(client, request.getPhones());
        return client;
    }

    public ClientDto clientToDto(Client client) {
        String address = (client.getAddress() != null) ? client.getAddress().getStreet() : null;

        List<String> phones = null;
        if (client.getPhones() != null) {
            phones = client.getPhones().stream().map(Phone::getNumber).collect(Collectors.toList());
        }

        return new ClientDto(client.getId(), client.getName(), address, phones);
    }

    private void mapPhones(Client client, List<String> phones) {
        // Добавляем телефоны если есть
        if (phones != null) {
            for (String phoneNumber : phones) {
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    Phone phone = new Phone(phoneNumber.trim());
                    client.addPhone(phone);
                }
            }
        }
    }

    private void mapAddress(Client client, String street) {
        // Добавляем адрес если есть
        if (street != null && !street.trim().isEmpty()) {
            Address address = new Address(street.trim());
            client.setAddress(address);
        }
    }
}
