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
 * Transitively downloads all the artifacts for dependencies into
 * directories that are separated out by scope.
 * <p>
 * If a directory is not provided, no download will occur for that
 * dependency scope.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class DownloadOperation extends AbstractOperation<DownloadOperation> {
    private boolean offline_ = false;
    private HierarchicalProperties properties_ = null;
    private ArtifactRetriever retriever_ = null;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencyScopes dependencies_ = new DependencyScopes();
    private File libCompileDirectory_;
    private File libProvidedDirectory_;
    private File libRuntimeDirectory_;
    private File libStandaloneDirectory_;
    private File libTestDirectory_;
    private boolean downloadSources_ = false;
    private boolean downloadJavadoc_ = false;

    /**
     * Performs the download operation.
     *
     * @since 1.5
     */
    public void execute() {
        if (offline_) {
            System.out.println("Offline mode: download is disabled");
            return;
        }

        executeDownloadCompileDependencies();
        executeDownloadProvidedDependencies();
        executeDownloadRuntimeDependencies();
        executeDownloadStandaloneDependencies();
        executeDownloadTestDependencies();
        if (!silent()) {
            System.out.println("Downloading finished successfully.");
        }
    }

    /**
     * Part of the {@link #execute} operation, download the {@code compile} scope artifacts.
     *
     * @since 1.5
     */
    protected void executeDownloadCompileDependencies() {
        executeDownloadDependencies(libCompileDirectory(), dependencies().resolveCompileDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, download the {@code provided} scope artifacts.
     *
     * @since 1.8
     */
    protected void executeDownloadProvidedDependencies() {
        executeDownloadDependencies(libProvidedDirectory(), dependencies().resolveProvidedDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, download the {@code runtime} scope artifacts.
     *
     * @since 1.5
     */
    protected void executeDownloadRuntimeDependencies() {
        executeDownloadDependencies(libRuntimeDirectory(), dependencies().resolveRuntimeDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, download the {@code standalone} scope artifacts.
     *
     * @since 1.5
     */
    protected void executeDownloadStandaloneDependencies() {
        executeDownloadDependencies(libStandaloneDirectory(), dependencies().resolveStandaloneDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, download the {@code test} scope artifacts.
     *
     * @since 1.5
     */
    protected void executeDownloadTestDependencies() {
        executeDownloadDependencies(libTestDirectory(), dependencies().resolveTestDependencies(properties(), artifactRetriever(), repositories()));
    }

    /**
     * Part of the {@link #execute} operation, download the artifacts for a particular dependency scope.
     *
     * @param destinationDirectory the directory in which the artifacts should be downloaded
     * @param dependencies         the dependencies to download
     * @since 1.6
     */
    protected void executeDownloadDependencies(File destinationDirectory, DependencySet dependencies) {
        if (destinationDirectory == null) {
            return;
        }

        destinationDirectory.mkdirs();

        var additional_classifiers = new String[0];

        if (downloadSources_ || downloadJavadoc_) {
            var classifiers = new ArrayList<String>();
            if (downloadSources_) classifiers.add(CLASSIFIER_SOURCES);
            if (downloadJavadoc_) classifiers.add(CLASSIFIER_JAVADOC);

            additional_classifiers = classifiers.toArray(new String[0]);
        }

        dependencies.transferIntoDirectory(new VersionResolution(properties()), artifactRetriever(), repositories(), destinationDirectory, additional_classifiers);
    }

    /**
     * Configures a compile operation from a {@link BaseProject}.
     *
     * @param project the project to configure the compile operation from
     * @return this operation instance
     * @since 1.5
     */
    public DownloadOperation fromProject(BaseProject project) {
        return offline(project.offline())
            .properties(project.properties())
            .artifactRetriever(project.artifactRetriever())
            .repositories(project.repositories())
            .dependencies(project.dependencies())
            .libCompileDirectory(project.libCompileDirectory())
            .libProvidedDirectory(project.libProvidedDirectory())
            .libRuntimeDirectory(project.libRuntimeDirectory())
            .libStandaloneDirectory(project.libStandaloneDirectory())
            .libTestDirectory(project.libTestDirectory())
            .downloadSources(project.downloadSources())
            .downloadJavadoc(project.downloadJavadoc());
    }

    /**
     * Indicates whether the operation has to run offline.
     *
     * @param flag {@code true} if the operation runs offline; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 2.0
     */
    public DownloadOperation offline(boolean flag) {
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
     * @since 1.5.18
     */
    public DownloadOperation repositories(Repository... repositories) {
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
    public DownloadOperation repositories(List<Repository> repositories) {
        repositories_.addAll(repositories);
        return this;
    }

    /**
     * Provides scoped dependencies for artifact download.
     *
     * @param dependencies the dependencies that will be resolved for artifact download
     * @return this operation instance
     * @since 1.5
     */
    public DownloadOperation dependencies(DependencyScopes dependencies) {
        dependencies_.include(dependencies);
        return this;
    }

    /**
     * Provides the {@code compile} scope download directory.
     *
     * @param directory the directory to download the {@code compile} scope artifacts into
     * @return this operation instance
     * @since 1.5
     */
    public DownloadOperation libCompileDirectory(File directory) {
        libCompileDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code provided} scope download directory.
     *
     * @param directory the directory to download the {@code provided} scope artifacts into
     * @return this operation instance
     * @since 1.8
     */
    public DownloadOperation libProvidedDirectory(File directory) {
        libProvidedDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code runtime} scope download directory.
     *
     * @param directory the directory to download the {@code runtime} scope artifacts into
     * @return this operation instance
     * @since 1.5
     */
    public DownloadOperation libRuntimeDirectory(File directory) {
        libRuntimeDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code standalone} scope download directory.
     *
     * @param directory the directory to download the {@code standalone} scope artifacts into
     * @return this operation instance
     * @since 1.5
     */
    public DownloadOperation libStandaloneDirectory(File directory) {
        libStandaloneDirectory_ = directory;
        return this;
    }

    /**
     * Provides the {@code test} scope download directory.
     *
     * @param directory the directory to download the {@code test} scope artifacts into
     * @return this operation instance
     * @since 1.5
     */
    public DownloadOperation libTestDirectory(File directory) {
        libTestDirectory_ = directory;
        return this;
    }

    /**
     * Indicates whether the sources classifier should also be downloaded.
     *
     * @param flag {@code true} if the sources classifier should be downloaded; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 1.5.6
     */
    public DownloadOperation downloadSources(boolean flag) {
        downloadSources_ = flag;
        return this;
    }

    /**
     * Indicates whether the javadoc classifier should also be downloaded.
     *
     * @param flag {@code true} if the javadoc classifier should be downloaded; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 1.5.6
     */
    public DownloadOperation downloadJavadoc(boolean flag) {
        downloadJavadoc_ = flag;
        return this;
    }

    /**
     * Provides the artifact retriever to use.
     *
     * @param retriever the artifact retriever
     * @return this operation instance
     * @since 1.5.18
     */
    public DownloadOperation artifactRetriever(ArtifactRetriever retriever) {
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
    public DownloadOperation properties(HierarchicalProperties properties) {
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
     * Retrieves the scoped dependencies that will be used for artifact download.
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
     * Retrieves the {@code compile} scope download directory.
     *
     * @return the {@code compile} scope download directory
     * @since 1.5
     */
    public File libCompileDirectory() {
        return libCompileDirectory_;
    }

    /**
     * Retrieves the {@code provided} scope download directory.
     *
     * @return the {@code provided} scope download directory
     * @since 1.8
     */
    public File libProvidedDirectory() {
        return libProvidedDirectory_;
    }

    /**
     * Retrieves the {@code runtime} scope download directory.
     *
     * @return the {@code runtime} scope download directory
     * @since 1.5
     */
    public File libRuntimeDirectory() {
        return libRuntimeDirectory_;
    }

    /**
     * Retrieves the {@code standalone} scope download directory.
     *
     * @return the {@code standalone} scope download directory
     * @since 1.5
     */
    public File libStandaloneDirectory() {
        return libStandaloneDirectory_;
    }

    /**
     * Retrieves the {@code test} scope download directory.
     *
     * @return the {@code test} scope download directory
     * @since 1.5
     */
    public File libTestDirectory() {
        return libTestDirectory_;
    }

    /**
     * Retrieves whether the sources classifier should also be downloaded.
     *
     * @return {@code true} if the sources classifier should be downloaded; or
     * {@code false} otherwise
     * @since 1.5.6
     */
    public boolean downloadSources() {
        return downloadSources_;
    }

    /**
     * Retrieves whether the javadoc classifier should also be downloaded.
     *
     * @return {@code true} if the sources classifier should be downloaded; or
     * {@code false} otherwise
     * @since 1.5.6
     */
    public boolean downloadJavadoc() {
        return downloadJavadoc_;
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
