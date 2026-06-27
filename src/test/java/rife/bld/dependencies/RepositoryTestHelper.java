/*
 * Copyright 2026 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.tools.exceptions.FileUtilsErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static rife.bld.dependencies.Repository.*;

/**
 * Hands out repositories to tests so the resolution load is spread as evenly as
 * possible across all the Maven Central mirrors.
 * <p>
 * A {@link DependencyResolver} contacts the repositories in order and stops at
 * the first one that holds the artifact (see {@code parseMavenMetadata} and
 * {@code transferIntoDirectory}). Because the dependencies these tests resolve
 * exist in every mirror, only the <em>first</em> repository of each list is
 * actually contacted; the rest act purely as fallbacks. The load therefore
 * lands entirely on whichever repository is handed out first.
 * <p>
 * That load is dominated by a handful of tests with large transitive trees, each
 * sending hundreds of reads to its single head repository, so a blind round-robin
 * cannot balance the actual reads. Instead, the head is chosen greedily: each test
 * is handed the mirror that has accumulated the fewest reads so far, observed
 * through {@link #retriever()}. After a heavy test lands on the lightest mirror,
 * that mirror becomes the heaviest and is avoided until the others catch up, so
 * the cumulative reads converge toward an even split. Tests must resolve through
 * {@link #retriever()} for their reads to be counted.
 */
public final class RepositoryTestHelper {
    public static final List<Repository> MAVEN_CENTRAL_REPOSITORIES = List.of(
            MAVEN_CENTRAL,
            APACHE,
            GOOGLE_MAVEN_CENTRAL,
            GOOGLE_MAVEN_CENTRAL_EU,
            GOOGLE_MAVEN_CENTRAL_ASIA
    );

    // Actual reads observed per repository, used as the greedy balancing signal.
    private static final Map<Repository, AtomicLong> READS = new ConcurrentHashMap<>();
    static {
        MAVEN_CENTRAL_REPOSITORIES.forEach(repo -> READS.put(repo, new AtomicLong()));
    }

    // Rotates the scan start so mirrors tied on load (e.g. all zero at startup)
    // are still handed out in turn rather than always defaulting to the first.
    private static final AtomicInteger TIE_BREAKER = new AtomicInteger(0);

    // Retriever that behaves like ArtifactRetriever.instance() (uncached) but
    // records every read against its repository so the load can be balanced.
    private static final ArtifactRetriever COUNTING_RETRIEVER = new ArtifactRetriever() {
        String getCached(RepositoryArtifact artifact) {
            return null;
        }

        void cache(RepositoryArtifact artifact, String content) {
        }

        @Override
        public String readString(RepositoryArtifact artifact)
        throws FileUtilsErrorException {
            READS.computeIfAbsent(artifact.repository(), r -> new AtomicLong()).incrementAndGet();
            return super.readString(artifact);
        }
    };

    private RepositoryTestHelper() {
    }

    /**
     * The retriever tests must use so their reads are attributed for balancing.
     */
    public static ArtifactRetriever retriever() {
        return COUNTING_RETRIEVER;
    }

    public static Repository getNextRepository() {
        return leastLoaded();
    }

    public static List<Repository> getNextRepositories(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
        if (count > MAVEN_CENTRAL_REPOSITORIES.size()) {
            throw new IllegalArgumentException("count cannot exceed repository list size: "
                    + MAVEN_CENTRAL_REPOSITORIES.size());
        }

        // The head bears the load, so pick the least-loaded mirror; the remaining
        // mirrors are appended only as (never-contacted) fallbacks.
        var head = leastLoaded();
        var result = new ArrayList<Repository>(count);
        if (count > 0) {
            result.add(head);
        }
        for (var repo : MAVEN_CENTRAL_REPOSITORIES) {
            if (result.size() == count) {
                break;
            }
            if (!repo.equals(head)) {
                result.add(repo);
            }
        }
        return List.copyOf(result);
    }

    public static List<Repository> getNextRepositories() {
        return getNextRepositories(2);
    }

    /**
     * Returns the mirror that has accumulated the fewest reads so far, rotating
     * the scan start so equally-loaded mirrors are still spread in turn.
     */
    private static Repository leastLoaded() {
        var size = MAVEN_CENTRAL_REPOSITORIES.size();
        var offset = TIE_BREAKER.getAndIncrement();
        Repository best = null;
        var bestReads = Long.MAX_VALUE;
        for (var i = 0; i < size; i++) {
            var repo = MAVEN_CENTRAL_REPOSITORIES.get(Math.floorMod(offset + i, size));
            var reads = READS.get(repo).get();
            if (reads < bestReads) {
                bestReads = reads;
                best = repo;
            }
        }
        return best;
    }
}
