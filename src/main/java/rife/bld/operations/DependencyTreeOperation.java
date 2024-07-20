/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.BldCache;
import rife.bld.BldVersion;
import rife.bld.BuildExecutor;
import rife.bld.dependencies.*;
import rife.bld.wrapper.Wrapper;
import rife.ioc.HierarchicalProperties;

import java.io.File;
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
    private boolean offline_ = false;
    private HierarchicalProperties properties_ = null;
    private HierarchicalProperties extensionProperties_ = null;
    private ArtifactRetriever retriever_ = null;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencyScopes dependencies_ = new DependencyScopes();
    private final List<Repository> extensionRepositories_ = new ArrayList<>();
    private final DependencyScopes extensionDependencies_ = new DependencyScopes();

    private final StringBuilder dependencyTree_ = new StringBuilder();
    private File libBldDir_ = null;

    /**
     * Performs the dependency tree operation.
     *
     * @since 1.5.21
     */
    public void execute() {
        if (offline_) {
            System.out.println("Offline mode: dependency-tree is disabled");
            return;
        }

        // calculate the dependency tree of the extensions, using the cache if possible

        String extensions_tree = null;
        BldCache extensions_cache = null;
        if (libBldDir_ != null) {
            extensions_cache =  new BldCache(libBldDir_, new VersionResolution(extensionProperties()));
            extensions_cache.fingerprintExtensions(
                extensionRepositories().stream().map(Repository::toString).toList(),
                extensionDependencies().scope(compile).stream().map(Dependency::toString).toList());
            if (extensions_cache.isExtensionHashValid()) {
                var cached_tree = extensions_cache.getCachedExtensionsDependencyTree();
                if (cached_tree != null) {
                    extensions_tree = cached_tree;
                }
            }
        }

        if (extensions_tree == null) {
            extensions_tree = executeGenerateExtensionsDependencies();
            if (extensions_cache != null) {
                extensions_cache.cacheExtensionsDependencyTree(extensions_tree);
                extensions_cache.writeCache();
            }
        }

        // calculate the dependency tree of the dependencies, using the cache if possible

        String compile_tree = null;
        String provided_tree = null;
        String runtime_tree = null;
        String test_tree = null;
        BldCache dependencies_cache = null;
        if (libBldDir_ != null) {
            dependencies_cache =  new BldCache(libBldDir_, new VersionResolution(properties()));
            dependencies_cache.fingerprintDependencies(repositories(), dependencies());
            if (dependencies_cache.isDependenciesHashValid()) {
                var cached_compile_tree = dependencies_cache.getCachedDependenciesCompileDependencyTree();
                if (cached_compile_tree != null) {
                    compile_tree = cached_compile_tree;
                }
                var cached_provided_tree = dependencies_cache.getCachedDependenciesProvidedDependencyTree();
                if (cached_provided_tree != null) {
                    provided_tree = cached_provided_tree;
                }
                var cached_runtime_tree = dependencies_cache.getCachedDependenciesRuntimeDependencyTree();
                if (cached_runtime_tree != null) {
                    runtime_tree = cached_runtime_tree;
                }
                var cached_test_tree = dependencies_cache.getCachedDependenciesTestDependencyTree();
                if (cached_test_tree != null) {
                    test_tree = cached_test_tree;
                }
            }
        }
        
        var write_dependencies_cache = false;
        if (compile_tree == null) {
            compile_tree = executeGenerateCompileDependencies();
            if (dependencies_cache != null) {
                dependencies_cache.cacheDependenciesCompileDependencyTree(compile_tree);
                write_dependencies_cache = true;
            }
        }
        if (provided_tree == null) {
            provided_tree = executeGenerateProvidedDependencies();
            if (dependencies_cache != null) {
                dependencies_cache.cacheDependenciesProvidedDependencyTree(provided_tree);
                write_dependencies_cache = true;
            }
        }
        if (runtime_tree == null) {
            runtime_tree = executeGenerateRuntimeDependencies();
            if (dependencies_cache != null) {
                dependencies_cache.cacheDependenciesRuntimeDependencyTree(runtime_tree);
                write_dependencies_cache = true;
            }
        }
        if (test_tree == null) {
            test_tree = executeGenerateTestDependencies();
            if (dependencies_cache != null) {
                dependencies_cache.cacheDependenciesTestDependencyTree(test_tree);
                write_dependencies_cache = true;
            }
        }

        if (write_dependencies_cache) {
            dependencies_cache.writeCache();
        }

        // output the dependency trees

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
        var extensions_tree = extensionDependencies().scope(compile).generateTransitiveDependencyTree(new VersionResolution(extensionProperties()), artifactRetriever(), extensionRepositories(), compile, runtime);
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
        var compile_tree = dependencies().scope(compile).generateTransitiveDependencyTree(new VersionResolution(properties()), artifactRetriever(), repositories(), compile);
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
        var provided_tree = dependencies().scope(provided).generateTransitiveDependencyTree(new VersionResolution(properties()), artifactRetriever(), repositories(), compile, runtime);
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
        var runtime_tree = dependencies().scope(runtime).generateTransitiveDependencyTree(new VersionResolution(properties()), artifactRetriever(), repositories(), compile, runtime);
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
        var test_tree = dependencies().scope(test).generateTransitiveDependencyTree(new VersionResolution(properties()), artifactRetriever(), repositories(), compile, runtime);
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
        libBldDir_ = project.libBldDirectory();

        // add the repositories and dependencies from the extensions
        var wrapper = new Wrapper();
        wrapper.currentDir(project.workDirectory());
        try {
            wrapper.initWrapperProperties(BldVersion.getVersion());

            var extension_properties = BuildExecutor.setupProperties(project.workDirectory());
            extension_properties = new HierarchicalProperties().parent(extension_properties);
            extension_properties.putAll(wrapper.wrapperProperties());
            extensionProperties(extension_properties);

            for (var repository : wrapper.repositories()) {
                extensionRepositories().add(Repository.resolveRepository(extensionProperties(), repository));
            }
            extensionDependencies().scope(compile).addAll(wrapper.extensions().stream().map(Dependency::parse).toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // add the repositories and the dependencies from the project
        return offline(project.offline())
            .properties(project.properties())
            .artifactRetriever(project.artifactRetriever())
            .repositories(project.repositories())
            .dependencies(project.dependencies());
    }

    /**
     * Indicates whether the operation has to run offline.
     *
     * @param flag {@code true} if the operation runs offline; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 2.0
     */
    public DependencyTreeOperation offline(boolean flag) {
        offline_ = flag;
        return this;
    }

    /**
     * Returns whether the operation has to run offline.
     *
     * @return {@code true} if the operation runs offline; or
     *         {@code false} otherwise
     * @since 2.0
     */
    public boolean offline() {
        return offline_;
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
     * Provides the hierarchical properties to use.
     *
     * @param properties the hierarchical properties
     * @return this operation instance
     * @since 2.0
     */
    public DependencyTreeOperation properties(HierarchicalProperties properties) {
        properties_ = properties;
        return this;
    }

    /**
     * Provides the extension hierarchical properties to use.
     *
     * @param properties the extension hierarchical properties
     * @return this operation instance
     * @since 2.0
     */
    public DependencyTreeOperation extensionProperties(HierarchicalProperties properties) {
        extensionProperties_ = properties;
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

    /**
     * Returns the hierarchical properties that are used.
     *
     * @return the hierarchical properties
     * @since 2.0
     */
    public HierarchicalProperties properties() {
        if (properties_ == null) {
            properties_ = new HierarchicalProperties();
        }
        return properties_;
    }

    /**
     * Returns the extension hierarchical properties that are used.
     *
     * @return the extension hierarchical properties
     * @since 2.0
     */
    public HierarchicalProperties extensionProperties() {
        if (extensionProperties_ == null) {
            extensionProperties_ = new HierarchicalProperties();
        }
        return extensionProperties_;
    }
}
