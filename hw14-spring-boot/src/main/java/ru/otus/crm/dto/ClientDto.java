package ru.otus.crm.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ClientDto {
    private Long id;
    private String name;
    private String street;
    private List<String> phones;
}
