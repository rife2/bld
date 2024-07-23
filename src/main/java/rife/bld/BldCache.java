/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.bld.dependencies.DependencyScopes;
import rife.bld.dependencies.Repository;
import rife.bld.dependencies.VersionResolution;
import rife.bld.wrapper.Wrapper;
import rife.tools.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Provides functionalities related to dependency hashing and caching.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.0
 */
public class BldCache {
    /**
     * Represents the name of a cache file used by bld.
     * @since 2.0
     */
    public static final String BLD_CACHE = "bld.cache";

    private static final String PROPERTY_SUFFIX_HASH = ".hash";
    private static final String PROPERTY_SUFFIX_LOCAL = ".local";
    private static final String PROPERTY_SUFFIX_DOWNLOAD_SOURCES = ".download.sources";
    private static final String PROPERTY_SUFFIX_DOWNLOAD_JAVADOC = ".download.javadoc";
    private static final String PROPERTY_SUFFIX_DEPENDENCY_TREE = ".dependency.tree";

    private static final String WRAPPER_PROPERTIES_HASH = Wrapper.WRAPPER_PROPERTIES + PROPERTY_SUFFIX_HASH;
    private static final String BLD_BUILD_HASH = "bld-build" + PROPERTY_SUFFIX_HASH;

    private static final String PROPERTY_EXTENSIONS_PREFIX = "bld.extensions";
    private static final String PROPERTY_EXTENSIONS_HASH = PROPERTY_EXTENSIONS_PREFIX + PROPERTY_SUFFIX_HASH;
    private static final String PROPERTY_EXTENSIONS_LOCAL = PROPERTY_EXTENSIONS_PREFIX + PROPERTY_SUFFIX_LOCAL;
    private static final String PROPERTY_EXTENSIONS_DOWNLOAD_SOURCES = PROPERTY_EXTENSIONS_PREFIX + PROPERTY_SUFFIX_DOWNLOAD_SOURCES;
    private static final String PROPERTY_EXTENSIONS_DOWNLOAD_JAVADOC = PROPERTY_EXTENSIONS_PREFIX + PROPERTY_SUFFIX_DOWNLOAD_JAVADOC;
    private static final String PROPERTY_EXTENSIONS_DEPENDENCY_TREE = PROPERTY_EXTENSIONS_PREFIX + PROPERTY_SUFFIX_DEPENDENCY_TREE;

    private static final String PROPERTY_DEPENDENCIES_PREFIX = "bld.dependencies";
    private static final String PROPERTY_DEPENDENCIES_HASH = PROPERTY_DEPENDENCIES_PREFIX + PROPERTY_SUFFIX_HASH;
    private static final String PROPERTY_DEPENDENCIES_DOWNLOAD_SOURCES = PROPERTY_DEPENDENCIES_PREFIX + PROPERTY_SUFFIX_DOWNLOAD_SOURCES;
    private static final String PROPERTY_DEPENDENCIES_DOWNLOAD_JAVADOC = PROPERTY_DEPENDENCIES_PREFIX + PROPERTY_SUFFIX_DOWNLOAD_JAVADOC;
    private static final String PROPERTY_DEPENDENCIES_COMPILE_DEPENDENCY_TREE = PROPERTY_DEPENDENCIES_PREFIX + ".compile" + PROPERTY_SUFFIX_DEPENDENCY_TREE;
    private static final String PROPERTY_DEPENDENCIES_PROVIDED_DEPENDENCY_TREE = PROPERTY_DEPENDENCIES_PREFIX + ".provided" + PROPERTY_SUFFIX_DEPENDENCY_TREE;
    private static final String PROPERTY_DEPENDENCIES_RUNTIME_DEPENDENCY_TREE = PROPERTY_DEPENDENCIES_PREFIX + ".runtime" + PROPERTY_SUFFIX_DEPENDENCY_TREE;
    private static final String PROPERTY_DEPENDENCIES_TEST_DEPENDENCY_TREE = PROPERTY_DEPENDENCIES_PREFIX + ".test" + PROPERTY_SUFFIX_DEPENDENCY_TREE;

