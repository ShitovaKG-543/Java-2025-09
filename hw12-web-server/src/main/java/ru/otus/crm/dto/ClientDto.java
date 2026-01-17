package ru.otus.crm.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ClientDto {
    private Long id;
    private String name;
    private String street;
    private List<String> phones;

    //    public Client toClient() {
    //        Client client = new Client(name != null ? name.trim() : "");
    //
    //        if (street != null && !street.trim().isEmpty()) {
    //            ru.otus.crm.model.Address address = new ru.otus.crm.model.Address(street.trim());
    //            client.setAddress(address);
    //            address.setClient(client);
    //        }
    //
    //        if (phones != null) {
    //            for (String phoneNumber : phones) {
    //                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
    //                    ru.otus.crm.model.Phone phone = new ru.otus.crm.model.Phone(phoneNumber.trim());
    //                    client.addPhone(phone);
    //                }
    //            }
    //        }
    //
    //        return client;
    //    }
}
