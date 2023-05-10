/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Convenience class to map a {@link Scope} to its dependencies.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class DependencyScopes extends LinkedHashMap<Scope, DependencySet> {
    /**
     * Creates an empty dependency scope map.
     *
     * @since 1.5
     */
    public DependencyScopes() {
    }

    /**
     * Creates a dependency scope map from another one.
     *
     * @param other the other map to create this one from
     * @since 1.5
     */
    public DependencyScopes(DependencyScopes other) {
        for (var entry : other.entrySet()) {
            put(entry.getKey(), new DependencySet(entry.getValue()));
        }
    }

    /**
     * Includes all the dependencies from another dependency scope map.
     *
     * @param other the other map to include dependencies from
     * @since 1.5
     */
    public void include(DependencyScopes other) {
        for (var entry : other.entrySet()) {
            var dependencies = get(entry.getKey());
            if (dependencies == null) {
                dependencies = new DependencySet();
                put(entry.getKey(), dependencies);
            }
            dependencies.addAll(entry.getValue());
        }
    }

    /**
     * Retrieves the {@link DependencySet} for a particular scope.
     *
     * @param scope the scope to retrieve the dependencies for
     * @return the scope's {@code DependencySet};
     * or an empty {@code DependencySet} if none have been defined for the provided scope.
     * @since 1.5
     */
    public DependencySet scope(Scope scope) {
        return computeIfAbsent(scope, k -> new DependencySet());
    }

    /**
     * Returns the transitive set of dependencies that would be used for the compile scope in a project.
     *
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the compile scope dependency set
     * @since 1.6
     */
    public DependencySet resolveCompileDependencies(ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(retriever, repositories,
            new Scope[]{Scope.provided, Scope.compile},
            new Scope[]{Scope.compile},
            null);
    }

    /**
     * Returns the transitive set of dependencies that would be used for the runtime scope in a project.
     *
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the runtime scope dependency set
     * @since 1.6
     */
    public DependencySet resolveRuntimeDependencies(ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(retriever, repositories,
            new Scope[]{Scope.provided, Scope.compile, Scope.runtime},
            new Scope[]{Scope.compile, Scope.runtime},
            resolveCompileDependencies(retriever, repositories));
    }

    /**
     * Returns the transitive set of dependencies that would be used for the standalone scope in a project.
     *
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the standalone scope dependency set
     * @since 1.6
     */
    public DependencySet resolveStandaloneDependencies(ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(retriever, repositories,
            new Scope[]{Scope.standalone},
            new Scope[]{Scope.compile, Scope.runtime},
            null);
    }

    /**
     * Returns the transitive set of dependencies that would be used for the test scope in a project.
     *
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the test scope dependency set
     * @since 1.6
     */
    public DependencySet resolveTestDependencies(ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(retriever, repositories,
            new Scope[]{Scope.test},
            new Scope[]{Scope.compile, Scope.runtime},
            null);
    }

    private DependencySet resolveScopedDependencies(ArtifactRetriever retriever, List<Repository> repositories, Scope[] resolvedScopes, Scope[] transitiveScopes, DependencySet excluded) {
        var dependencies = new DependencySet();
        for (var scope : resolvedScopes) {
            var scoped_dependencies = get(scope);
            if (scoped_dependencies != null) {
                for (var dependency : scoped_dependencies) {
                    dependencies.addAll(new DependencyResolver(retriever, repositories, dependency).getAllDependencies(transitiveScopes));
                }
            }
        }
        if (excluded != null) {
            dependencies.removeAll(excluded);
        }
        return dependencies;
    }
}
