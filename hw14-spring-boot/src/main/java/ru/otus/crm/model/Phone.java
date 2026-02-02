package ru.otus.crm.model;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Builder
@Table("phone")
public record Phone(@Id @Column("id") Long id, @Column("number") String number, @Column("client_id") Long clientId) {}
