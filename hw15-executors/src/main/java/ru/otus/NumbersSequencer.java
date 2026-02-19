package ru.otus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumbersSequencer {
    private static final Logger logger = LoggerFactory.getLogger(NumbersSequencer.class);
    private boolean isThread1Turn = true;

    // Состояние для первого потока
    private int number1 = 1;
    private boolean increasing1 = true;

    // Состояние для второго потока
    private int number2 = 1;
    private boolean increasing2 = true;

    private synchronized void action(int threadId) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                while ((threadId == 1) != isThread1Turn) {
                    this.wait();
                }

                if (threadId == 1) {
                    logger.info("Поток 1: {}", number1);
                    number1 = getNextNumber(number1, increasing1);
                    increasing1 = shouldIncrease(number1, increasing1);
                } else {
                    logger.info("Поток 2: {}", number2);
                    number2 = getNextNumber(number2, increasing2);
                    increasing2 = shouldIncrease(number2, increasing2);
                }

                isThread1Turn = !isThread1Turn;
                sleep();
                notifyAll();

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private int getNextNumber(int current, boolean increasing) {
        return increasing ? current + 1 : current - 1;
    }

    private boolean shouldIncrease(int number, boolean increasing) {
        if (number >= 10) return false;
        if (number <= 1) return true;
        return increasing;
    }

    public static void main(String[] args) {
        NumbersSequencer numbersSequencer = new NumbersSequencer();
        new Thread(() -> numbersSequencer.action(1)).start();
        new Thread(() -> numbersSequencer.action(2)).start();
    }

    private static void sleep() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
