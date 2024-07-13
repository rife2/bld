/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.BldVersion;
import rife.bld.dependencies.*;
import rife.bld.wrapper.Wrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static rife.bld.dependencies.Scope.*;

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
    private final List<Repository> extensionRepositories_ = new ArrayList<>();
    private final DependencyScopes extensionDependencies_ = new DependencyScopes();

    private final StringBuilder dependencyTree_ = new StringBuilder();

    /**
     * Performs the dependency tree operation.
     *
     * @since 1.5.21
     */
    public void execute() {
        var extensions_tree = executeGenerateExtensionsDependencies();
        var compile_tree = executeGenerateCompileDependencies();
        var provided_tree = executeGenerateProvidedDependencies();
        var runtime_tree = executeGenerateRuntimeDependencies();
        var test_tree = executeGenerateTestDependencies();
        dependencyTree_.setLength(0);
        dependencyTree_.append(extensions_tree);
        dependencyTree_.append(System.lineSeparator());
        dependencyTree_.append(compile_tree);
        dependencyTree_.append(System.lineSeparator());
        dependencyTree_.append(provided_tree);
        dependencyTree_.append(System.lineSeparator());
        dependencyTree_.append(runtime_tree);
        dependencyTree_.append(System.lineSeparator());
        dependencyTree_.append(test_tree);
        dependencyTree_.append(System.lineSeparator());

        System.out.println(extensions_tree);
        System.out.println(compile_tree);
        System.out.println(provided_tree);
        System.out.println(runtime_tree);
        System.out.println(test_tree);
    }

    /**
     * Part of the {@link #execute} operation, generates the tree for the extensions.
     *
     * @since 2.0
     */
    protected String executeGenerateExtensionsDependencies() {
        var extensions_tree = extensionDependencies().scope(compile).generateTransitiveDependencyTree(artifactRetriever(), extensionRepositories(), compile, runtime);
        if (extensions_tree.isEmpty()) {
            extensions_tree = "no dependencies" + System.lineSeparator();
        }
        return "extensions:" + System.lineSeparator() + extensions_tree;
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
     * Part of the {@link #execute} operation, generates the tree for the provided scope.
     *
     * @since 1.7.3
     */
    protected String executeGenerateProvidedDependencies() {
        var provided_tree = dependencies().scope(provided).generateTransitiveDependencyTree(artifactRetriever(), repositories(), compile, runtime);
        if (provided_tree.isEmpty()) {
            provided_tree = "no dependencies" + System.lineSeparator();
        }
        return "provided:" + System.lineSeparator() + provided_tree;
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
     * Part of the {@link #execute} operation, generates the tree for the test scope.
     *
     * @since 1.7.3
     */
    protected String executeGenerateTestDependencies() {
        var test_tree = dependencies().scope(test).generateTransitiveDependencyTree(artifactRetriever(), repositories(), compile, runtime);
        if (test_tree.isEmpty()) {
            test_tree = "no dependencies" + System.lineSeparator();
        }
        return "test:" + System.lineSeparator() + test_tree;
    }


    /**
     * Configures a dependency tree operation from a {@link BaseProject}.
     *
     * @param project the project to configure the operation from
     * @since 1.5.21
     */
    public DependencyTreeOperation fromProject(BaseProject project) {
        // add the repositories and dependencies from the extensions
        var wrapper = new Wrapper();
        wrapper.currentDir(project.workDirectory());
        try {
            wrapper.initWrapperProperties(BldVersion.getVersion());
            for (var repository : wrapper.repositories()) {
                extensionRepositories().add(Repository.resolveRepository(project.properties(), repository));
            }
            extensionDependencies().scope(compile).addAll(wrapper.extensions().stream().map(Dependency::parse).toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // add the repositories and the dependencies from the project
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
     * Provides extension repositories to resolve the extension dependencies against.
     *
     * @param repositories extension repositories against which extension dependencies will be resolved
     * @return this operation instance
     * @since 2.0
     */
    public DependencyTreeOperation extensionRepositories(Repository... repositories) {
        extensionRepositories_.addAll(List.of(repositories));
        return this;
    }

    /**
     * Provides a list of extension repositories to resolve the extension dependencies against.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param repositories a list of extension repositories against which extension dependencies will be resolved
     * @return this operation instance
     * @since 2.0
     */
    public DependencyTreeOperation extensionRepositories(List<Repository> repositories) {
        extensionRepositories_.addAll(repositories);
        return this;
    }

    /**
     * Provides scoped extension dependencies to generate a tree for.
     *
     * @param dependencies the extension dependencies that will be resolved for tree generation
     * @return this operation instance
     * @since 2.0
     */
    public DependencyTreeOperation extensionDependencies(DependencyScopes dependencies) {
        extensionDependencies_.include(dependencies);
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
     * Retrieves the extension repositories in which the dependencies will be resolved.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the extension repositories used for dependency resolution
     * @since 2.0
     */
    public List<Repository> extensionRepositories() {
        return extensionRepositories_;
    }

    /**
     * Retrieves the scoped extension dependencies that will be used for tree generation.
     * <p>
     * This is a modifiable structure that can be retrieved and changed.
     *
     * @return the scoped extension dependencies
     * @since 2.0
     */
    public DependencyScopes extensionDependencies() {
        return extensionDependencies_;
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
