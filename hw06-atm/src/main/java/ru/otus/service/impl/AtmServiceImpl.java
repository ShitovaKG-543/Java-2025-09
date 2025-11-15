package ru.otus.service.impl;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ru.otus.exception.AtmError;
import ru.otus.exception.AtmException;
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
        } catch (AtmException e) {
            log.error("Возникла ошибка во время пополнения: ", e);
        }
    }

    @Override
    public Map<Banknote, Integer> withdraw(int amount) {

        Atm atm = Atm.getInstance();

        if (amount <= 0) {
            String errorMessage = "Сумма для снятия должна быть положительной";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        if (amount > atm.getTotalBalance()) {
            log.error(String.format(AtmError.INSUFFICIENT_FUNDS_ERROR.getMessege(), amount, atm.getTotalBalance()));
            throw new AtmException(AtmError.INSUFFICIENT_FUNDS_ERROR, amount, atm.getTotalBalance());
        }

        Map<Banknote, Integer> result = atm.getAmount(amount);

        return result;
    }
}
