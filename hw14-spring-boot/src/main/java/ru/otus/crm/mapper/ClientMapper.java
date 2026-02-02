package ru.otus.crm.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.otus.crm.dto.ClientDto;
import ru.otus.crm.model.Address;
import ru.otus.crm.model.Client;
import ru.otus.crm.model.Phone;

@Component
public class ClientMapper {

    public ClientDto toDto(Client client) {
        if (client == null) {
            return null;
        }

        ClientDto dto = ClientDto.builder().build();
        dto.setId(client.id());
        dto.setName(client.name());

        if (client.address() != null) {
            dto.setStreet(client.address().street());
        }

        if (client.phones() != null) {
            List<String> phoneNumbers = new ArrayList<>();
            for (Phone phone : client.phones()) {
                phoneNumbers.add(phone.number());
            }
            dto.setPhones(phoneNumbers);
        } else {
            dto.setPhones(new ArrayList<>());
        }

        return dto;
    }

    public Client toEntity(ClientDto dto) {
        if (dto == null) {
            return null;
        }

        Client.ClientBuilder clientBuilder = Client.builder().id(dto.getId()).name(dto.getName());

        mapAddress(clientBuilder, dto.getStreet());
        mapPhones(clientBuilder, dto.getPhones());

        return clientBuilder.build();
    }

    private void mapAddress(Client.ClientBuilder clientBuilder, String street) {
        // Создаем адрес, если указана улица
        if (street != null && !street.trim().isEmpty()) {
            Address address = Address.builder().street(street.trim()).build();
            clientBuilder.address(address);
        }
    }

    private void mapPhones(Client.ClientBuilder clientBuilder, List<String> phones) {
        // Добавляем телефоны
        if (phones != null) {
            Set<Phone> phonesEntities = new HashSet<>();
            for (String phoneNumber : phones) {
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    Phone phone = Phone.builder().number(phoneNumber.trim()).build();
                    phonesEntities.add(phone);
                }
            }
            if (!phonesEntities.isEmpty()) {
                clientBuilder.phones(phonesEntities);
            }
        }
    }
}
