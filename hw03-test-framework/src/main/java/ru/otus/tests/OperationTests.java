package ru.otus.tests;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.annotations.After;
import ru.otus.annotations.Before;
import ru.otus.annotations.Test;

public class OperationTests {
    private static final Logger log = LoggerFactory.getLogger(OperationTests.class);

    private Random random;
    private int testNumber;

    @Before
    public void actionBefore1() {
        log.info("OperationTests. Action before 1...");
    }

    @Before
    public void actionBefore2() {
        random = new Random();
        testNumber = random.nextInt(100) + 1;
        log.info("OperationTests. Action before 2...");
    }

    @Before
    public void actionBefore3() {
        log.info("OperationTests. Action before 3...");
    }

    @After
    public void actionAfter1() {
        log.info("OperationTests. After test 1: Cleaning up...");
    }

    @After
    public void actionAfter2() {
        log.info("OperationTests. After test 2: Cleaning up...");
    }

    @After
    public void actionAfter3() {
        log.info("OperationTests. After test 3: Cleaning up...");
    }

    @Test
    public void testAddition() {
        log.info("Running test: Addition");
        int result = 2 + 2;
        assert result == 4 : "Addition failed";
    }

    @Test
    public void testFailing() {
        log.info("Running test: Failing test");
        throw new RuntimeException("This test is supposed to fail");
    }

    @Test
    public void testSubtraction() {
        log.info("Running test: Subtraction");
        int result = 5 - 3;
        assert result == 2 : "Subtraction failed";
    }

    // Метод без аннотации - не должен запускаться
    public void helperMethod() {
        log.info("This should not be executed");
    }

    @Test
    public void testSquareOperation() {
        log.info("Testing square operation for number: {}", testNumber);
        int squared = testNumber * testNumber;
        int expected = (int) Math.pow(testNumber, 2);

        if (squared != expected) {
            throw new AssertionError("Square calculation failed for: " + testNumber);
        }
        log.info("Square test passed: {}² = {}", testNumber, squared);
    }

    @Test
    public void testDivisionByZeroProtection() {
        log.info("Testing division by zero protection");
        try {
            int result = testNumber / 1;
            log.info("Division test passed: {} / 1 = {}", testNumber, result);
        } catch (ArithmeticException e) {
            throw new AssertionError("Unexpected division error: " + e.getMessage());
        }
    }
}
