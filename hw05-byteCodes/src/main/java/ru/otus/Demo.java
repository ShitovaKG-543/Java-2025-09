package ru.otus;

import java.util.List;
import ru.otus.bytebuddy.ProxyFactory;

public class Demo {

    public static void main(String[] args) {

        TestLogging testLogging = ProxyFactory.createProxy(new TestLogging());
        testLogging.calculation(6);
        testLogging.calculation(4, 3);
        testLogging.handle(4, true);
        testLogging.calculation(3, 5, "test");
        testLogging.anyMethod(List.of("test1", "test2"), 'c');
    }
}
