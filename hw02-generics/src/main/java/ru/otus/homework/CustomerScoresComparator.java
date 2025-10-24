package ru.otus.homework;

import java.util.Comparator;

public class CustomerScoresComparator implements Comparator<Customer> {

    @Override
    public int compare(Customer o1, Customer o2) {
        int scoreCompare = Long.compare(o1.getScores(), o2.getScores());
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        return Long.compare(o1.getId(), o2.getId());
    }
}
