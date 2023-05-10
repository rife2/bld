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
        addAll(other);
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
     * Transfers the artifacts for the dependencies into the provided directory.
     * <p>
     * The destination directory must exist and be writable.
     *
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the transfer
     * @param directory    the directory to transfer the artifacts into
     * @return the list of artifacts that were transferred successfully
     * @throws DependencyTransferException when an error occurred during the transfer
     * @since 1.5.10
     */
    public List<RepositoryArtifact> transferIntoDirectory(ArtifactRetriever retriever, List<Repository> repositories, File directory) {
        return transferIntoDirectory(retriever, repositories, directory, (String[]) null);
    }

    /**
     * Transfers the artifacts for the dependencies into the provided directory,
     * including other classifiers.
     * <p>
     * The destination directory must exist and be writable.
     *
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the download
     * @param directory    the directory to download the artifacts into
     * @param classifiers  the additional classifiers to transfer
     * @return the list of artifacts that were transferred successfully
     * @throws DependencyTransferException when an error occurred during the transfer
     * @since 1.5.10
     */
    public List<RepositoryArtifact> transferIntoDirectory(ArtifactRetriever retriever, List<Repository> repositories, File directory, String... classifiers) {
        var result = new ArrayList<RepositoryArtifact>();
        for (var dependency : this) {
            var artifact = new DependencyResolver(retriever, repositories, dependency).transferIntoDirectory(directory);
            if (artifact != null) {
                result.add(artifact);
            }

            if (classifiers != null) {
                for (var classifier : classifiers) {
                    if (classifier != null) {
                        var classifier_artifact = new DependencyResolver(retriever, repositories, dependency.withClassifier(classifier)).transferIntoDirectory(directory);
                        if (classifier_artifact != null) {
                            result.add(classifier_artifact);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the dependency that was stored in the set.
     * <p>
     * The version can be different from the dependency passed in and this
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
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to look for dependencies in
     * @param scopes       the scopes to return the transitive dependencies for
     * @return the generated tree description string; or an empty string if
     * there were no dependencies to describe
     * @since 1.5.21
     */
    public String generateTransitiveDependencyTree(ArtifactRetriever retriever, List<Repository> repositories, Scope... scopes) {
        var compile_dependencies = new DependencySet();
        for (var dependency : this) {
            compile_dependencies.addAll(new DependencyResolver(retriever, repositories, dependency).getAllDependencies(scopes));
        }
        return compile_dependencies.generateDependencyTree();
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