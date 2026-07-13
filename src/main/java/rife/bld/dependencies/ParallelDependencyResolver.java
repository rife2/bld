/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Resolves multiple dependencies in parallel within a list of
 * Maven-compatible repositories.
 * <p>
 * The parallelism is determined by {@link VersionResolution#resolutionParallelism()},
 * setting it to {@code 1} makes the resolution sequential. The results are
 * always identical to resolving each dependency sequentially with a
 * {@link DependencyResolver}, in the same order.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.3.1
 */
public class ParallelDependencyResolver {
    private final VersionResolution resolution_;
    private final ArtifactRetriever retriever_;
    private final List<Repository> repositories_;

    /**
     * Creates a new parallel resolver.
     * <p>
     * The repositories will be checked in the order they're listed.
     *
     * @param resolution   the version resolution state that can be cached
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @since 2.3.1
     */
    public ParallelDependencyResolver(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories) {
        resolution_ = resolution;
        retriever_ = retriever;
        if (repositories == null) {
            repositories = List.of();
        }
        repositories_ = repositories;
    }

    /**
     * Resolves the transitive dependencies of multiple root dependencies,
     * merging the results in the order of the provided roots.
     * <p>
     * The roots are resolved in parallel while a shared prefetcher
     * speculatively warms the retriever cache across all of them.
     *
     * @param roots  the root dependencies to resolve
     * @param scopes the scopes to return the transitive dependencies for
     * @return the merged transitive dependencies of all the roots
     * @since 2.3.1
     */
    public DependencySet resolveAllDependencies(Collection<Dependency> roots, Scope... scopes) {
        var result = new DependencySet();
        if (roots.isEmpty()) {
            return result;
        }

        var prefetcher = PomPrefetcher.create(resolution_, retriever_, repositories_);
        try {
            var resolutions = new ArrayList<Supplier<DependencySet>>(roots.size());
            for (var root : roots) {
                resolutions.add(() -> new DependencyResolver(resolution_, retriever_, repositories_, root).getAllDependencies(prefetcher, scopes));
            }
            for (var dependencies : ParallelExecution.execute(resolutions, resolution_.resolutionParallelism())) {
                result.addAll(dependencies);
            }
        } finally {
            if (prefetcher != null) {
                prefetcher.shutdown();
            }
        }
        return result;
    }

    /**
     * Resolves the latest versions of multiple dependencies, returning them
     * in the same order as the provided dependencies.
     *
     * @param dependencies the dependencies to resolve the latest versions of
     * @return the latest versions in the order of the provided dependencies
     * @since 2.3.1
     */
    public List<Version> resolveLatestVersions(List<Dependency> dependencies) {
        var resolutions = new ArrayList<Supplier<Version>>(dependencies.size());
        for (var dependency : dependencies) {
            resolutions.add(() -> new DependencyResolver(resolution_, retriever_, repositories_, dependency).latestVersion());
        }
        return ParallelExecution.execute(resolutions, resolution_.resolutionParallelism());
    }

    /**
     * Returns the version resolution state that can be cached.
     *
     * @return the version resolution state
     * @since 2.3.1
     */
    public VersionResolution resolution() {
        return resolution_;
    }

    /**
     * Retrieve the repositories that are used by this resolver.
     *
     * @return the resolver's repositories
     * @since 2.3.1
     */
    public List<Repository> repositories() {
        return repositories_;
    }
}
