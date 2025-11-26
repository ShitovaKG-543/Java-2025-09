package ru.otus.exception;

public class ImpossibleIssuedAtmException extends RuntimeException {

    public ImpossibleIssuedAtmException(int remains) {
        super("Невозможно выдать запрошенную сумму доступными банкнотами. Остаток: %d руб".formatted(remains));
    }
}
