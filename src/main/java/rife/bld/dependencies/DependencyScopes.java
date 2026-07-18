/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.ioc.HierarchicalProperties;

import java.util.ArrayList;
import java.util.HashSet;
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
     * Returns the BOMs that apply to the version resolution of a particular
     * scope.
     * <p>
     * BOMs follow the same scope composition as the classpaths: the
     * {@code compile} scope BOMs also apply to the {@code provided},
     * {@code runtime} and {@code test} scopes, and the {@code runtime} and
     * {@code provided} scope BOMs also apply to the {@code test} scope.
     * The BOMs are returned with the scope's own BOMs first, followed by
     * the inherited ones in the standard scope order, for the {@code test}
     * scope that is {@code test}, {@code compile}, {@code provided},
     * {@code runtime}. This order determines their precedence when several
     * manage the same dependency: a BOM declared in the scope where a
     * dependency is used takes precedence over a BOM that is inherited from
     * another scope, and among the inherited BOMs the more fundamental
     * scope wins.
     * <p>
     * The {@code standalone} scope only uses its own BOMs, and its BOMs
     * deliberately never apply to any other scope.
     *
     * @param scope the scope to retrieve the applicable BOMs for
     * @return the BOMs that apply to the scope's version resolution
     * @since 2.4.0
     */
    public List<Bom> effectiveBoms(Scope scope) {
        var boms = new ArrayList<Bom>();
        for (var bom_scope : bomScopes(scope)) {
            var scoped_dependencies = get(bom_scope);
            if (scoped_dependencies != null) {
                boms.addAll(scoped_dependencies.boms());
            }
        }
        return boms;
    }

    private static Scope[] bomScopes(Scope scope) {
        return switch (scope) {
            case provided -> new Scope[]{Scope.provided, Scope.compile};
            case runtime -> new Scope[]{Scope.runtime, Scope.compile};
            case test -> new Scope[]{Scope.test, Scope.compile, Scope.provided, Scope.runtime};
            default -> new Scope[]{scope};
        };
    }

    /**
     * Returns the version-less dependencies that are not covered by the
     * BOMs that apply to their scope.
     * <p>
     * Scopes that no BOMs apply to are not considered, their
     * version-less dependencies simply resolve to the latest version.
     * Dependencies whose version is supplied by a {@code bld.override}
     * property are not reported either.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the BOM resolution
     * @return the version-less dependencies that no applicable BOM covers
     * @since 2.4.0
     */
    public List<Dependency> versionlessDependenciesWithoutBom(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        var result = new ArrayList<Dependency>();
        for (var entry : entrySet()) {
            var effective_boms = effectiveBoms(entry.getKey());
            if (effective_boms.isEmpty()) {
                continue;
            }
            var resolution = new VersionResolution(properties, retriever, repositories, effective_boms);
            for (var dependency : entry.getValue()) {
                if (dependency.version().equals(VersionNumber.UNKNOWN) &&
                    !resolution.coversDependency(dependency) &&
                    !resolution.versionOverrides().containsKey(dependency.toArtifactString()) &&
                    !result.contains(dependency)) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    /**
     * Returns the version conflicts between the BOMs that apply to the
     * scopes, where more than one applicable BOM manages the same
     * dependency at a different version.
     * <p>
     * Each conflict is reported once, the versions are keyed by their BOM
     * in precedence order so that the first is the version that is used.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the BOM resolution
     * @return the version conflicts between the applicable BOMs
     * @since 2.4.0
     */
    public List<VersionResolution.BomVersionConflict> bomVersionConflicts(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        var result = new ArrayList<VersionResolution.BomVersionConflict>();
        var seen = new HashSet<String>();
        for (var scope : keySet()) {
            var effective_boms = effectiveBoms(scope);
            if (effective_boms.size() < 2) {
                continue;
            }
            for (var conflict : VersionResolution.resolveBomVersionConflicts(properties, retriever, repositories, effective_boms)) {
                if (seen.add(conflict.dependency() + conflict.bomVersions())) {
                    result.add(conflict);
                }
            }
        }
        return result;
    }

    /**
     * Returns the declared dependencies whose explicit version differs
     * from the version that a BOM applying to their scope manages them at.
     * <p>
     * The declared version is used for the dependency itself, its
     * transitive dependencies still resolve to the versions that the BOMs
     * manage. Dependencies whose version is supplied by a
     * {@code bld.override} property are not reported. Each difference is
     * reported once.
     *
     * @param properties   the properties to use to get artifacts
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the BOM resolution
     * @return the version differences between the declared dependencies
     * and the applicable BOMs
     * @since 2.4.0
     */
    public List<VersionResolution.DeclaredVersionConflict> declaredVersionConflicts(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories) {
        var result = new ArrayList<VersionResolution.DeclaredVersionConflict>();
        var seen = new HashSet<String>();
        for (var entry : entrySet()) {
            var effective_boms = effectiveBoms(entry.getKey());
            if (effective_boms.isEmpty()) {
                continue;
            }
            for (var conflict : VersionResolution.resolveDeclaredVersionConflicts(properties, retriever, repositories, effective_boms, entry.getValue())) {
                if (seen.add(conflict.dependency() + conflict.declaredVersion() + conflict.bom() + conflict.bomVersion())) {
                    result.add(conflict);
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
            Scope.compile,
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
            Scope.provided,
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
            Scope.runtime,
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
            Scope.standalone,
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
            Scope.test,
            new Scope[]{Scope.test},
            new Scope[]{Scope.compile, Scope.runtime},
            null);
    }

    private DependencySet resolveScopedDependencies(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories, Scope scope, Scope[] resolvedScopes, Scope[] transitiveScopes, DependencySet excluded) {
        var roots = new ArrayList<Dependency>();
        for (var resolved_scope : resolvedScopes) {
            var scoped_dependencies = get(resolved_scope);
            if (scoped_dependencies != null) {
                roots.addAll(scoped_dependencies);
            }
        }
        var resolution = new VersionResolution(properties, retriever, repositories, effectiveBoms(scope));
        var dependencies = new ParallelDependencyResolver(resolution, retriever, repositories).resolveAllDependencies(roots, transitiveScopes);
        if (excluded != null) {
            dependencies.removeAll(excluded);
        }
        return dependencies;
    }
}