    private final File cacheDir_;
    private final VersionResolution resolution_;
    private String extensionsHash_;
    private Boolean extensionsDownloadSources_;
    private Boolean extensionsDownloadJavadocs_;
    private List<File> extensionsLocalArtifacts_;
    private String extensionsDependencyTree_;
    private String dependenciesHash_;
    private Boolean dependenciesDownloadSources_;
    private Boolean dependenciesDownloadJavadocs_;
    private String dependenciesCompileDependencyTree_;
    private String dependenciesProvidedDependencyTree_;
    private String dependenciesRuntimeDependencyTree_;
    private String dependenciesTestDependencyTree_;

    /**
     * Creates a new {@code BldCache} instance.
     *
     * @param cacheDir the directory where the bld cache file is stored
     * @param resolution the version resolution that should be used when needed during the cache operations
     * @since 2.0
     */
    public BldCache(File cacheDir, VersionResolution resolution) {
        cacheDir_ = cacheDir;
        resolution_ = resolution;

        new File(cacheDir, WRAPPER_PROPERTIES_HASH).delete();
        new File(cacheDir, BLD_BUILD_HASH).delete();
    }

    /**
     * Calculates the hash that corresponds to the provided repositories and extensions.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param repositories the repositories to include into the hash
     * @param extensions the extensions to include into the hash
     * @since 2.0
     * @see #isExtensionsHashValid()
     * @see #isExtensionsCacheValid()
     * @see #writeCache()
     */
    public void cacheExtensionsHash(Collection<String> repositories, Collection<String> extensions) {
        try {
            var overrides_fp = String.join("\n", resolution_.versionOverrides().entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).toList());
            var repositories_fp = String.join("\n", repositories);
            var extensions_fp = String.join("\n", extensions);
            var fingerprint = overrides_fp + "\n" + repositories_fp + "\n" + extensions_fp + "\n";
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(fingerprint.getBytes(StandardCharsets.UTF_8));

            extensionsHash_ = StringUtils.encodeHexLower(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Determined whether the extensions hash stored in this {@code BldCache} instance is the same as the one stored
     * in the cache on disk.
     *
     * @return {@code true} is the extensions hash is the same; or {@code false} otherwise
     * @since 2.0
     * @see #cacheExtensionsHash
     * @see #isExtensionsCacheValid
     */
    public boolean isExtensionsHashValid() {
        return validateExtensionsHash(extensionsHash_);
    }

    /**
     * Sets which other artifacts should be downloaded besides main jars for the extensions.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param downloadSources whether the extensions sources should be downloaded or not
     * @param downloadJavadoc whether the extensions javadocs should be downloaded or not
     * @since 2.0
     * @see #isExtensionsCacheValid()
     * @see #writeCache()
     */
    public void cacheExtensionsDownloads(boolean downloadSources, boolean downloadJavadoc) {
        extensionsDownloadSources_ = downloadSources;
        extensionsDownloadJavadocs_ = downloadJavadoc;
    }

    /**
     * Sets which local artifacts were used by the transfer of the extensions into the project.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param extensionsLocalArtifacts the list of extension files that were pulled from local storage
     * @since 2.0
     * @see #isExtensionsCacheValid()
     * @see #writeCache()
     */
    public void cacheExtensionsLocalArtifacts(List<File> extensionsLocalArtifacts) {
        extensionsLocalArtifacts_ = extensionsLocalArtifacts;
    }

    /**
     * Sets the textual presentation of the extensions dependency tree.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param dependencyTree the textual presentation of the extensions dependency tree
     * @since 2.0
     * @see #getCachedExtensionsDependencyTree
     * @see #writeCache()
     */
    public void cacheExtensionsDependencyTree(String dependencyTree) {
        extensionsDependencyTree_ = dependencyTree;
    }

    /**
     * Retrieves the textual presentation of the extensions dependency tree from the cache state on disk.
     *
     * @return the cached textual presentation of the extensions dependency tree; or {@code null} of this dependency
     * tree wasn't found in the stored cache
     * @since 2.0
     * @see #cacheExtensionsDependencyTree
     */
    public String getCachedExtensionsDependencyTree() {
        return hashProperties().getProperty(PROPERTY_EXTENSIONS_DEPENDENCY_TREE);
    }

    /**
     * Determines whether the extensions state stored in this {@code BldCache} instance is the same as the state of the cache on disk.
     *
     * @return {@code true} if state is identical; or {@code false} otherwise
     * @since 2.0
     * @see #cacheExtensionsHash
     * @see #cacheExtensionsDownloads
     * @see #cacheExtensionsLocalArtifacts
     */
    public boolean isExtensionsCacheValid() {
        var properties = hashProperties();
        if (properties.isEmpty()) {
            return false;
        }

        if (extensionsDownloadSources_ != Boolean.parseBoolean(properties.getProperty(PROPERTY_EXTENSIONS_DOWNLOAD_SOURCES))) {
            return false;
        }

        if (extensionsDownloadJavadocs_ != Boolean.parseBoolean(properties.getProperty(PROPERTY_EXTENSIONS_DOWNLOAD_JAVADOC))) {
            return false;
        }

        return validateExtensionsHash(extensionsHash_);
    }

    private boolean validateExtensionsHash(String hash) {
        var properties = hashProperties();
        if (properties.isEmpty()) {
            return false;
        }

        if (!hash.equals(properties.getProperty(PROPERTY_EXTENSIONS_HASH))) {
            return false;
        }

        var local_files = properties.getProperty(PROPERTY_EXTENSIONS_LOCAL);
        if (local_files != null && !local_files.isEmpty()) {
            var lines = StringUtils.split(local_files, "\n");
            if (!lines.isEmpty()) {
                // other lines are last modified timestamps of local files
                // that were dependency artifacts
                while (!lines.isEmpty()) {
                    var line = lines.get(0);
                    var parts = line.split(":", 2);
                    // verify that the local file has the same modified timestamp still
                    if (parts.length == 2) {
                        var file = new File(parts[1]);
                        if (!file.exists() || !file.canRead() || file.lastModified() != Long.parseLong(parts[0])) {
                            break;
                        }
                    } else {
                        break;
                    }
                    lines.remove(0);
                }

                // there were no invalid lines, so the hash file contents are valid
                return lines.isEmpty();
            }
        }

        return true;
    }

    /**
     * Calculates the hash that corresponds to the provided repositories and dependencies.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param repositories the repositories to include into the hash
     * @param dependencies the dependencies to include into the hash
     * @since 2.0
     * @see #isDependenciesHashValid()
     * @see #isDependenciesCacheValid()
     * @see #writeCache()
     */
    public void cacheDependenciesHash(List<Repository> repositories, DependencyScopes dependencies) {
        var finger_print = new StringBuilder();
        finger_print.append(String.join("\n", resolution_.versionOverrides().entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).toList()));
        for (var repository : repositories) {
            finger_print.append(repository.toString());
            finger_print.append('\n');
        }
        for (var entry : dependencies.entrySet()) {
            finger_print.append(entry.getKey());
            finger_print.append('\n');
            if (entry.getValue() != null) {
                for (var dependency : entry.getValue()) {
                    finger_print.append(dependency.toString());
                    finger_print.append('\n');
                }
            }
        }

        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(finger_print.toString().getBytes(StandardCharsets.UTF_8));
            dependenciesHash_ = StringUtils.encodeHexLower(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Determined whether the dependencies hash stored in this {@code BldCache} instance is the same as the one stored
     * in the cache on disk.
     *
     * @return {@code true} is the dependencies hash is the same; or {@code false} otherwise
     * @since 2.0
     * @see #cacheDependenciesHash
     * @see #isDependenciesCacheValid
     */
    public boolean isDependenciesHashValid() {
        return validateDependenciesHash(dependenciesHash_);
    }

    /**
     * Sets which other artifacts should be downloaded besides main jars for the dependencies.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param downloadSources whether the dependencies sources should be downloaded or not
     * @param downloadJavadoc whether the dependencies javadocs should be downloaded or not
     * @since 2.0
     * @see #isDependenciesCacheValid()
     * @see #writeCache()
     */
    public void cacheDependenciesDownloads(boolean downloadSources, boolean downloadJavadoc) {
        dependenciesDownloadSources_ = downloadSources;
        dependenciesDownloadJavadocs_ = downloadJavadoc;
    }

    /**
     * Sets the textual presentation of the compile scope dependency tree.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param compileTree the textual presentation of the compile scope dependency tree
     * @since 2.0
     * @see #getCachedDependenciesCompileDependencyTree
     * @see #writeCache()
     */
    public void cacheDependenciesCompileDependencyTree(String compileTree) {
        dependenciesCompileDependencyTree_ = compileTree;
    }

    /**
     * Retrieves the textual presentation of the compile scope dependency tree from the cache state on disk.
     *
     * @return the cached textual presentation of the compile scope dependency tree; or {@code null} of this dependency
     * tree wasn't found in the stored cache
     * @since 2.0
     * @see #cacheDependenciesCompileDependencyTree
     */
    public String getCachedDependenciesCompileDependencyTree() {
        return hashProperties().getProperty(PROPERTY_DEPENDENCIES_COMPILE_DEPENDENCY_TREE);
    }

    /**
     * Sets the textual presentation of the provided scope dependency tree.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param providedTree the textual presentation of the provided scope dependency tree
     * @since 2.0
     * @see #getCachedDependenciesProvidedDependencyTree
     * @see #writeCache()
     */
    public void cacheDependenciesProvidedDependencyTree(String providedTree) {
        dependenciesProvidedDependencyTree_ = providedTree;
    }

    /**
     * Retrieves the textual presentation of the provided scope dependency tree from the cache state on disk.
     *
     * @return the cached textual presentation of the provided scope dependency tree; or {@code null} of this dependency
     * tree wasn't found in the stored cache
     * @since 2.0
     * @see #cacheDependenciesProvidedDependencyTree
     */
    public String getCachedDependenciesProvidedDependencyTree() {
        return hashProperties().getProperty(PROPERTY_DEPENDENCIES_PROVIDED_DEPENDENCY_TREE);
    }

    /**
     * Sets the textual presentation of the runtime scope dependency tree.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param runtimeTree the textual presentation of the runtime scope dependency tree
     * @since 2.0
     * @see #getCachedDependenciesRuntimeDependencyTree
     * @see #writeCache()
     */
    public void cacheDependenciesRuntimeDependencyTree(String runtimeTree) {
        dependenciesRuntimeDependencyTree_ = runtimeTree;
    }

    /**
     * Retrieves the textual presentation of the runtime scope dependency tree from the cache state on disk.
     *
     * @return the cached textual presentation of the runtime scope dependency tree; or {@code null} of this dependency
     * tree wasn't found in the stored cache
     * @since 2.0
     * @see #cacheDependenciesRuntimeDependencyTree
     */
    public String getCachedDependenciesRuntimeDependencyTree() {
        return hashProperties().getProperty(PROPERTY_DEPENDENCIES_RUNTIME_DEPENDENCY_TREE);
    }

    /**
     * Sets the textual presentation of the test scope dependency tree.
     * <p>
     * This will be stored with this instance of {@code BldCache}, using {@link #writeCache()} is required to store it to disk.
     *
     * @param testTree the textual presentation of the test scope dependency tree
     * @since 2.0
     * @see #getCachedDependenciesTestDependencyTree
     * @see #writeCache()
     */
    public void cacheDependenciesTestDependencyTree(String testTree) {
        dependenciesTestDependencyTree_ = testTree;
    }

    /**
     * Retrieves the textual presentation of the test scope dependency tree from the cache state on disk.
     *
     * @return the cached textual presentation of the test scope dependency tree; or {@code null} of this dependency
     * tree wasn't found in the stored cache
     * @since 2.0
     * @see #cacheDependenciesTestDependencyTree
     */
    public String getCachedDependenciesTestDependencyTree() {
        return hashProperties().getProperty(PROPERTY_DEPENDENCIES_TEST_DEPENDENCY_TREE);
    }

    /**
     * Determines whether the dependencies state stored in this {@code BldCache} instance is the same as the state of the cache on disk.
     *
     * @return {@code true} if state is identical; or {@code false} otherwise
     * @since 2.0
     * @see #cacheDependenciesHash
     * @see #cacheDependenciesDownloads
     */
    public boolean isDependenciesCacheValid() {
        var properties = hashProperties();
        if (properties.isEmpty()) {
            return false;
        }

        if (dependenciesDownloadSources_ != Boolean.parseBoolean(properties.getProperty(PROPERTY_DEPENDENCIES_DOWNLOAD_SOURCES))) {
            return false;
        }

        if (dependenciesDownloadJavadocs_ != Boolean.parseBoolean(properties.getProperty(PROPERTY_DEPENDENCIES_DOWNLOAD_JAVADOC))) {
            return false;
        }

        return validateDependenciesHash(dependenciesHash_);
    }

    private boolean validateDependenciesHash(String hash) {
        var properties = hashProperties();
        if (properties.isEmpty()) {
            return false;
        }

        return hash.equals(properties.getProperty(PROPERTY_DEPENDENCIES_HASH));
    }

    private File getCacheFile() {
        return new File(cacheDir_, BLD_CACHE);
    }

    private Properties hashProperties() {
        var properties = new Properties();
        if (getCacheFile().exists()) {
            try {
                try (var reader = new BufferedReader(new FileReader(getCacheFile()))) {
                    properties.load(reader);
                }
            } catch (IOException e) {
                // no-op, we'll store a new properties file when we're writing the cache
            }
        }
        return properties;
    }

    /**
     * Writes the state of this {@code BldCache} instance to disk.
     *
     * @since 2.0
     */
    public void writeCache() {
        var properties = hashProperties();

        try {
            if (extensionsHash_ != null) {
                if (!extensionsHash_.equals(properties.get(PROPERTY_EXTENSIONS_HASH))) {
                    properties.put(PROPERTY_EXTENSIONS_HASH, extensionsHash_);
                    properties.remove(PROPERTY_EXTENSIONS_DEPENDENCY_TREE);
                }

                if (extensionsDependencyTree_ != null) {
                    properties.put(PROPERTY_EXTENSIONS_DEPENDENCY_TREE, extensionsDependencyTree_);
                }
            }

            if (extensionsDownloadSources_ != null) {
                properties.put(PROPERTY_EXTENSIONS_DOWNLOAD_SOURCES, String.valueOf(extensionsDownloadSources_));
            }

            if (extensionsDownloadJavadocs_ != null) {
                properties.put(PROPERTY_EXTENSIONS_DOWNLOAD_JAVADOC, String.valueOf(extensionsDownloadJavadocs_));
            }

            if (extensionsLocalArtifacts_ != null) {
                var extensions_local = new StringBuilder();
                for (var file : extensionsLocalArtifacts_) {
                    if (file.exists() && file.canRead()) {
                        if (!extensions_local.isEmpty()) {
                            extensions_local.append("\n");
                        }
                        extensions_local.append(file.lastModified()).append(':').append(file.getAbsolutePath());
                    }
                }
                properties.put(PROPERTY_EXTENSIONS_LOCAL, extensions_local.toString());
            }

            if (dependenciesHash_ != null) {
                if (!dependenciesHash_.equals(properties.get(PROPERTY_DEPENDENCIES_HASH))) {
                    properties.put(PROPERTY_DEPENDENCIES_HASH, dependenciesHash_);
                    properties.remove(PROPERTY_DEPENDENCIES_COMPILE_DEPENDENCY_TREE);
                    properties.remove(PROPERTY_DEPENDENCIES_PROVIDED_DEPENDENCY_TREE);
                    properties.remove(PROPERTY_DEPENDENCIES_RUNTIME_DEPENDENCY_TREE);
                    properties.remove(PROPERTY_DEPENDENCIES_TEST_DEPENDENCY_TREE);
                }

                if (dependenciesCompileDependencyTree_ != null) {
                    properties.put(PROPERTY_DEPENDENCIES_COMPILE_DEPENDENCY_TREE, dependenciesCompileDependencyTree_);
                }

                if (dependenciesProvidedDependencyTree_ != null) {
                    properties.put(PROPERTY_DEPENDENCIES_PROVIDED_DEPENDENCY_TREE, dependenciesProvidedDependencyTree_);
                }

                if (dependenciesRuntimeDependencyTree_ != null) {
                    properties.put(PROPERTY_DEPENDENCIES_RUNTIME_DEPENDENCY_TREE, dependenciesRuntimeDependencyTree_);
                }

                if (dependenciesTestDependencyTree_ != null) {
                    properties.put(PROPERTY_DEPENDENCIES_TEST_DEPENDENCY_TREE, dependenciesTestDependencyTree_);
                }
            }

            if (dependenciesDownloadSources_ != null) {
                properties.put(PROPERTY_DEPENDENCIES_DOWNLOAD_SOURCES, String.valueOf(dependenciesDownloadSources_));
            }

            if (dependenciesDownloadJavadocs_ != null) {
                properties.put(PROPERTY_DEPENDENCIES_DOWNLOAD_JAVADOC, String.valueOf(dependenciesDownloadJavadocs_));
            }

            cacheDir_.mkdirs();

            try (var writer = new BufferedWriter(new FileWriter(getCacheFile()))) {
                properties.store(writer, null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

