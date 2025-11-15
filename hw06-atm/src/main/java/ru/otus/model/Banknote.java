package ru.otus.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Banknote {
    private final NominalValue nominalValue;

    public int getNominalAmount() {
        return nominalValue.getValue();
    }
}
