package ru.otus.jdbc.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import ru.otus.crm.annotation.Id;

public class EntityClassMetaDataImpl<T> implements EntityClassMetaData<T> {
    private Class<T> clazz;
    private Constructor<T> constructor;
    private Field idField;
    private List<Field> allFields;
    private List<Field> fieldsWithoutId;

    public EntityClassMetaDataImpl() {
        // Определяем класс из generic параметра
        determineClass();
        initialize();
    }

    public EntityClassMetaDataImpl(Class<T> clazz) {
        this.clazz = clazz;
        initialize();
    }

    @SuppressWarnings("unchecked")
    private void determineClass() {
        try {
            // Получаем generic тип из родительского класса
            ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();

            // Если класс создан анонимно
            if (genericSuperclass == null || genericSuperclass.equals(Object.class)) {
                // Попробуем получить тип из интерфейса
                ParameterizedType genericInterface =
                        (ParameterizedType) getClass().getGenericInterfaces()[0];
                this.clazz = (Class<T>) genericInterface.getActualTypeArguments()[0];
            } else {
                this.clazz = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot determine entity class. "
                            + "Please specify class explicitly: new EntityClassMetaDataImpl<>(Client.class)",
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private void initialize() {
        if (clazz == null) {
            throw new IllegalStateException("Class not determined");
        }

        try {
            // Получаем конструктор без параметров
            this.constructor = (Constructor<T>) clazz.getDeclaredConstructor();
            this.constructor.setAccessible(true);

            // Инициализируем списки полей
            this.allFields = new ArrayList<>();
            this.fieldsWithoutId = new ArrayList<>();

            // Получаем все поля класса, включая приватные
            Field[] declaredFields = clazz.getDeclaredFields();

            for (Field field : declaredFields) {
                field.setAccessible(true);
                allFields.add(field);

                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                } else {
                    fieldsWithoutId.add(field);
                }
            }

            // Проверяем, что найдено поле с @Id
            if (idField == null) {
                throw new IllegalStateException("No field with @Id annotation found in class " + clazz.getName());
            }

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Class " + clazz.getName() + " must have a no-args constructor", e);
        }
    }

    @Override
    public String getName() {
        // Преобразуем имя класса в snake_case для имени таблицы
        // Например: Client -> client, UserProfile -> user_profile
        String className = clazz.getSimpleName();
        return camelToSnake(className).toLowerCase();
    }

    private String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        char c = str.charAt(0);
        result.append(Character.toLowerCase(c));

        for (int i = 1; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    @Override
    public Constructor<T> getConstructor() {
        return constructor;
    }

    @Override
    public Field getIdField() {
        return idField;
    }

    @Override
    public List<Field> getAllFields() {
        return new ArrayList<>(allFields);
    }

    @Override
    public List<Field> getFieldsWithoutId() {
        return new ArrayList<>(fieldsWithoutId);
    }
}
