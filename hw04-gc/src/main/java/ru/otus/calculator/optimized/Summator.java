package ru.otus.calculator.optimized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Summator {
    private int sum = 0;
    private int prevValue = 0;
    private int prevPrevValue = 0;
    private int sumLastThreeValues = 0;
    private int someValue = 0;
    // !!! эта коллекция должна остаться. Заменять ее на счетчик нельзя.
    private final List<Data> listValues = new ArrayList<>();
    private final SecureRandom random = new SecureRandom();

    // !!! сигнатуру метода менять нельзя
    public void calc(Data data) {
        int currentValue = data.getValue();
        int randomValue = random.nextInt();

        listValues.add(data);
        if (listValues.size() % 100_000 == 0) {
            listValues.clear();
        }
        sum += currentValue + randomValue;

        sumLastThreeValues = currentValue + prevValue + prevPrevValue;

        prevPrevValue = prevValue;
        prevValue = currentValue;
        // Предвычисление констант для цикла
        int operationValue = (sumLastThreeValues * sumLastThreeValues) / (currentValue + 1) - sum;
        int listSize = listValues.size();

        for (var idx = 0; idx < 3; idx++) {
            someValue += operationValue;
            someValue = Math.abs(someValue) + listSize;
        }
    }

    public int getSum() {
        return sum;
    }

    public int getPrevValue() {
        return prevValue;
    }

    public int getPrevPrevValue() {
        return prevPrevValue;
    }

    public int getSumLastThreeValues() {
        return sumLastThreeValues;
    }

    public int getSomeValue() {
        return someValue;
    }
}
