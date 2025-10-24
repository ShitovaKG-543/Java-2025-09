package ru.otus.runner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.annotations.After;
import ru.otus.annotations.Before;
import ru.otus.annotations.Test;
import ru.otus.utils.TestResults;

public class TestRunner {

    private static final Logger log = LoggerFactory.getLogger(TestRunner.class);

    public void runTests(String className) {

        try {
            Class<?> testClass = Class.forName(className);
            TestResults results = runTestsForClass(testClass);
            printStatistics(results, className);
        } catch (ClassNotFoundException e) {
            log.error("Class not found: {}", className);
        }
    }

    private TestResults runTestsForClass(Class<?> testClass) {
        TestResults results = new TestResults();
        log.info("\n=== TEST CLASS {} ===", testClass.getName());
        List<Method> beforeMethods = findMethodsByAnnotation(testClass, Before.class);
        List<Method> testMethods = findMethodsByAnnotation(testClass, Test.class);
        List<Method> afterMethods = findMethodsByAnnotation(testClass, After.class);

        log.info("Found {} test methods", testMethods.size());

        for (Method testMethod : testMethods) {
            boolean isTestSuccess = runSingleTest(testClass, testMethod, beforeMethods, afterMethods);

            if (isTestSuccess) {
                results.incrementPassed();
            } else {
                results.incrementFailed();
            }
        }

        return results;
    }

    private boolean runSingleTest(
            Class<?> testClass, Method testMethod, List<Method> beforeMethods, List<Method> afterMethods) {
        boolean isTestSuccess = false;
        Object testInstance = createTestInstance(testClass);
        if (testInstance == null) {
            return false;
        }

        String testName = testMethod.getName();

        try {
            // Выполняем Before методы
            executeMethods(testInstance, beforeMethods);

            // Выполняем тестовый метод
            testMethod.invoke(testInstance);
            isTestSuccess = true;
            log.info("Test passed: {}", testName);

        } catch (Exception e) {
            Throwable rootCause = getRootCause(e);
            log.warn("Test failed: {} - {}", testName, rootCause.getMessage());

        } finally {
            // Всегда выполняем After методы
            try {
                executeMethods(testInstance, afterMethods);
            } catch (Exception e) {
                log.error("Error in After method for test: {}", testName);
            }
        }
        return isTestSuccess;
    }

    private List<Method> findMethodsByAnnotation(Class<?> testClass, Class<? extends Annotation> annotation) {
        List<Method> methods = new ArrayList<>();
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                methods.add(method);
            }
        }
        return methods;
    }

    private Object createTestInstance(Class<?> testClass) {
        try {
            return testClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Failed to create test instance: {}", e.getMessage());
            return null;
        }
    }

    private void executeMethods(Object instance, List<Method> methods) {
        for (Method method : methods) {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute method: " + method.getName(), e);
            }
        }
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void printStatistics(TestResults results, String className) {
        log.info("\n=== TEST RESULTS OF CLASS {} ===", className);
        log.info("Total tests: {}", results.getTotal());
        log.info("Passed: {}", results.getPassed());
        log.info("Failed: {}", results.getFailed());
        log.info("Success rate: {}%", results.getSuccessRate());

        if (results.getFailed() > 0) {
            log.warn("Some tests failed! Please check the logs above.");
        } else {
            log.info("All tests passed successfully!");
        }
    }
}
