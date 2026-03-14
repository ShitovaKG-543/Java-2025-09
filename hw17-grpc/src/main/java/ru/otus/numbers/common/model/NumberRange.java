package ru.otus.numbers.common.model;

public class NumberRange {
    private final int firstValue;
    private final int lastValue;

    public NumberRange(int firstValue, int lastValue) {
        this.firstValue = firstValue;
        this.lastValue = lastValue;
    }

    public int getFirstValue() {
        return firstValue;
    }

    public int getLastValue() {
        return lastValue;
    }

    public boolean isValid() {
        return firstValue < lastValue;
    }

    public int getCount() {
        return lastValue - firstValue;
    }
}
