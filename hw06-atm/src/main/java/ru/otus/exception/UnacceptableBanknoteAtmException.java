package ru.otus.exception;

public class UnacceptableBanknoteAtmException extends RuntimeException {

    public UnacceptableBanknoteAtmException(String nominalValue) {
        super("Внесена недопустимая банкнота номиналом %s руб".formatted(nominalValue));
    }
}
