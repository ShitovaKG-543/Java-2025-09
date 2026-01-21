package ru.otus.appcontainer;

import java.lang.reflect.Method;

public class MethodWithConfig {
    final Method method;
    final Object configInstance;

    MethodWithConfig(Method method, Object configInstance) {
        this.method = method;
        this.configInstance = configInstance;
    }
}
