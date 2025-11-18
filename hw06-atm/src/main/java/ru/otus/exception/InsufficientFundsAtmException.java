package ru.otus.exception;

public class InsufficientFundsAtmException extends RuntimeException {

    public InsufficientFundsAtmException(int requestedAmount, int totalBalance) {
        super("Недостаточно средств для выдачи суммы %d руб. Всего в банкомате %d руб"
                .formatted(requestedAmount, totalBalance));
    }
}
