/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.ioc.HierarchicalProperties;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class is responsible for managing the versions that are applied
 * during dependency resolution.
 * <p>
 * It allows users to specify a property key with the prefix "{@code bld.override}" where the values will be parsed as
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
 * <p>
 * It can also import versions from the dependency management sections of
 * bills of materials (BOMs). When resolving versions, the
 * following precedence applies: a version from a "{@code bld.override}"
 * property always wins, followed by a version explicitly declared
 * in the build file, followed by a version from a BOM. Dependencies that
 * are declared without a version and that are not covered by a BOM,
 * resolve to their latest version.
 * <p>
 * It also captures other dependency resolution preferences, like the number
 * of parallel artifact transfers through the "{@code bld.transferParallelism}"
 * property and the number of parallel POM retrievals during transitive
 * dependency resolution through the "{@code bld.resolutionParallelism}"
 * property.
 * @since 2.0
 */
public class VersionResolution {
    /**
     * The prefix for property keys used to override versions of dependencies.
     * @since 2.0
     */
    public static final String PROPERTY_OVERRIDE_PREFIX = "bld.override";

    /**
     * The property key that determines how many artifact transfers are
     * performed in parallel, {@code 1} makes them sequential.
     * @since 2.4.0
     */
    public static final String PROPERTY_TRANSFER_PARALLELISM = "bld.transferParallelism";
    private static final int DEFAULT_TRANSFER_PARALLELISM = 6;

    /**
     * The property key that determines how many POMs are speculatively
     * retrieved in parallel during transitive dependency resolution,
     * {@code 1} disables the parallel retrieval.
     * @since 2.4.0
     */
    public static final String PROPERTY_RESOLUTION_PARALLELISM = "bld.resolutionParallelism";
    private static final int DEFAULT_RESOLUTION_PARALLELISM = 6;

