package ru.otus.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import ru.otus.exception.ImpossibleIssuedAtmException;
import ru.otus.exception.UnacceptableBanknoteAtmException;

@Slf4j
public class Atm {
    private static volatile Atm instance;
    private final Map<Banknote, Integer> cells;

    private Atm() {
        this.cells = new TreeMap<>((b1, b2) -> Integer.compare(b2.getNominalAmount(), b1.getNominalAmount()));
        log.info("Создан новый экземпляр банкомата");
    }

    /**
     * Получение экземпляра банкомата.
     */
    public static Atm getInstance() {
        if (instance == null) {
            synchronized (Atm.class) {
                if (instance == null) {
                    instance = new Atm();
                }
            }
        }
        return instance;
    }

    /**
     * Инициализация банкомата с указанными номиналами.
     *
     * @param supportedBanknotes - поддерживаемые банкоматом банкноты
     */
    public void initialize(List<Banknote> supportedBanknotes) {
        cells.clear();
        for (Banknote banknote : supportedBanknotes) {
            cells.put(banknote, 0);
        }
    }

    /**
     * Получение общего баланса.
     */
    public int getTotalBalance() {

        return this.cells.keySet().stream()
                .mapToInt(banknote -> banknote.getNominalAmount() * this.cells.get(banknote))
                .sum();
    }

    /**
     * Получение общего баланса по ячейкам.
     */
    public Map<Banknote, Integer> getBalanceByBanknotes() {
        Map<Banknote, Integer> result = new HashMap<>();
        for (Banknote banknote : this.cells.keySet()) {
            result.put(banknote, banknote.getNominalAmount() * this.cells.get(banknote));
        }
        return result;
    }

    /**
     * Добавление банкнот в ячейки.
     *
     * @param banknotes - банкноты и их количество, добавляемые в ячейки
     */
    public void addBanknotes(Map<Banknote, Integer> banknotes) {
        List<Integer> errorNominals = new ArrayList<>();
        for (Banknote banknote : banknotes.keySet()) {
            if (this.cells.containsKey(banknote)) {
                this.cells.put(banknote, this.cells.get(banknote) + banknotes.get(banknote));
            } else {
                errorNominals.add(banknote.getNominalAmount());
            }
        }
        if (!errorNominals.isEmpty()) {
            throw new UnacceptableBanknoteAtmException(
                    errorNominals.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
    }

    /**
     * Снятие суммы с ячеек.
     *
     * @param amount - снимаемая сумма
     * @return банкноты и их количество
     */
    public Map<Banknote, Integer> getAmount(int amount) {
        Map<Banknote, Integer> result = new HashMap<>();
        int remainingAmount = amount;

        // Временная копия для отслеживания изменений
        Map<Banknote, Integer> tempWithdrawal = new HashMap<>();

        // Пытаемся выдать сумму доступными банкнотами
        for (Banknote banknote : this.cells.keySet()) {
            if (cells.get(banknote) == 0) continue;

            int nominalAmount = banknote.getNominalAmount();
            int neededCount = remainingAmount / nominalAmount;
            int availableCount = Math.min(neededCount, cells.get(banknote));

            if (availableCount > 0) {
                tempWithdrawal.put(banknote, availableCount);
                remainingAmount -= availableCount * nominalAmount;
            }

            if (remainingAmount == 0) {
                break;
            }
        }

        // Если не удалось набрать точную сумму
        if (remainingAmount > 0) {
            RuntimeException exception = new ImpossibleIssuedAtmException(remainingAmount);
            log.error(exception.getMessage());
            throw exception;
        }

        // Фактическое изъятие банкнот из ячеек
        for (Map.Entry<Banknote, Integer> entry : tempWithdrawal.entrySet()) {
            this.cells.put(entry.getKey(), this.cells.get(entry.getKey()) - entry.getValue());
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
