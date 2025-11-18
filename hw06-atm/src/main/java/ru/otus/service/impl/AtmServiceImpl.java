package ru.otus.service.impl;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ru.otus.exception.ImpossibleIssuedAtmException;
import ru.otus.exception.InsufficientFundsAtmException;
import ru.otus.model.Atm;
import ru.otus.model.Banknote;
import ru.otus.service.AtmService;

@Slf4j
public class AtmServiceImpl implements AtmService {

    @Override
    public void initializeAtm(List<Banknote> supportedBanknotes) {
        Atm atm = Atm.getInstance();
        atm.initialize(supportedBanknotes);
    }

    @Override
    public String getBalanceInfo() {
        Atm atm = Atm.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("Общий баланс: ").append(atm.getTotalBalance()).append(" руб.\n");
        sb.append("Детали по ячейкам:\n");

        Map<Banknote, Integer> balanceByBanknotes = atm.getBalanceByBanknotes();

        balanceByBanknotes.keySet().stream()
                .sorted((b1, b2) -> Integer.compare(b2.getNominalAmount(), b1.getNominalAmount()))
                .forEach(banknote -> sb.append(String.format(
                                "В ячейке с номиралом %d руб содержится %d руб",
                                banknote.getNominalAmount(), balanceByBanknotes.get(banknote)))
                        .append("\n"));

        return sb.toString();
    }

    @Override
    public void deposit(Map<Banknote, Integer> banknotes) {

        Atm atm = Atm.getInstance();
        try {
            atm.addBanknotes(banknotes);
            log.info("Банкноты успешно добавлены в ячейки");
        } catch (RuntimeException e) {
            log.error("Возникла ошибка во время пополнения: ", e);
        }
    }

    @Override
    public Map<Banknote, Integer> withdraw(int amount) {
        log.info("Запрошено к выдаче {} руб", amount);

        Atm atm = Atm.getInstance();

        if (amount <= 0) {
            String errorMessage = "Сумма для снятия должна быть положительной";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        if (amount > atm.getTotalBalance()) {
            RuntimeException exception = new InsufficientFundsAtmException(amount, atm.getTotalBalance());
            log.error(exception.getMessage());
            throw exception;
        }
        try {
            Map<Banknote, Integer> result = atm.getAmount(amount);
            logWithdrawAmount(result);
            return result;
        } catch (ImpossibleIssuedAtmException e) {
            // Сообщим клиенту о невозможности выполнения операции
            System.out.println(e.getMessage());
            return null;
        }
    }

    private void logWithdrawAmount(Map<Banknote, Integer> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("Детали по выдаче:\n");

        map.keySet().stream()
                .sorted((b1, b2) -> Integer.compare(b2.getNominalAmount(), b1.getNominalAmount()))
                .forEach(banknote -> sb.append(String.format(
                                "Выдано купюр с номиралом %d руб %d штук, что составляет %d руб",
                                banknote.getNominalAmount(),
                                map.get(banknote),
                                banknote.getNominalAmount() * map.get(banknote)))
                        .append("\n"));

        log.info(sb.toString());
    }
}