    private final Map<String, Version> versionOverrides_ = new HashMap<>();
    private final Map<String, Version> bomVersions_;
    private final int transferParallelism_;
    private final int resolutionParallelism_;

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
     * Creates a new instance of the {@code VersionResolution} class from hierarchical properties that
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
        transferParallelism_ = parseParallelism(properties, PROPERTY_TRANSFER_PARALLELISM, DEFAULT_TRANSFER_PARALLELISM);
        resolutionParallelism_ = parseParallelism(properties, PROPERTY_RESOLUTION_PARALLELISM, DEFAULT_RESOLUTION_PARALLELISM);
        bomVersions_ = Map.of();
    }

    private VersionResolution(VersionResolution base, Map<String, Version> bomVersions) {
        versionOverrides_.putAll(base.versionOverrides_);
        transferParallelism_ = base.transferParallelism_;
        resolutionParallelism_ = base.resolutionParallelism_;
        bomVersions_ = Map.copyOf(bomVersions);
    }

    /**
     * Creates a new instance of the {@code VersionResolution} class from
     * hierarchical properties and bills of materials whose dependency
     * management sections supply versions during resolution.
     * <p>
     * The BOMs are imported in the order they're provided, the first BOM
     * that manages a particular dependency determines its version. The
     * version overrides of the properties take precedence over any BOM
     * version, they also apply when resolving the BOMs themselves.
     *
     * @param properties   the hierarchical properties that will be used to determine the version overrides
     * @param retriever    the retriever to use to get the BOMs
     * @param repositories the repositories to resolve the BOMs in
     * @param boms         the BOMs to import
     * @since 2.4.0
     */
    public VersionResolution(HierarchicalProperties properties, ArtifactRetriever retriever, List<Repository> repositories, Collection<Bom> boms) {
        this(new VersionResolution(properties), retriever, repositories, boms);
    }

    private VersionResolution(VersionResolution base, ArtifactRetriever retriever, List<Repository> repositories, Collection<Bom> boms) {
        this(base, resolveBomVersions(base, retriever, repositories, boms));
    }

    VersionResolution withBoms(ArtifactRetriever retriever, List<Repository> repositories, Collection<Bom> boms) {
        if (boms == null || boms.isEmpty()) {
            return this;
        }
        return new VersionResolution(this, retriever, repositories, boms);
    }

    private static Map<String, Version> resolveBomVersions(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories, Collection<Bom> boms) {
        var bom_versions = new HashMap<String, Version>();
        if (boms != null) {
            for (var bom : boms) {
                var pom = new DependencyResolver(resolution, retriever, repositories, bom).getMavenPom(bom);
                for (var managed : pom.getManagedDependencies()) {
                    if (managed.version() != null && !managed.version().isBlank()) {
                        var dependency = managed.convertToDependency();
                        bom_versions.putIfAbsent(dependency.toArtifactString(), dependency.version());
                    }
                }
            }
        }
        return bom_versions;
    }

    private static int parseParallelism(HierarchicalProperties properties, String property, int defaultValue) {
        if (properties != null) {
            var parallelism = properties.getValueString(property);
            if (parallelism != null && !parallelism.isBlank()) {
                try {
                    return Math.max(1, Integer.parseInt(parallelism.trim()));
                } catch (NumberFormatException e) {
                    Logger.getLogger("rife.bld").warning("Unable to parse the " + property + " property as an integer: '" + parallelism + "', using " + defaultValue + " instead");
                }
            }
        }
        return defaultValue;
    }

    /**
     * Overrides the version of a given dependency with the corresponding overridden version.
     *
     * @param original the dependency for which the version needs to be overridden
     * @return the overridden version if it is available; or the original version otherwise
     * @since 2.0
     */
    public Version overrideVersion(Dependency original) {
        var overridden = versionOverrides_.get(original.toArtifactString());
        if (overridden != null) {
            return overridden;
        }
        if (VersionNumber.UNKNOWN.equals(original.version())) {
            var bom_version = bomVersions_.get(original.toArtifactString());
            if (bom_version != null) {
                return bom_version;
            }
        }
        return original.version();
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
        return withVersion(original, overridden);
    }

    /**
     * Applies version overrides to a dependency that was explicitly declared
     * in the build file.
     * <p>
     * A version from the {@code bld.override} property always takes
     * precedence. A bill of materials version is only applied when the
     * dependency was declared without a version, an explicitly declared
     * version is never rewritten by a BOM.
     *
     * @param declared the declared dependency to apply overrides to
     * @return the dependency with the overridden version if one applies; or
     * the original dependency otherwise
     * @since 2.4.0
     */
    public Dependency overrideDeclaredDependency(Dependency declared) {
        var overridden = versionOverrides_.get(declared.toArtifactString());
        if (overridden == null && VersionNumber.UNKNOWN.equals(declared.version())) {
            overridden = bomVersions_.get(declared.toArtifactString());
        }
        if (overridden == null) {
            return declared;
        }
        return withVersion(declared, overridden);
    }

    /**
     * Applies version overrides to a transitive dependency that was
     * encountered during resolution.
     * <p>
     * A version from the {@code bld.override} property always takes
     * precedence, followed by a matching bill of materials version that
     * pins the transitive dependency regardless of the version its parent
     * POM declared.
     *
     * @param transitive the transitive dependency to apply overrides to
     * @return the dependency with the overridden version if one applies; or
     * the original dependency otherwise
     * @since 2.4.0
     */
    public Dependency overrideTransitiveDependency(Dependency transitive) {
        var overridden = versionOverrides_.get(transitive.toArtifactString());
        if (overridden == null) {
            overridden = bomVersions_.get(transitive.toArtifactString());
        }
        if (overridden == null) {
            return transitive;
        }
        return withVersion(transitive, overridden);
    }

    private static Dependency withVersion(Dependency original, Version version) {
        return new Dependency(original.groupId(),
            original.artifactId(),
            version,
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
    public Map<String, Version> versionOverrides() {
        return versionOverrides_;
    }

    /**
     * Returns the map of versions that were imported from bills of
     * materials, where the key is the name of the dependency and the value
     * is the managed version.
     *
     * @return the map of BOM versions
     * @since 2.4.0
     */
    public Map<String, Version> bomVersions() {
        return bomVersions_;
    }

    /**
     * Returns the number of artifact transfers that are performed in parallel,
     * {@code 1} means transfers are sequential.
     *
     * @return the number of parallel artifact transfers
     * @since 2.4.0
     */
    public int transferParallelism() {
        return transferParallelism_;
    }

    /**
     * Returns the number of POMs that are speculatively retrieved in parallel
     * during transitive dependency resolution, {@code 1} means the parallel
     * retrieval is disabled.
     *
     * @return the number of parallel POM retrievals
     * @since 2.4.0
     */
    public int resolutionParallelism() {
        return resolutionParallelism_;
    }
}
