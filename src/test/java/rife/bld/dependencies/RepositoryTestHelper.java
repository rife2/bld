/*
 * Copyright 2026 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static rife.bld.dependencies.Repository.*;

/**
 * Helper for distributing Maven repository selection in tests.
 * <p>
 * The starting index is seeded from system properties {@code os.name}, {@code os.arch},
 * {@code os.version}, and {@code java.version}. This ensures different platforms/JVMs
 * begin iteration at different repositories, reducing load on a single mirror while
 * keeping selection deterministic per environment for reproducible tests.
 * <p>
 * All methods are thread-safe and perform round-robin selection from {@link #MAVEN_CENTRAL_REPOSITORIES}.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @author <a href="https://www.uwyn.com">Geert Bevin</a>
 */
public final class RepositoryTestHelper {
    /**
     * List of Maven Central repositories and mirrors used for test distribution.
     */
    public static final List<Repository> MAVEN_CENTRAL_REPOSITORIES = List.of(
            MAVEN_CENTRAL,
            APACHE,
            GOOGLE_MAVEN_CENTRAL,
            GOOGLE_MAVEN_CENTRAL_EU,
            GOOGLE_MAVEN_CENTRAL_ASIA
    );

    // Generate seed from os.name, os.arch, os.version, java.version
    private static final int SEED = Math.abs(
            (System.getProperty("os.name", "") +
                    System.getProperty("os.arch", "") +
                    System.getProperty("os.version", "") +
                    System.getProperty("java.version", ""))
                    .hashCode()
    );

    private static final AtomicInteger COUNTER = new AtomicInteger(SEED);

    private RepositoryTestHelper() {
        // Utility class
    }

    /**
     * Returns the next {@link Repository} using round-robin selection.
     * <p>
     * The sequence starts at an index derived from the current OS and JVM properties,
     * then increments atomically on each call.
     *
     * @return the next repository in the rotation
     */
    public static Repository getNextRepository() {
        var index = COUNTER.getAndIncrement() % MAVEN_CENTRAL_REPOSITORIES.size();
        return MAVEN_CENTRAL_REPOSITORIES.get(index);
    }

    /**
     * Returns the next {@code count} repositories using round-robin selection.
     * <p>
     * The starting position is based on the current counter value, which is initially
     * seeded from OS and JVM properties. The counter is advanced by {@code count}.
     *
     * @param count the number of repositories to return
     * @return an unmodifiable list of repositories
     * @throws IllegalArgumentException if {@code count} is negative or exceeds the
     *                                  number of available repositories
     */
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

    /**
     * Returns the next 2 repositories using round-robin selection.
     * <p>
     * Convenience method equivalent to {@code getNextRepositories(2)}.
     *
     * @return an unmodifiable list containing 2 repositories
     * @see #getNextRepositories(int)
     */
    public static List<Repository> getNextRepositories() {
        return getNextRepositories(2);
    }
}