package ru.otus.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.annotations.After;
import ru.otus.annotations.Before;
import ru.otus.annotations.Test;

public class CollectionTests {

    private static final Logger logger = LoggerFactory.getLogger(CollectionTests.class);

    private List<String> stringList;
    private Set<Integer> numberSet;
    private Map<String, Integer> wordCountMap;

    @Before
    public void setUpCollections() {
        logger.info("Setting up collections for testing");

        stringList = new ArrayList<>();
        stringList.add("apple");
        stringList.add("banana");
        stringList.add("cherry");

        numberSet = new HashSet<>();
        numberSet.add(1);
        numberSet.add(2);
        numberSet.add(3);

        wordCountMap = new HashMap<>();
        wordCountMap.put("hello", 1);
        wordCountMap.put("world", 2);
        wordCountMap.put("test", 3);
    }

    @After
    public void cleanUpCollections() {
        stringList.clear();
        numberSet.clear();
        wordCountMap.clear();
        logger.info("Collections cleaned up");
    }

    @Test
    public void testListOperations() {
        logger.info("Testing List operations");

        // Проверяем размер
        if (stringList.size() != 3) {
            throw new AssertionError("List should have 3 elements");
        }

        // Проверяем содержимое
        if (!stringList.contains("banana")) {
            throw new AssertionError("List should contain 'banana'");
        }

        // Тестируем получение по индексу
        String firstElement = stringList.get(0);
        if (!"apple".equals(firstElement)) {
            throw new AssertionError("First element should be 'apple'");
        }

        logger.info("List operations test passed. List: {}", stringList);
    }

    @Test
    public void testIndexOutOfBoundsException() {
        logger.info("Testing IndexOutOfBoundsException");

        // ОШИБКА: обращение к несуществующему индексу
        String element = stringList.get(10);
        logger.info("Element at index 10: {}", element);
    }

    @Test
    public void testSetUniqueness() {
        logger.info("Testing Set uniqueness");

        int initialSize = numberSet.size();
        numberSet.add(2); // Дубликат
        numberSet.add(4); // Новый элемент

        if (numberSet.size() != initialSize + 1) {
            throw new AssertionError("Set should only grow by 1 when adding duplicate and new element");
        }

        logger.info("Set uniqueness test passed. Set: {}", numberSet);
    }

    @Test
    public void testMapOperations() {
        logger.info("Testing Map operations");

        // Проверяем существующие ключи
        if (!wordCountMap.containsKey("hello")) {
            throw new AssertionError("Map should contain key 'hello'");
        }

        // Проверяем значения
        Integer testValue = wordCountMap.get("test");
        if (testValue == null || testValue != 3) {
            throw new AssertionError("Key 'test' should have value 3");
        }

        // Тестируем добавление
        wordCountMap.put("new", 10);
        if (wordCountMap.get("new") != 10) {
            throw new AssertionError("New key should have value 10");
        }

        logger.info("Map operations test passed. Map size: {}", wordCountMap.size());
    }
}
