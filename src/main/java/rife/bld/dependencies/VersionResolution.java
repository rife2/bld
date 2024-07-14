/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.ioc.HierarchicalProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for managing version overrides for dependencies.
 * <p>
 * It allows users to specify a property keys with the prefix "{@code bld.override}" where the values will be parsed as
 * a comma-separated list of dependencies with the versions that should override any other versions that are encountered.
 * <p>
 * For instance:
 * <pre>
 * bld.override=com.uwyn.rife2:bld-tests-badge:1.4.7,com.h2database:h2:2.2.222
 * </pre>
 * <p>
 * Multiple override properties can be used by simply adding differentiators behind the "{@code bld.override}" keys.
 * <p>
 * For instance:
 * <pre>
 * bld.override-tests=com.uwyn.rife2:bld-tests-badge:1.4.7
 * bld.override-h2=com.h2database:h2:2.2.222
 * </pre>
 * @since 2.0
 */
public class VersionResolution {
    /**
     * The prefix for property keys used to override versions of dependencies.
     * @since 2.0
     */
    public static final String PROPERTY_OVERRIDE_PREFIX = "bld.override";

    private final Map<String, VersionNumber> versionOverrides_ = new HashMap<>();

    /**
     * Returns a dummy {@code VersionResolution} instance that doesn't override anything.
     *
     * @return the dummy instance
     * @since 2.0
     */
    static VersionResolution dummy() {
        return new VersionResolution(null);
    }

    /**
     * Creates a new instance of the {@code VersionReslution} class from hierarchical properties that
     * are passed in.
     * <p>
     * The actual version overrides are determined at instantiation time and any future changes to the
     * properties will not influence version resolution.
     *
     * @param properties the hierarchical properties that will be used to determine the version overrides
     * @since 2.0
     */
    public VersionResolution(HierarchicalProperties properties) {
        if (properties != null) {
            for (var name : properties.getNames()) {
                if (name.startsWith(PROPERTY_OVERRIDE_PREFIX)) {
                    for (var override : properties.get(name).toString().split(",")) {
                        override = override.trim();
                        if (!override.isBlank()) {
                            var dependency = Dependency.parse(override);
                            if (dependency != null) {
                                versionOverrides_.put(dependency.toArtifactString(), dependency.version());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Overrides the version of a given dependency with the corresponding overridden version.
     *
     * @param original the dependency for which the version needs to be overridden
     * @return the overridden version if it is available; or the original version otherwise
     * @since 2.0
     */
    public VersionNumber overrideVersion(Dependency original) {
        var overridden = versionOverrides_.get(original.toArtifactString());
        if (overridden == null) {
            return original.version();
        }
        return overridden;
    }

    /**
     * Overrides the version of a given dependency with the corresponding overridden version and
     * creates a new dependency object with the overridden version, if needed.
     *
     * @param original the dependency for which the version needs to be overridden
     * @return the dependency with the overridden version if it's available; or the original dependency otherwise
     * @since 2.0
     */
    public Dependency overrideDependency(Dependency original) {
        var overridden = versionOverrides_.get(original.toArtifactString());
        if (overridden == null) {
            return original;
        }
        return new Dependency(original.groupId(),
            original.artifactId(),
            overridden,
            original.classifier(),
            original.type(),
            original.exclusions(),
            original.parent());
    }

    /**
     * Returns the map of version overrides, where the key is the name of the dependency and the value is the overridden version.
     *
     * @return the map of version overrides
     * @since 2.0
     */
    public Map<String, VersionNumber> versionOverrides() {
        return versionOverrides_;
    }
}
