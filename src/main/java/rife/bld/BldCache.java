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

    private static final String WRAPPER_PROPERTIES_HASH = Wrapper.WRAPPER_PROPERTIES + PROPERTY_SUFFIX_HASH;
    private static final String BLD_BUILD_HASH = "bld-build" + PROPERTY_SUFFIX_HASH;

    private static final String PROPERTY_EXTENSIONS_PREFIX = "bld.extensions";
    private static final String PROPERTY_EXTENSIONS_HASH = PROPERTY_EXTENSIONS_PREFIX + PROPERTY_SUFFIX_HASH;
    private static final String PROPERTY_EXTENSIONS_LOCAL = PROPERTY_EXTENSIONS_PREFIX + PROPERTY_SUFFIX_LOCAL;

    private static final String PROPERTY_DEPENDENCIES_PREFIX = "bld.dependencies";
    private static final String PROPERTY_DEPENDENCIES_HASH = PROPERTY_DEPENDENCIES_PREFIX + PROPERTY_SUFFIX_HASH;

    private final File bldLibDir_;
    private final VersionResolution resolution_;
    private String extensionsHash_;
    private String dependenciesHash_;

    public BldCache(File bldLibDir, VersionResolution resolution) {
        bldLibDir_ = bldLibDir;
        resolution_ = resolution;

        new File(bldLibDir, WRAPPER_PROPERTIES_HASH).delete();
        new File(bldLibDir, BLD_BUILD_HASH).delete();
    }

    public void fingerprintExtensions(Collection<String> repositories, Collection<String> extensions, boolean downloadSources, boolean downloadJavadoc) {
        try {
            var overrides_fp = String.join("\n", resolution_.versionOverrides().entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).toList());
            var repositories_fp = String.join("\n", repositories);
            var extensions_fp = String.join("\n", extensions);
            var fingerprint = overrides_fp + "\n" + repositories_fp + "\n" + extensions_fp + "\n" + downloadSources + "\n" + downloadJavadoc;
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(fingerprint.getBytes(StandardCharsets.UTF_8));

            extensionsHash_ = StringUtils.encodeHexLower(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    public void fingerprintDependencies(List<Repository> repositories, DependencyScopes dependencies, boolean downloadSources, boolean downloadJavadoc) {
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
        finger_print.append(downloadSources)
            .append('\n')
            .append(downloadJavadoc)
            .append('\n');

        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(finger_print.toString().getBytes(StandardCharsets.UTF_8));
            dependenciesHash_ = StringUtils.encodeHexLower(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    public boolean isExtensionHashValid() {
        return validateExtensionsHash(extensionsHash_);
    }

    private File getCacheFile() {
        return new File(bldLibDir_, BLD_CACHE);
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

    public boolean isDependenciesHashValid() {
        return validateDependenciesHash(dependenciesHash_);
    }

    private boolean validateDependenciesHash(String hash) {
        var properties = hashProperties();
        if (properties.isEmpty()) {
            return false;
        }

        return hash.equals(properties.getProperty(PROPERTY_DEPENDENCIES_HASH));
    }

    public void writeCache() {
        writeCache(null);
    }

    public void writeCache(List<File> extensionsLocalArtifacts) {
        var properties = hashProperties();

        try {
            if (extensionsHash_ != null) {
                properties.put(PROPERTY_EXTENSIONS_HASH, extensionsHash_);
            }

            if (extensionsLocalArtifacts != null) {
                var extensions_local = new StringBuilder();
                for (var file : extensionsLocalArtifacts) {
                    if (file.exists() && file.canRead()) {
                        extensions_local.append("\n").append(file.lastModified()).append(':').append(file.getAbsolutePath());
                    }
                }
                properties.put(PROPERTY_EXTENSIONS_LOCAL, extensions_local.toString());
            }

            if (dependenciesHash_ != null) {
                properties.put(PROPERTY_DEPENDENCIES_HASH, dependenciesHash_);
            }

            bldLibDir_.mkdirs();

            try (var writer = new BufferedWriter(new FileWriter(getCacheFile()))) {
                properties.store(writer, null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

