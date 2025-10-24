package ru.otus.utils;

public class TestResults {
    private int passed = 0;
    private int failed = 0;

    public void incrementPassed() {
        passed++;
    }

    public void incrementFailed() {
        failed++;
    }

    public int getTotal() {
        return passed + failed;
    }

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failed;
    }

    public double getSuccessRate() {
        int total = getTotal();
        return total > 0 ? Math.round((passed * 100.0 / total) * 100.0) / 100.0 : 0;
    }
}
