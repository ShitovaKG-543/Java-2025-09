package ru.otus;

import java.util.List;
import ru.otus.runner.TestRunner;
import ru.otus.tests.CollectionTests;
import ru.otus.tests.OperationTests;
import ru.otus.tests.StringTests;

public class Main {
    public static void main(String[] args) {

        List<Class<?>> classList = List.of(StringTests.class, OperationTests.class, CollectionTests.class);
        TestRunner runner = new TestRunner();
        for (Class<?> element : classList) {
            runner.runTests(element.getName());
        }
    }
}
