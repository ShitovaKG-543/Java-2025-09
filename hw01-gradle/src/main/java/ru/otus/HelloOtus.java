package ru.otus;

import com.google.common.collect.ImmutableList;

public class HelloOtus {
    public static void main(String... args) {
        ImmutableList<String> list = ImmutableList.of("Hello", "Guava");
        System.out.println(list);
    }
}
