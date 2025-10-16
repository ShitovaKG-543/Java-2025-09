package ru.otus.homework;

import java.util.ArrayDeque;
import java.util.Deque;

public class CustomerReverseOrder {

    private final Deque<Customer> customers = new ArrayDeque<>();

    public void add(Customer customer) {
        Customer immutableCustomer = new Customer(customer.getId(), customer.getName(), customer.getScores());
        customers.push(immutableCustomer);
    }

    public Customer take() {
        return customers.pop();
    }
}
