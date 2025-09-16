package edu.ccrm.util;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface Searchable<T, R> {
    Optional<T> find(List<T> items, R criteria);
}

