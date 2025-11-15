package ru.otus;

import java.util.List;
import ru.otus.annotations.Log;

public class TestLogging {

    @Log
    public void calculation(int param) {}

    public void calculation(int param1, int param2) {}

    @Log
    public void calculation(int param1, int param2, String param3) {}

    @Log
    public void handle(int param1, boolean param2) {}

    @Log
    public void anyMethod(List<String> strings, char c) {}
}
