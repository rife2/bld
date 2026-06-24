/*
 * Copyright 2026 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static rife.bld.dependencies.Repository.*;

public final class RepositoryTestHelper {
    public static final List<Repository> MAVEN_CENTRAL_REPOSITORIES = List.of(
            MAVEN_CENTRAL,
            APACHE,
            GOOGLE_MAVEN_CENTRAL,
            GOOGLE_MAVEN_CENTRAL_EU,
            GOOGLE_MAVEN_CENTRAL_ASIA
    );

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private RepositoryTestHelper() {
    }

    public static Repository getNextRepository() {
        var index = COUNTER.getAndIncrement() % MAVEN_CENTRAL_REPOSITORIES.size();
        return MAVEN_CENTRAL_REPOSITORIES.get(index);
    }

    public static List<Repository> getNextRepositories(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
        if (count > MAVEN_CENTRAL_REPOSITORIES.size()) {
            throw new IllegalArgumentException("count cannot exceed repository list size: "
                    + MAVEN_CENTRAL_REPOSITORIES.size());
        }

        var start = COUNTER.getAndAdd(count) % MAVEN_CENTRAL_REPOSITORIES.size();
        var result = new ArrayList<Repository>(count);
        for (var i = 0; i < count; i++) {
            result.add(MAVEN_CENTRAL_REPOSITORIES.get((start + i) % MAVEN_CENTRAL_REPOSITORIES.size()));
        }
        return List.copyOf(result);
    }

    public static List<Repository> getNextRepositories() {
        return getNextRepositories(2);
    }
}
