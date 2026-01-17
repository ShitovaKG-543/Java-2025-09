package ru.otus.crm.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClientRequest {
    private String name;
    private String street;
    private List<String> phones;
}
