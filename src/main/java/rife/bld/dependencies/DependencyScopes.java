/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.ioc.HierarchicalProperties;

import java.util.ArrayList;
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
     * Includes all the dependencies, local dependencies, local modules and
     * BOMs from another dependency scope map.
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
            dependencies.include(entry.getValue());
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
     * Returns the version-less dependencies that are not covered by the
     * BOMs that are declared in their scope.
     * <p>
     * Scopes that don't declare any BOMs are not considered, their
     * version-less dependencies simply resolve to the latest version.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the BOM resolution
     * @return the version-less dependencies that no BOM in their scope covers
     * @since 2.4.0
     */
    public List<Dependency> versionlessDependenciesWithoutBom(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        var result = new ArrayList<Dependency>();
        for (var scoped_dependencies : values()) {
            if (scoped_dependencies.boms().isEmpty()) {
                continue;
            }
            var resolution = new VersionResolution(properties, retriever, repositories, scoped_dependencies.boms());
            for (var dependency : scoped_dependencies) {
                if (dependency.version().equals(VersionNumber.UNKNOWN) &&
                    !resolution.bomVersions().containsKey(dependency.toArtifactString()) &&
                    !result.contains(dependency)) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    /**
     * Returns the transitive set of dependencies that would be used for the compile scope in a project.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the compile scope dependency set
     * @since 2.0
     */
    public DependencySet resolveCompileDependencies(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(properties, retriever, repositories,
            new Scope[]{Scope.compile},
            new Scope[]{Scope.compile},
            null);
    }

    /**
     * Returns the transitive set of dependencies that would be used for the provided scope in a project.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the provided scope dependency set
     * @since 2.0
     */
    public DependencySet resolveProvidedDependencies(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(properties, retriever, repositories,
            new Scope[]{Scope.provided},
            new Scope[]{Scope.compile, Scope.runtime},
            null);
    }

    /**
     * Returns the transitive set of dependencies that would be used for the runtime scope in a project.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the runtime scope dependency set
     * @since 2.0
     */
    public DependencySet resolveRuntimeDependencies(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(properties, retriever, repositories,
            new Scope[]{Scope.compile, Scope.runtime},
            new Scope[]{Scope.compile, Scope.runtime},
            resolveCompileDependencies(properties, retriever, repositories));
    }

    /**
     * Returns the transitive set of dependencies that would be used for the standalone scope in a project.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the standalone scope dependency set
     * @since 2.0
     */
    public DependencySet resolveStandaloneDependencies(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(properties, retriever, repositories,
            new Scope[]{Scope.standalone},
            new Scope[]{Scope.compile, Scope.runtime},
            null);
    }

    /**
     * Returns the transitive set of dependencies that would be used for the test scope in a project.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @return the test scope dependency set
     * @since 2.0
     */
    public DependencySet resolveTestDependencies(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        return resolveScopedDependencies(properties, retriever, repositories,
            new Scope[]{Scope.test},
            new Scope[]{Scope.compile, Scope.runtime},
            null);
    }

    private DependencySet resolveScopedDependencies(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories, Scope[] resolvedScopes, Scope[] transitiveScopes, DependencySet excluded) {
        var roots = new ArrayList<Dependency>();
        var boms = new ArrayList<Bom>();
        for (var scope : resolvedScopes) {
            var scoped_dependencies = get(scope);
            if (scoped_dependencies != null) {
                roots.addAll(scoped_dependencies);
                boms.addAll(scoped_dependencies.boms());
            }
        }
        var resolution = new VersionResolution(properties, retriever, repositories, boms);
        var dependencies = new ParallelDependencyResolver(resolution, retriever, repositories).resolveAllDependencies(roots, transitiveScopes);
        if (excluded != null) {
            dependencies.removeAll(excluded);
        }
        return dependencies;
    }
}
