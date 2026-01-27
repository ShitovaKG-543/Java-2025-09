package ru.otus.appcontainer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;

@SuppressWarnings("squid:S1068")
public class AppComponentsContainerImpl implements AppComponentsContainer {

    private final List<Object> appComponents = new ArrayList<>();
    private final Map<String, Object> appComponentsByName = new HashMap<>();

    public AppComponentsContainerImpl(Class<?> initialConfigClass) {
        processConfig(initialConfigClass);
    }

    public AppComponentsContainerImpl(Class<?>... configClasses) {
        List<Class<?>> sortedConfigClasses = Arrays.stream(configClasses)
                .sorted(Comparator.comparingInt(configClass -> configClass
                        .getAnnotation(AppComponentsContainerConfig.class)
                        .order()))
                .toList();

        processConfigs(sortedConfigClasses);
    }

    public AppComponentsContainerImpl(String packageName) {
        List<Class<?>> configClasses = findConfigClassesUsingReflections(packageName);
        processConfigs(configClasses);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C getAppComponent(Class<C> componentClass) {
        List<Object> foundComponents = new ArrayList<>();

        // Ищем компоненты по всем сохраненным
        for (Object component : appComponents) {
            if (componentClass.isAssignableFrom(component.getClass())) {
                foundComponents.add(component);
            }
        }

        if (foundComponents.isEmpty()) {
            throw new RuntimeException("Не найден компонент типа: " + componentClass.getName());
        }

        if (foundComponents.size() > 1) {
            throw new RuntimeException("Найдено более одного компонента типа: " + componentClass.getName());
        }

        return (C) foundComponents.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> C getAppComponent(String componentName) {
        Object component = appComponentsByName.get(componentName);
        if (component == null) {
            throw new RuntimeException("Не найден компонент с именем: " + componentName);
        }
        return (C) component;
    }

    private void processConfig(Class<?> configClass) {
        checkConfigClass(configClass);

        // Собираем методы из одного конфига
        List<MethodWithConfig> methods = collectMethodsFromConfig(configClass);

        // Создаем компоненты из этих методов
        createComponentsFromMethods(methods);
    }

    private void processConfigs(List<Class<?>> configClasses) {
        List<MethodWithConfig> allMethods = new ArrayList<>();

        // Собираем все методы из всех конфигов
        for (Class<?> configClass : configClasses) {
            allMethods.addAll(collectMethodsFromConfig(configClass));
        }

        // Сортируем и создаем компоненты
        createComponentsFromMethods(allMethods);
    }

    private void checkConfigClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(AppComponentsContainerConfig.class)) {
            throw new IllegalArgumentException(String.format("Given class is not config %s", configClass.getName()));
        }
    }

    private List<Class<?>> findConfigClassesUsingReflections(String packageName) {
        try {
            Reflections reflections = new Reflections(
                    new ConfigurationBuilder().forPackages(packageName).addScanners(Scanners.TypesAnnotated));

            Set<Class<?>> configClasses = reflections.getTypesAnnotatedWith(AppComponentsContainerConfig.class);

            return configClasses.stream()
                    .sorted(Comparator.comparingInt(clazz -> clazz.getAnnotation(AppComponentsContainerConfig.class)
                            .order()))
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new))
                    .stream()
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при сканировании пакета " + packageName, e);
        }
    }

    private List<MethodWithConfig> collectMethodsFromConfig(Class<?> configClass) {
        checkConfigClass(configClass);
        List<MethodWithConfig> methods = new ArrayList<>();

        try {
            Object configInstance = configClass.getDeclaredConstructor().newInstance();
            List<Method> componentMethods = Arrays.stream(configClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(AppComponent.class))
                    .toList();

            for (Method method : componentMethods) {
                methods.add(new MethodWithConfig(method, configInstance));
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании экземпляра конфига " + configClass.getName(), e);
        }

        return methods;
    }

    private void createComponentsFromMethods(List<MethodWithConfig> methodsWithConfig) {
        // Сортируем все методы по order
        methodsWithConfig.sort(Comparator.comparingInt(
                m -> m.method.getAnnotation(AppComponent.class).order()));

        // Создаем компоненты в правильном порядке
        for (MethodWithConfig methodWithConfig : methodsWithConfig) {
            createComponent(methodWithConfig.method, methodWithConfig.configInstance);
        }
    }

    private void createComponent(Method method, Object configInstance) {
        AppComponent annotation = method.getAnnotation(AppComponent.class);
        String componentName = annotation.name();

        checkComponentNameUniqueness(componentName);

        try {
            Object[] args = resolveDependencies(method);
            Object component = method.invoke(configInstance, args);
            registerComponent(componentName, component);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании компонента " + componentName, e);
        }
    }

    private void checkComponentNameUniqueness(String componentName) {
        if (appComponentsByName.containsKey(componentName)) {
            throw new IllegalArgumentException("Компонент с именем '" + componentName + "' уже существует");
        }
    }

    private Object[] resolveDependencies(Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            Object dependency = findComponentByType(paramType);

            if (dependency == null) {
                throw new RuntimeException(
                        "Не удалось найти зависимость типа " + paramType.getName() + " для метода " + method.getName());
            }
            args[i] = dependency;
        }

        return args;
    }

    private void registerComponent(String componentName, Object component) {
        appComponents.add(component);
        appComponentsByName.put(componentName, component);
    }

    private Object findComponentByType(Class<?> componentClass) {
        List<Object> foundComponents = findComponentsByType(componentClass);

        if (foundComponents.isEmpty()) {
            return null;
        }

        if (foundComponents.size() > 1) {
            throw new RuntimeException("Найдено более одного компонента типа: " + componentClass.getName());
        }

        return foundComponents.get(0);
    }

    private List<Object> findComponentsByType(Class<?> componentClass) {
        List<Object> foundComponents = new ArrayList<>();

        for (Object component : appComponents) {
            if (componentClass.isAssignableFrom(component.getClass())) {
                foundComponents.add(component);
            }
        }

        return foundComponents;
    }
}
