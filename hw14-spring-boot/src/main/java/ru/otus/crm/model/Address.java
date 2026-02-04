package ru.otus.crm.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Builder
@Table("address")
public record Address(@Id @Column("id") Long id, @Column("street") String street, @Column("client_id") Long clientId) {}
