package ru.otus.homework;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class CustomerService {

    private final NavigableMap<Customer, String> map = new TreeMap<>(new CustomerScoresComparator());

    public Map.Entry<Customer, String> getSmallest() {
        Map.Entry<Customer, String> entry = map.firstEntry();
        return entry == null ? null : copyEntry(entry);
    }

    public Map.Entry<Customer, String> getNext(Customer customer) {
        Map.Entry<Customer, String> entry = map.higherEntry(customer);
        return entry == null ? null : copyEntry(entry);
    }

    public void add(Customer customer, String data) {
        Customer immutableCustomer = new Customer(customer.getId(), customer.getName(), customer.getScores());
        map.put(immutableCustomer, data);
    }

    private Map.Entry<Customer, String> copyEntry(Map.Entry<Customer, String> entry) {
        Customer original = entry.getKey();
        Customer copy = new Customer(original.getId(), original.getName(), original.getScores());
        return Map.entry(copy, entry.getValue());
    }
}
