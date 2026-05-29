/*
 * Copyright 2026 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static rife.bld.dependencies.Repository.*;

public final class RepositoryTestHelper {
    public static final List<Repository> MAVEN_CENTRAL_REPOSITORIES = List.of(
            MAVEN_CENTRAL,
            APACHE,
            GOOGLE_MAVEN_CENTRAL,
            GOOGLE_MAVEN_CENTRAL_EU,
            GOOGLE_MAVEN_CENTRAL_ASIA
    );

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RepositoryTestHelper() {
    }

    public static Repository getRandomRepository() {
        int index = SECURE_RANDOM.nextInt(MAVEN_CENTRAL_REPOSITORIES.size());
        return MAVEN_CENTRAL_REPOSITORIES.get(index);
    }

    public static List<Repository> getRandomRepositories(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
        if (count > MAVEN_CENTRAL_REPOSITORIES.size()) {
            throw new IllegalArgumentException("count cannot exceed repository list size: "
                    + MAVEN_CENTRAL_REPOSITORIES.size());
        }

        List<Repository> shuffled = new ArrayList<>(MAVEN_CENTRAL_REPOSITORIES);
        Collections.shuffle(shuffled, SECURE_RANDOM);
        return Collections.unmodifiableList(shuffled.subList(0, count));
    }

    public static List<Repository> getRandomRepositories() {
        return getRandomRepositories(2);
    }
}
