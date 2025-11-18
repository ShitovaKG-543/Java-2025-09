package ru.otus;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import ru.otus.model.Banknote;
import ru.otus.model.NominalValue;
import ru.otus.service.AtmService;
import ru.otus.service.impl.AtmServiceImpl;

@Slf4j
public class AtmDemo {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        AtmService atmService = new AtmServiceImpl();
        List<Banknote> banknotes = List.of(
                new Banknote(NominalValue.HUNDRED),
                new Banknote(NominalValue.FIVE_HUNDRED),
                new Banknote(NominalValue.THOUSAND));
        atmService.initializeAtm(banknotes);
        String balanceInfo = atmService.getBalanceInfo();
        log.info(balanceInfo);

        Map<Banknote, Integer> banknoteAndCount = new HashMap<>();
        banknoteAndCount.put(new Banknote(NominalValue.HUNDRED), 5);
        banknoteAndCount.put(new Banknote(NominalValue.FIVE_HUNDRED), 10);
        banknoteAndCount.put(new Banknote(NominalValue.THOUSAND), 3);
        banknoteAndCount.put(new Banknote(NominalValue.TWO_HUNDRED), 2);
        banknoteAndCount.put(new Banknote(NominalValue.FIVE_THOUSAND), 1);

        atmService.deposit(banknoteAndCount);
        balanceInfo = atmService.getBalanceInfo();
        log.info(balanceInfo);

        Map<Banknote, Integer> banknoteIssued = atmService.withdraw(1700);
        balanceInfo = atmService.getBalanceInfo();
        log.info(balanceInfo);

        Map<Banknote, Integer> banknoteIssued2 = atmService.withdraw(4700);
        balanceInfo = atmService.getBalanceInfo();
        log.info(balanceInfo);

        Map<Banknote, Integer> banknoteIssued3 = atmService.withdraw(1700);
        balanceInfo = atmService.getBalanceInfo();
        log.info(balanceInfo);
    }
}
