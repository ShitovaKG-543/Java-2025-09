package ru.otus.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Banknote {
    private final NominalValue nominalValue;

    public int getNominalAmount() {
        return nominalValue.getValue();
    }
}
