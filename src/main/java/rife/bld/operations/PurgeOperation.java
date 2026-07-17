/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.dependencies.*;
import rife.ioc.HierarchicalProperties;

import java.io.File;
import java.util.*;

import static rife.bld.dependencies.Dependency.CLASSIFIER_JAVADOC;
import static rife.bld.dependencies.Dependency.CLASSIFIER_SOURCES;

/**
 * Transitively checks all the artifacts for dependencies in the directories
 * that are separated out by scope, any files that aren't required will be deleted.
 * <p>
 * If a directory is not provided, no purge will occur for that
 * dependency scope.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class PurgeOperation extends AbstractOperation<PurgeOperation> {
    private boolean offline_ = false;
    private HierarchicalProperties properties_ = null;
    private ArtifactRetriever retriever_ = null;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencyScopes dependencies_ = new DependencyScopes();
    private File libCompileDirectory_;
    private File libCompileModulesDirectory_;
    private File libProvidedDirectory_;
    private File libProvidedModulesDirectory_;
    private File libRuntimeDirectory_;
    private File libRuntimeModulesDirectory_;
    private File libStandaloneDirectory_;
    private File libStandaloneModulesDirectory_;
    private File libTestDirectory_;
    private File libTestModulesDirectory_;
    private boolean preserveSources_ = false;
    private boolean preserveJavadoc_ = false;

    /**
     * Performs the purge operation.
     *
     * @since 1.5
     */
    public void execute() {
        if (offline_) {
            System.out.println("Offline mode: purge is disabled");
            return;
        }

        executePurgeCompileDependencies();
        executePurgeProvidedDependencies();
        executePurgeRuntimeDependencies();
        executePurgeStandaloneDependencies();
        executePurgeTestDependencies();
        if (!silent()) {
            System.out.println("Purging finished successfully.");
        }
    }

    /**
     * Part of the {@link #execute} operation, purge the {@code compile} scope artifacts.
     *
     * @since 1.5
     */
    protected void executePurgeCompileDependencies() {
        executePurgeDependencies(libCompileDirectory(), libCompileModulesDirectory(), dependencies().resolveCompileDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, purge the {@code provided} scope artifacts.
     *
     * @since 1.8
     */
    protected void executePurgeProvidedDependencies() {
        executePurgeDependencies(libProvidedDirectory(), libProvidedModulesDirectory(), dependencies().resolveProvidedDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, purge the {@code runtime} scope artifacts.
     *
     * @since 1.5
     */
    protected void executePurgeRuntimeDependencies() {
        executePurgeDependencies(libRuntimeDirectory(), libRuntimeModulesDirectory(), dependencies().resolveRuntimeDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, purge the {@code standalone} scope artifacts.
     *
     * @since 1.5
     */
    protected void executePurgeStandaloneDependencies() {
        executePurgeDependencies(libStandaloneDirectory(), libStandaloneModulesDirectory(), dependencies().resolveStandaloneDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, purge the {@code test} scope artifacts.
     *
     * @since 1.5
     */
    protected void executePurgeTestDependencies() {
        executePurgeDependencies(libTestDirectory(), libTestModulesDirectory(), dependencies().resolveTestDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, purge the artifacts for a particular dependency scope.
     *
     * @param classpathDirectory the directory from which the artifacts should be purged
     * @param modulesDirectory   the directory from which the modules should be purged
     * @param dependencies       the dependencies to purge
     * @since 2.1
     */
    protected void executePurgeDependencies(File classpathDirectory, File modulesDirectory, DependencySet dependencies) {
        if (classpathDirectory == null && modulesDirectory == null) {
            return;
        }

        var classpath_names = new HashSet<String>();
        var modules_names = new HashSet<String>();
        for (var dependency : dependencies) {
            var filenames = classpath_names;
            if (dependency.isModularJar()) {
                filenames = modules_names;
            }
            addTransferLocations(filenames, dependency);
            if (preserveSources_ && !dependency.excludedClassifiers().contains(CLASSIFIER_SOURCES)) {
                addTransferLocations(filenames, dependency.withClassifier(CLASSIFIER_SOURCES));
            }
            if (preserveJavadoc_ && !dependency.excludedClassifiers().contains(CLASSIFIER_JAVADOC)) {
                addTransferLocations(filenames, dependency.withClassifier(CLASSIFIER_JAVADOC));
            }
        }

        purgeFromDirectory(classpathDirectory, modulesDirectory, classpath_names);
        purgeFromDirectory(modulesDirectory, classpathDirectory, modules_names);
    }

    private static void purgeFromDirectory(File directory, File preserveDirectory, HashSet<String> preservedFileNames) {
        if (directory == null) {
            return;
        }

        boolean printed_header = false;
        var classpath_files = directory.listFiles();
        if (classpath_files != null) {
            for (var file : classpath_files) {
                if (!preservedFileNames.contains(file.getName()) && !file.equals(preserveDirectory)) {
                    if (!printed_header) {
                        printed_header = true;
                        System.out.println("Deleting from " + directory.getName() + ":");
                    }
                    System.out.println("    " + file.getName());
                    file.delete();
                }
            }
        }
    }

    private void addTransferLocations(HashSet<String> filenames, Dependency dependency) {
        var resolution = new VersionResolution(properties());
        for (var location : new DependencyResolver(resolution, artifactRetriever(), repositories(), dependency).getTransferLocations()) {
            filenames.add(location.substring(location.lastIndexOf("/") + 1));
        }
    }

    /**
     * Configures a compile operation from a {@link BaseProject}.
     *
     * @param project the project to configure the compile operation from
     * @since 1.5
     */
    public PurgeOperation fromProject(BaseProject project) {
        return offline(project.offline())
            .properties(project.properties())
            .artifactRetriever(project.artifactRetriever())
            .repositories(project.repositories())
            .dependencies(project.dependencies())
            .libCompileDirectory(project.libCompileDirectory())
            .libCompileModulesDirectory(project.libCompileModulesDirectory())
            .libProvidedDirectory(project.libProvidedDirectory())
            .libProvidedModulesDirectory(project.libProvidedModulesDirectory())
            .libRuntimeDirectory(project.libRuntimeDirectory())
            .libRuntimeModulesDirectory(project.libRuntimeModulesDirectory())
            .libStandaloneDirectory(project.libStandaloneDirectory())
            .libStandaloneModulesDirectory(project.libStandaloneModulesDirectory())
            .libTestDirectory(project.libTestDirectory())
            .libTestModulesDirectory(project.libTestModulesDirectory())
            .preserveSources(project.downloadSources())
            .preserveJavadoc(project.downloadJavadoc());
    }

    /**
     * Indicates whether the operation has to run offline.
     *
     * @param flag {@code true} if the operation runs offline; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 2.0
     */
    public PurgeOperation offline(boolean flag) {
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
     * Indicates whether the sources classifier files should be preserved.
     *
     * @param flag {@code true} if the sources classifier files should be preserved; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 1.5.6
     */
    public PurgeOperation preserveSources(boolean flag) {
        preserveSources_ = flag;
        return this;
    }

    /**
     * Indicates whether the javadoc classifier files should be preserved.
     *
     * @param flag {@code true} if the javadoc classifier files should be preserved; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 1.5.6
     */
    public PurgeOperation preserveJavadoc(boolean flag) {
        preserveJavadoc_ = flag;
        return this;
    }

    /**
     * Provides repositories to resolve the dependencies against.
     *
     * @param repositories repositories against which dependencies will be resolved
     * @return this operation instance
     * @since 1.5.18
     */
    public PurgeOperation repositories(Repository... repositories) {
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
     * @since 1.5
     */
    public PurgeOperation repositories(List<Repository> repositories) {
        repositories_.addAll(repositories);
        return this;
    }

    /**
     * Provides scoped dependencies for artifact purge.
     *
     * @param dependencies the dependencies that will be resolved for artifact purge
     * @return this operation instance
     * @since 1.5
     */
    public PurgeOperation dependencies(DependencyScopes dependencies) {
        dependencies_.include(dependencies);
        return this;
    }

    /**
     * Provides the {@code compile} scope purge directory.
     *
     * @param directory the directory to purge the {@code compile} scope artifacts from
     * @return this operation instance
     * @since 1.5
     */
    public PurgeOperation libCompileDirectory(File directory) {
        libCompileDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code compile} scope modules purge directory.
     *
     * @param directory the directory to purge the {@code compile} scope modules from
     * @return this operation instance
     * @since 2.1
     */
    public PurgeOperation libCompileModulesDirectory(File directory) {
        libCompileModulesDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code provided} scope purge directory.
     *
     * @param directory the directory to purge the {@code provided} scope artifacts from
     * @return this operation instance
     * @since 1.8
     */
    public PurgeOperation libProvidedDirectory(File directory) {
        libProvidedDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code provided} scope modules purge directory.
     *
     * @param directory the directory to purge the {@code provided} scope modules from
     * @return this operation instance
     * @since 2.1
     */
    public PurgeOperation libProvidedModulesDirectory(File directory) {
        libProvidedModulesDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code runtime} scope purge directory.
     *
     * @param directory the directory to purge the {@code runtime} scope artifacts from
     * @return this operation instance
     * @since 1.5
     */
    public PurgeOperation libRuntimeDirectory(File directory) {
        libRuntimeDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code runtime} scope modules purge directory.
     *
     * @param directory the directory to purge the {@code runtime} scope modules from
     * @return this operation instance
     * @since 2.1
     */
    public PurgeOperation libRuntimeModulesDirectory(File directory) {
        libRuntimeModulesDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code standalone} scope purge directory.
     *
     * @param directory the directory to purge the {@code standalone} scope artifacts from
     * @return this operation instance
     * @since 1.5
     */
    public PurgeOperation libStandaloneDirectory(File directory) {
        libStandaloneDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code standalone} scope modules purge directory.
     *
     * @param directory the directory to purge the {@code standalone} scope modules from
     * @return this operation instance
     * @since 2.1
     */
    public PurgeOperation libStandaloneModulesDirectory(File directory) {
        libStandaloneModulesDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code test} scope purge directory.
     *
     * @param directory the directory to purge the {@code test} scope artifacts from
     * @return this operation instance
     * @since 1.5
     */
    public PurgeOperation libTestDirectory(File directory) {
        libTestDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code test} scope modules purge directory.
     *
     * @param directory the directory to purge the {@code test} scope modules from
     * @return this operation instance
     * @since 2.1
     */
    public PurgeOperation libTestModulesDirectory(File directory) {
        libTestModulesDirectory_ = directory;
        return this;
    }

    /**
     * Provides the artifact retriever to use.
     *
     * @param retriever the artifact retriever
     * @return this operation instance
     * @since 1.5.18
     */
    public PurgeOperation artifactRetriever(ArtifactRetriever retriever) {
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
    public PurgeOperation properties(HierarchicalProperties properties) {
        properties_ = properties;
        return this;
    }

    /**
     * Retrieves the repositories in which the dependencies will be resolved.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the repositories used for dependency resolution
     * @since 1.5
     */
    public List<Repository> repositories() {
        return repositories_;
    }

    /**
     * Retrieves the scoped dependencies that will be used for artifact purge.
     * <p>
     * This is a modifiable structure that can be retrieved and changed.
     *
     * @return the scoped dependencies
     * @since 1.5
     */
    public DependencyScopes dependencies() {
        return dependencies_;
    }

    /**
     * Retrieves the {@code compile} scope purge directory.
     *
     * @return the {@code compile} scope purge directory
     * @since 1.5
     */
    public File libCompileDirectory() {
        return libCompileDirectory_;
    }

    /**
     * Retrieves the {@code compile} scope modules purge directory.
     *
     * @return the {@code compile} scope modules purge directory
     * @since 2.1
     */
    public File libCompileModulesDirectory() {
        return libCompileModulesDirectory_;
    }

    /**
     * Retrieves the {@code provided} scope purge directory.
     *
     * @return the {@code provided} scope purge directory
     * @since 1.8
     */
    public File libProvidedDirectory() {
        return libProvidedDirectory_;
    }

    /**
     * Retrieves the {@code provided} scope modules purge directory.
     *
     * @return the {@code provided} scope modules purge directory
     * @since 2.1
     */
    public File libProvidedModulesDirectory() {
        return libProvidedModulesDirectory_;
    }

    /**
     * Retrieves the {@code runtime} scope purge directory.
     *
     * @return the {@code runtime} scope purge directory
     * @since 1.5
     */
    public File libRuntimeDirectory() {
        return libRuntimeDirectory_;
    }

    /**
     * Retrieves the {@code runtime} scope modules purge directory.
     *
     * @return the {@code runtime} scope modules purge directory
     * @since 2.1
     */
    public File libRuntimeModulesDirectory() {
        return libRuntimeModulesDirectory_;
    }

    /**
     * Retrieves the {@code standalone} scope purge directory.
     *
     * @return the {@code standalone} scope purge directory
     * @since 1.5
     */
    public File libStandaloneDirectory() {
        return libStandaloneDirectory_;
    }

    /**
     * Retrieves the {@code standalone} scope modules purge directory.
     *
     * @return the {@code standalone} scope modules purge directory
     * @since 2.1
     */
    public File libStandaloneModulesDirectory() {
        return libStandaloneModulesDirectory_;
    }

    /**
     * Retrieves the {@code test} scope purge directory.
     *
     * @return the {@code test} scope purge directory
     * @since 1.5
     */
    public File libTestDirectory() {
        return libTestDirectory_;
    }

    /**
     * Retrieves the {@code test} scope modules purge directory.
     *
     * @return the {@code test} scope modules purge directory
     * @since 2.1
     */
    public File libTestModulesDirectory() {
        return libTestModulesDirectory_;
    }

    /**
     * Retrieves whether the sources classifier files should be preserved.
     *
     * @return {@code true} if the sources classifier should be preserved; or
     * {@code false} otherwise
     * @since 1.5.6
     */
    public boolean preserveSources() {
        return preserveSources_;
    }

    /**
     * Retrieves whether the javadoc classifier files should be preserved.
     *
     * @return {@code true} if the javadoc classifier should be preserved; or
     * {@code false} otherwise
     * @since 1.5.6
     */
    public boolean preserveJavadoc() {
        return preserveJavadoc_;
    }

    /**
     * Returns the artifact retriever that is used.
     *
     * @return the artifact retriever
     * @since 1.5.18
     */
    public ArtifactRetriever artifactRetriever() {
        if (retriever_ == null) {
            return ArtifactRetriever.instance();
        }
        return retriever_;
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
}
