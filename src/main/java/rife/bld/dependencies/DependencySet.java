/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.bld.dependencies.exceptions.DependencyTransferException;

import java.io.File;
import java.util.*;

/**
 * Convenience class to handle a set of {@link Dependency} objects.
 * <p>
 * Only a single version of each dependency can exist in this set.
 * When adding a new dependency, it will only be added if it didn't exist
 * in the set yet, or if the new dependency has a higher version than
 * the existing one.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class DependencySet extends AbstractSet<Dependency> implements Set<Dependency> {
    private final Map<Dependency, Dependency> dependencies_ = new LinkedHashMap<>();
    private final Set<LocalDependency> localDependencies_ = new LinkedHashSet<>();
    private final Set<LocalModule> localModules_ = new LinkedHashSet<>();
    private final Set<Bom> boms_ = new LinkedHashSet<>();

    /**
     * Creates an empty dependency set.
     *
     * @since 1.5
     */
    public DependencySet() {
    }

    /**
     * Creates a dependency set from another one.
     *
     * @param other the other set to create this one from
     * @since 1.5
     */
    public DependencySet(DependencySet other) {
        include(other);
    }

    /**
     * Includes all the dependencies, local dependencies, local modules and
     * BOMs from another dependency set.
     *
     * @param other the other set to include
     * @return this dependency set instance
     * @since 2.4.0
     */
    public DependencySet include(DependencySet other) {
        addAll(other);
        localDependencies_.addAll(other.localDependencies_);
        localModules_.addAll(other.localModules_);
        boms_.addAll(other.boms_);
        return this;
    }

    /**
     * Includes a dependency into the dependency set.
     *
     * @param dependency the dependency to include
     * @return this dependency set instance
     * @since 1.5
     */
    public DependencySet include(Dependency dependency) {
        add(dependency);
        return this;
    }

    /**
     * Includes a local dependency into the dependency set.
     * <p>
     * Local dependencies aren't resolved and point to a location on
     * the file system.
     *
     * @param dependency the dependency to include
     * @return this dependency set instance
     * @since 1.5.2
     */
    public DependencySet include(LocalDependency dependency) {
        localDependencies_.add(dependency);
        return this;
    }

    /**
     * Retrieves the local dependencies.
     *
     * @return the set of local dependencies
     * @since 1.5.2
     */
    public Set<LocalDependency> localDependencies() {
        return localDependencies_;
    }

    /**
     * Includes a bill of materials in the dependency set.
     * <p>
     * BOMs aren't transferred, their dependency management sections supply
     * versions during the resolution of this dependency set.
     *
     * @param bom the BOM to include
     * @return this dependency set instance
     * @since 2.4.0
     */
    public DependencySet include(Bom bom) {
        boms_.add(bom);
        return this;
    }

    /**
     * Retrieves the bills of materials.
     *
     * @return the set of BOMs
     * @since 2.4.0
     */
    public Set<Bom> boms() {
        return boms_;
    }

    /**
     * Includes a local module in the dependency set.
     * <p>
     * Local modules aren't resolved and point to a location on
     * the file system.
     *
     * @param module the module to include
     * @return this dependency set instance
     * @since 2.1
     */
    public DependencySet include(LocalModule module) {
        localModules_.add(module);
        return this;
    }

    /**
     * Retrieves the local modules.
     *
     * @return the set of local modules
     * @since 2.1
     */
    public Set<LocalModule> localModules() {
        return localModules_;
    }

    /**
     * Transfers the artifacts for the dependencies into the provided directory.
     * <p>
     * The destination directory must exist and be writable.
     *
     * @param resolution        the version resolution state that can be cached
     * @param retriever         the retriever to use to get artifacts
     * @param repositories      the repositories to use for the transfer
     * @param directory         the directory to transfer the artifacts into
     * @param modulesDirectory  the directory to download the modules into
     * @return the list of artifacts that were transferred successfully
     * @throws DependencyTransferException when an error occurred during the transfer
     * @since 2.1
     */
    public List<RepositoryArtifact> transferIntoDirectory(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories, File directory, File modulesDirectory) {
        return transferIntoDirectory(resolution, retriever, repositories, directory, modulesDirectory, (String[]) null);
    }

    /**
     * Transfers the artifacts for the dependencies into the provided directories,
     * including other classifiers.
     * <p>
     * The destination directory must exist and be writable.
     * <p>
     * The artifacts of different dependencies are transferred in parallel,
     * the {@value VersionResolution#PROPERTY_TRANSFER_PARALLELISM} property
     * can be used to change the number of simultaneous transfers, setting it
     * to {@code 1} makes the transfers sequential.
     *
     * @param resolution        the version resolution state that can be cached
     * @param retriever         the retriever to use to get artifacts
     * @param repositories      the repositories to use for the download
     * @param directory         the directory to download the artifacts into
     * @param modulesDirectory  the directory to download the modules into
     * @param classifiers       the additional classifiers to transfer
     * @return the list of artifacts that were transferred successfully
     * @throws DependencyTransferException when an error occurred during the transfer
     * @since 2.1
     */
    public List<RepositoryArtifact> transferIntoDirectory(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories, File directory, File modulesDirectory, String... classifiers) {
        return new DependencyTransferBatch()
            .add(this, directory, modulesDirectory, classifiers)
            .transfer(resolution, retriever, repositories);
    }

    /**
     * Returns the dependency that was stored in the set.
     * <p>
     * The version can be different from the dependency passed in, and this
     * method can be used to look up the actual version of the dependency in the set.
     *
     * @param dependency the dependency to look for
     * @return the dependency in the set; or
     * {@code null} if no such dependency exists
     * @since 1.5
     */
    public Dependency get(Dependency dependency) {
        return dependencies_.get(dependency);
    }

    /**
     * Generates the string description of the transitive hierarchical tree of
     * dependencies for a particular scope.
     *
     * @param resolution   the version resolution state that can be cached
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to look for dependencies in
     * @param scopes       the scopes to return the transitive dependencies for
     * @return the generated tree description string; or an empty string if
     * there were no dependencies to describe
     * @since 2.0
     */
    public String generateTransitiveDependencyTree(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories, Scope... scopes) {
        resolution = resolution.withBoms(retriever, repositories, boms_);
        return new ParallelDependencyResolver(resolution, retriever, repositories).resolveAllDependencies(this, scopes).generateDependencyTree();
    }

    /**
     * Generates the string description of the hierarchical tree of
     * dependencies in this {@code DependencySet}. This relies on the {@code Dependency}
     * {@code parent} field to be set correctly to indicate their relationships.
     *
     * @return the generated tree description string; or an empty string if
     * there were no dependencies to describe
     * @since 1.5.21
     */
    public String generateDependencyTree() {
        var result = new StringBuilder();

        var dependency_list = new ArrayList<>(this);
        var dependency_stack = new Stack<DependencyTreeEntry>();

        var roots = dependency_list.stream().filter(dependency -> dependency.parent() == null).toList();
        dependency_list.removeIf(dependency -> dependency.parent() == null);

        var roots_it = roots.iterator();
        while (roots_it.hasNext()) {
            var root = roots_it.next();

            dependency_list.add(0, root);
            stack:
            do {
                var list_it = dependency_list.iterator();
                while (list_it.hasNext()) {
                    var list_dep = list_it.next();
                    if (list_dep.parent() == null) {
                        list_it.remove();
                        var entry = new DependencyTreeEntry(null, list_dep, !roots_it.hasNext());
                        result.append(entry).append(System.lineSeparator());
                        dependency_stack.add(entry);
                    } else {
                        var stack_entry = dependency_stack.peek();
                        if (list_dep.parent().equals(stack_entry.dependency())) {
                            list_it.remove();

                            boolean last = dependency_list.stream().noneMatch(d -> d.parent().equals(stack_entry.dependency()));
                            var entry = new DependencyTreeEntry(stack_entry, list_dep, last);
                            result.append(entry).append(System.lineSeparator());
                            dependency_stack.add(entry);
                            continue stack;
                        }
                    }
                }
                dependency_stack.pop();
            } while (!dependency_stack.isEmpty());
        }

        return result.toString();
    }

    private record DependencyTreeEntry(DependencyTreeEntry parent, Dependency dependency, boolean last) {
        public String toString() {
            var result = new StringBuilder();

            if (last) {
                result.insert(0, "└─ ");
            } else {
                result.insert(0, "├─ ");
            }

            var p = parent();
            while (p != null) {
                if (p.last()) {
                    result.insert(0, "   ");
                } else {
                    result.insert(0, "│  ");
                }
                p = p.parent();
            }

            return result.toString() + dependency;
        }
    }

    public boolean add(Dependency dependency) {
        var existing = dependencies_.get(dependency);
        if (existing == null) {
            dependencies_.put(dependency, dependency);
            return true;
        }
        if (dependency.version().compareTo(existing.version()) > 0) {
            dependencies_.remove(dependency);
            dependencies_.put(dependency, dependency);
            return true;
        }
        return false;
    }

    public Iterator<Dependency> iterator() {
        return dependencies_.keySet().iterator();
    }

    public int size() {
        return dependencies_.size();
    }
}