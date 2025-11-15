package ru.otus.model;

import lombok.Getter;

@Getter
public enum NominalValue {
    HUNDRED(100),
    TWO_HUNDRED(200),
    FIVE_HUNDRED(500),
    THOUSAND(1000),
    TWO_THOUSAND(2000),
    FIVE_THOUSAND(5000);

    private final int value;

    NominalValue(int value) {
        this.value = value;
    }
}
