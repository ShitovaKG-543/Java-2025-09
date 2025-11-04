package ru.otus.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.annotations.After;
import ru.otus.annotations.Before;
import ru.otus.annotations.Test;

public class StringTests {
    private static final Logger log = LoggerFactory.getLogger(StringTests.class);

    private String testString;
    private StringBuilder stringBuilder;

    @Before
    public void actionBefore1() {
        testString = "Hello, Test Framework!";
        log.info("StringTests. Action before 1. Set up test data: {}", testString);
    }

    @Before
    public void actionBefore2() {
        stringBuilder = new StringBuilder();
        log.info("StringTests. Action before 2...");
    }

    @After
    public void actionAfter1() {
        log.info("StringTests. After test 1: Cleaning up...");
    }

    @After
    public void actionAfter2() {
        stringBuilder = null;
        log.info("StringTests. After test 2: Cleaning up...");
    }

    @Test
    public void testStringContains() {
        log.info("Testing if string contains specific text");
        if (!testString.contains("Test")) {
            throw new AssertionError("String should contain 'Test'");
        }
        log.info("String contains test passed");
    }

    @Test
    public void testStringBuilderAppend() {
        log.info("Testing StringBuilder append operations");
        stringBuilder.append("Hello");
        stringBuilder.append(" ");
        stringBuilder.append("World");

        String result = stringBuilder.toString();
        if (!"Hello World".equals(result)) {
            throw new AssertionError("StringBuilder result incorrect: " + result);
        }
        log.info("StringBuilder test passed: {}", result);
    }

    @Test
    public void testFailing() {
        log.info("Running test: Failing test");
        throw new RuntimeException("This test is supposed to fail");
    }

    @Test
    public void testStringSplit() {
        log.info("Testing string split operation");
        String[] parts = testString.split(", ");
        if (parts.length != 2) {
            throw new AssertionError("Split should result in 2 parts, got: " + parts.length);
        }
        if (!"Hello".equals(parts[0])) {
            throw new AssertionError("First part should be 'Hello', got: " + parts[0]);
        }
        log.info("String split test passed. Parts: {}, {}", parts[0], parts[1]);
    }

    @After
    public void actionAfter3() {
        testString = null;
        log.info("StringTests. After test 3: Cleaning up...");
    }

    @Test
    public void testIncorrectSubstring() {
        log.info("Testing incorrect substring usage");
        // ОШИБКА: неправильные индексы для substring
        String result = testString.substring(20, 10); // start > end
        log.info("Substring result: {}", result);
    }
}
