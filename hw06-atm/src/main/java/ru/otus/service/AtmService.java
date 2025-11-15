package ru.otus.service;

import java.util.List;
import java.util.Map;
import ru.otus.model.Banknote;

public interface AtmService {

    void initializeAtm(List<Banknote> supportedBanknotes);

    String getBalanceInfo();

    void deposit(Map<Banknote, Integer> banknotes);

    Map<Banknote, Integer> withdraw(int amount);
}
