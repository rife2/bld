/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.dependencies.*;

import java.util.ArrayList;
import java.util.List;

import static rife.bld.dependencies.Scope.compile;
import static rife.bld.dependencies.Scope.runtime;

/**
 * Transitively generates a hierarchical tree of dependencies.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.21
 */
public class DependencyTreeOperation extends AbstractOperation<DependencyTreeOperation> {
    private ArtifactRetriever retriever_ = null;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencyScopes dependencies_ = new DependencyScopes();
    private final StringBuilder dependencyTree_ = new StringBuilder();

    /**
     * Performs the dependency tree operation.
     *
     * @since 1.5.21
     */
    public void execute() {
        var compile_tree = executeGenerateCompileDependencies();
        var runtime_tree = executeGenerateRuntimeDependencies();
        dependencyTree_.setLength(0);
        dependencyTree_.append(compile_tree);
        dependencyTree_.append(System.lineSeparator());
        dependencyTree_.append(runtime_tree);
        dependencyTree_.append(System.lineSeparator());

        System.out.println(compile_tree);
        System.out.println(runtime_tree);
    }

    /**
     * Part of the {@link #execute} operation, generates the tree for the compile scope.
     *
     * @since 1.5.21
     */
    protected String executeGenerateCompileDependencies() {
        var compile_tree = dependencies().scope(compile).generateTransitiveDependencyTree(artifactRetriever(), repositories(), compile);
        if (compile_tree.isEmpty()) {
            compile_tree = "no dependencies" + System.lineSeparator();
        }
        return "compile:" + System.lineSeparator() + compile_tree;
    }

    /**
     * Part of the {@link #execute} operation, generates the tree for the runtime scope.
     *
     * @since 1.5.21
     */
    protected String executeGenerateRuntimeDependencies() {
        var runtime_tree = dependencies().scope(runtime).generateTransitiveDependencyTree(artifactRetriever(), repositories(), compile, runtime);
        if (runtime_tree.isEmpty()) {
            runtime_tree = "no dependencies" + System.lineSeparator();
        }
        return "runtime:" + System.lineSeparator() + runtime_tree;
    }


    /**
     * Configures a dependency tree operation from a {@link BaseProject}.
     *
     * @param project the project to configure the operation from
     * @since 1.5.21
     */
    public DependencyTreeOperation fromProject(BaseProject project) {
        return artifactRetriever(project.artifactRetriever())
            .repositories(project.repositories())
            .dependencies(project.dependencies());
    }

    /**
     * Provides repositories to resolve the dependencies against.
     *
     * @param repositories repositories against which dependencies will be resolved
     * @return this operation instance
     * @since 1.5.21
     */
    public DependencyTreeOperation repositories(Repository... repositories) {
        repositories_.addAll(List.of(repositories));
        return this;
    }

    /**
     * Provides a list of repositories to resolve the dependencies against.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param repositories a list of repositories against which dependencies will be resolved
     * @return this operation instance
     * @since 1.5.21
     */
    public DependencyTreeOperation repositories(List<Repository> repositories) {
        repositories_.addAll(repositories);
        return this;
    }

    /**
     * Provides scoped dependencies to generate a tree for.
     *
     * @param dependencies the dependencies that will be resolved for tree generation
     * @return this operation instance
     * @since 1.5.21
     */
    public DependencyTreeOperation dependencies(DependencyScopes dependencies) {
        dependencies_.include(dependencies);
        return this;
    }

    /**
     * Provides the artifact retriever to use.
     *
     * @param retriever the artifact retriever
     * @return this operation instance
     * @since 1.5.21
     */
    public DependencyTreeOperation artifactRetriever(ArtifactRetriever retriever) {
        retriever_ = retriever;
        return this;
    }

    /**
     * Retrieves the repositories in which the dependencies will be resolved.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the repositories used for dependency resolution
     * @since 1.5.21
     */
    public List<Repository> repositories() {
        return repositories_;
    }

    /**
     * Retrieves the scoped dependencies that will be used for tree generation.
     * <p>
     * This is a modifiable structure that can be retrieved and changed.
     *
     * @return the scoped dependencies
     * @since 1.5.21
     */
    public DependencyScopes dependencies() {
        return dependencies_;
    }

    /**
     * Returns the artifact retriever that is used.
     *
     * @return the artifact retriever
     * @since 1.5.21
     */
    public ArtifactRetriever artifactRetriever() {
        if (retriever_ == null) {
            return ArtifactRetriever.instance();
        }
        return retriever_;
    }

    /**
     * Returns the last generated dependency tree.
     *
     * @return the last generated dependency tree
     * @since 1.5.21
     */
    public String dependencyTree() {
        return dependencyTree_.toString();
    }
}
