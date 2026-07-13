/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Speculatively retrieves POMs in parallel so that they are already cached
 * by the artifact retriever when the sequential dependency resolution
 * processes them, without influencing the resolution semantics.
 * <p>
 * A single instance can be shared by multiple concurrent resolutions, the
 * POM of each unique dependency will only be prefetched once.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.3.1
 */
class PomPrefetcher {
    private final VersionResolution resolution_;
    private final ArtifactRetriever retriever_;
    private final List<Repository> repositories_;
    private final ExecutorService executor_;
    private final Set<Dependency> submitted_ = ConcurrentHashMap.newKeySet();

    /**
     * Creates a prefetcher when it can be beneficial.
     *
     * @return the prefetcher; or {@code null} when the retriever doesn't
     * cache the retrieved POMs or when the resolution parallelism disables it
     * @since 2.3.1
     */
    static PomPrefetcher create(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories) {
        // prefetching only has benefits when the retrieved POMs are
        // cached for the sequential resolution that follows
        if (!retriever.isCaching() || resolution.resolutionParallelism() <= 1) {
            return null;
        }
        return new PomPrefetcher(resolution, retriever, repositories);
    }

    private PomPrefetcher(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories) {
        resolution_ = resolution;
        retriever_ = retriever;
        repositories_ = repositories;
        executor_ = Executors.newFixedThreadPool(resolution.resolutionParallelism());
    }

    void prefetch(Collection<PomDependency> candidates) {
        for (var candidate : candidates) {
            var dependency = resolution_.overrideDependency(candidate.convertToDependency());
            if (submitted_.add(dependency)) {
                executor_.submit(() -> {
                    try {
                        new DependencyResolver(resolution_, retriever_, repositories_, dependency).getMavenPom(dependency);
                    } catch (Throwable e) {
                        // failures are ignored since they will resurface
                        // with the proper context when the dependency is
                        // resolved in order
                    }
                });
            }
        }
    }

    void shutdown() {
        executor_.shutdownNow();
    }
}
