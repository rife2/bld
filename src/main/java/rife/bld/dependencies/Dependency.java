/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Contains the information required to describe an url dependency in the build system.
 *
 * @param groupId    the dependency group identifier
 * @param artifactId the dependency url identifier
 * @param version    the dependency version
 * @param classifier the dependency classier
 * @param type       the dependency type
 * @param exclusions the dependency exclusions for transitive resolution
 * @param parent     the parent dependency that created this dependency (only for information purposes)
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public record Dependency(String groupId, String artifactId, VersionNumber version, String classifier, String type, ExclusionSet exclusions, Dependency parent) {
    public static final String CLASSIFIER_SOURCES = "sources";
    public static final String CLASSIFIER_JAVADOC = "javadoc";

    public Dependency(String groupId, String artifactId) {
        this(groupId, artifactId, null, null, null);
    }

    public Dependency(String groupId, String artifactId, VersionNumber version) {
        this(groupId, artifactId, version, null, null);
    }

    public Dependency(String groupId, String artifactId, VersionNumber version, String classifier) {
        this(groupId, artifactId, version, classifier, null);
    }

    public Dependency(String groupId, String artifactId, VersionNumber version, String classifier, String type) {
        this(groupId, artifactId, version, classifier, type, null);
    }

    public Dependency(String groupId, String artifactId, VersionNumber version, String classifier, String type, ExclusionSet exclusions) {
        this(groupId, artifactId, version, classifier, type, null, null);
    }

    public Dependency(String groupId, String artifactId, VersionNumber version, String classifier, String type, ExclusionSet exclusions, Dependency parent) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = (version == null ? VersionNumber.UNKNOWN : version);
        this.classifier = (classifier == null ? "" : classifier);
        this.type = (type == null ? "jar" : type);
        this.exclusions = (exclusions == null ? new ExclusionSet() : exclusions);
        this.parent = parent;
    }

    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("^(?<groupId>[^:@]+):(?<artifactId>[^:@]+)(?::(?<version>[^:@]+)(?::(?<classifier>[^:@]+))?)?(?:@(?<type>[^:@]+))?$");

    /**
     * Parses a dependency from a string representation.
     * The format is {@code groupId:artifactId:version:classifier@type}.
     * The {@code version}, {@code classifier} and {@code type} are optional.
     * <p>
     * If the string can't be successfully parsed, {@code null} will be returned.
     *
     * @param dependency the dependency string to parse
     * @return a parsed instance of {@code Dependency}; or
     * {@code null} when the string couldn't be parsed
     * @since 1.5.2
     */
    public static Dependency parse(String dependency) {
        if (dependency == null || dependency.isEmpty()) {
            return null;
        }

        var matcher = DEPENDENCY_PATTERN.matcher(dependency);
        if (!matcher.matches()) {
            return null;
        }

        var groupId = matcher.group("groupId");
        var artifactId = matcher.group("artifactId");
        var version = VersionNumber.parse(matcher.group("version"));
        var classifier = matcher.group("classifier");
        var type = matcher.group("type");

        return new Dependency(groupId, artifactId, version, classifier, type);
    }

    /**
     * Returns the base dependency of this dependency, replacing the version number
     * with an unknown version number.
     *
     * @return this dependency's base dependency
     * @since 1.5
     */
    public Dependency baseDependency() {
        return new Dependency(groupId, artifactId, VersionNumber.UNKNOWN, classifier, type);
    }

    /**
     * Adds an exclusion to this dependency.
     *
     * @param groupId    the exclusion group identifier, use {@code "*"} to exclude all groupIds
     * @param artifactId the exclusion url identifier, use {@code "*"} to exclude all artifactIds
     * @return this dependency instance
     * @since 1.5
     */
    public Dependency exclude(String groupId, String artifactId) {
        exclusions.add(new DependencyExclusion(groupId, artifactId));
        return this;
    }

    /**
     * Returns a new dependency with the same data, except for the provided classifier.
     *
     * @param classifier the classifier to use for the new dependency
     * @return the new dependency with the changed classifier
     * @since 1.5.6
     */
    public Dependency withClassifier(String classifier) {
        return new Dependency(groupId, artifactId, version, classifier, type);
    }

    /**
     * Returns a filename that corresponds to the dependency information.
     *
     * @return a filename for the dependency
     * @since 1.5.4
     */
    public String toFileName() {
        var result = new StringBuilder(artifactId());
        result.append("-").append(version());
        if (!classifier().isEmpty()) {
            result.append("-").append(classifier());
        }
        result.append(".").append(type());
        return result.toString();

    }

    public String toString() {
        var result = new StringBuilder(groupId).append(":").append(artifactId);
        if (!version.equals(VersionNumber.UNKNOWN)) {
            result.append(":").append(version);
        }
        if (!classifier.isEmpty()) {
            result.append(":").append(classifier);
        }
        if (!type.isEmpty() && !type.equals("jar")) {
            result.append("@").append(type);
        }
        return result.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Dependency) o;
        return groupId.equals(that.groupId) &&
               artifactId.equals(that.artifactId) &&
               classifier.equals(that.classifier) &&
               type.equals(that.type);
    }

    public int hashCode() {
        return Objects.hash(groupId, artifactId, classifier, type);
    }
}
