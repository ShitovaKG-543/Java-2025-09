package ru.otus.exception;

import lombok.Getter;

@Getter
public enum AtmError {
    INSUFFICIENT_FUNDS_ERROR("error-01", "Недостаточно средств для выдачи суммы %d руб. Всего в банкомате %d руб"),
    UNACCEPTABLE_BANKNOTE_ERROR("error-02", "Внесена недопустимая банкнота номиналом %s руб"),
    IMPOSSIBLE_ISSUED_ERROR("error-03", "Невозможно выдать запрошенную сумму доступными банкнотами. Остаток: %d руб");

    private final String codeError;
    private final String messege;

    AtmError(String codeError, String messege) {
        this.codeError = codeError;
        this.messege = messege;
    }
}
