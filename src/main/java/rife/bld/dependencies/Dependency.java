/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Contains the information required to describe a dependency in the build system.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class Dependency {
    public static final String CLASSIFIER_SOURCES = "sources";
    public static final String CLASSIFIER_JAVADOC = "javadoc";

    /**
     * The dependency type name for a JAR file that can be placed either on the class-path or on the module-path.
     *
     * @since 2.1
     */
    public static final String TYPE_JAR = "jar";

    /**
     * The dependency type name for a JAR file to unconditionally place on the class-path.
     *
     * @since 2.1
     */
    public static final String TYPE_CLASSPATH_JAR = "classpath-jar";

    /**
     * The dependency type name for a JAR file to unconditionally place on the module-path.
     *
     * @since 2.1
     */
    // see https://github.com/apache/maven/blob/maven-4.0.0-beta-3/api/maven-api-core/src/main/java/org/apache/maven/api/Type.java
    public static final String TYPE_MODULAR_JAR = "modular-jar";

    private final String groupId_;
    private final String artifactId_;
    private final Version version_;
    private final String classifier_;
    private final String type_;
    private final ExclusionSet exclusions_;
    private final Dependency parent_;
    private final HashSet<String> excludedClassifiers_;

    public Dependency(String groupId, String artifactId) {
        this(groupId, artifactId, null, null, null);
    }

    public Dependency(String groupId, String artifactId, Version version) {
        this(groupId, artifactId, version, null, null);
    }

    public Dependency(String groupId, String artifactId, Version version, String classifier) {
        this(groupId, artifactId, version, classifier, null);
    }

    public Dependency(String groupId, String artifactId, Version version, String classifier, String type) {
        this(groupId, artifactId, version, classifier, type, null);
    }

    public Dependency(String groupId, String artifactId, Version version, String classifier, String type, ExclusionSet exclusions) {
        this(groupId, artifactId, version, classifier, type, exclusions, null);
    }

    public Dependency(String groupId, String artifactId, Version version, String classifier, String type, ExclusionSet exclusions, Dependency parent) {
        if (type == null) {
            type = TYPE_JAR;
        }
        if (parent != null && parent.isModularJar() && TYPE_JAR.equals(type)) {
            type = TYPE_MODULAR_JAR;
        }

        this.groupId_ = groupId;
        this.artifactId_ = artifactId;
        this.version_ = (version == null ? VersionNumber.UNKNOWN : version);
        this.classifier_ = (classifier == null ? "" : classifier);
        this.type_ = type;
        this.exclusions_ = (exclusions == null ? new ExclusionSet() : exclusions);
        this.parent_ = parent;
        this.excludedClassifiers_ = new HashSet<>();
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
        var version = Version.parse(matcher.group("version"));
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
        return new Dependency(groupId_, artifactId_, VersionNumber.UNKNOWN, classifier_, type_);
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
        exclusions_.add(new DependencyExclusion(groupId, artifactId));
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
        return new Dependency(groupId_, artifactId_, version_, classifier, type_);
    }

    /**
     * Exclude the sources artifact from download operations.
     *
     * @return this dependency instance
     * @since 2.1
     */
    public Dependency excludeSources() {
        excludedClassifiers_.add(CLASSIFIER_SOURCES);
        return this;
    }

    /**
     * Exclude the javadoc artifact from download operations.
     *
     * @return this dependency instance
     * @since 2.1
     */
    public Dependency excludeJavadoc() {
        excludedClassifiers_.add(CLASSIFIER_JAVADOC);
        return this;
    }

    /**
     * Returns a filename that corresponds to the dependency information.
     *
     * @return a filename for the dependency
     * @since 1.5.4
     */
    public String toFileName() {
        var result = new StringBuilder(artifactId());
        result.append('-').append(version());
        if (!classifier().isEmpty()) {
            result.append('-').append(classifier());
        }
        result.append('.').append(type());
        return result.toString();

    }

    /**
     * Returns a string representation of the dependency in the format "groupId:artifactId".
     *
     * @return the string representation of the dependency
     * @since 2.0
     */
    public String toArtifactString() {
        return groupId_ + ':' + artifactId_;
    }

    public String toString() {
        var result = new StringBuilder(groupId_).append(':').append(artifactId_);
        if (!version_.equals(VersionNumber.UNKNOWN)) {
            result.append(':').append(version_);
        }
        if (!classifier_.isEmpty()) {
            result.append(':').append(classifier_);
        }
        if (!type_.isEmpty() && !TYPE_JAR.equals(type_)) {
            result.append('@').append(type_);
        }
        return result.toString();
    }

    /**
     * Returns this dependency's {@code groupId}.
     *
     * @return the {@code groupId} of this dependency
     * @since 1.5
     */
    public String groupId() {
        return groupId_;
    }

    /**
     * Returns this dependency's {@code artifactId}.
     *
     * @return the {@code artifactId} of this dependency
     * @since 1.5
     */
    public String artifactId() {
        return artifactId_;
    }

    /**
     * Returns this dependency's {@code version}.
     *
     * @return the {@code version} of this dependency
     * @since 1.5
     */
    public Version version() {
        return version_;
    }

    /**
     * Returns this dependency's {@code classifier}.
     *
     * @return the {@code classifier} of this dependency
     * @since 1.5
     */
    public String classifier() {
        return classifier_;
    }

    /**
     * Returns this dependency's {@code type}.
     *
     * @return the {@code type} of this dependency
     * @since 1.5
     */
    public String type() {
        return type_;
    }

    /**
     * Returns this dependency's {@code exclusions} for transitive resolution.
     *
     * @return the {@code exclusions} of this dependency
     * @since 1.5
     */
    public ExclusionSet exclusions() {
        return exclusions_;
    }

    public HashSet<String> excludedClassifiers() {
        return excludedClassifiers_;
    }

    /**
     * Returns this dependency's {@code parent} dependency that created this
     * dependency (only for information purposes).
     *
     * @return the {@code parent} of this dependency
     * @since 1.5
     */
    public Dependency parent() {
        return parent_;
    }

    /**
     * Indicates whether this dependency specifically is a classpath jar or not.
     *
     * @return {@code true} when this dependency specifically is a classpath jar; or {@code false} otherwise
     * @since 2.1
     */
    public boolean isClasspathJar() {
        return Module.TYPE_CLASSPATH_JAR.equals(type_);
    }

    /**
     * Indicates whether this dependency is a modular jar or not.
     *
     * @return {@code true} when this dependency is a modular jar; or {@code false} otherwise
     * @since 2.1
     */
    public boolean isModularJar() {
        return Module.TYPE_MODULAR_JAR.equals(type_);
    }

    private static String normalizedJarType(String type) {
        if (TYPE_JAR.equals(type) || TYPE_MODULAR_JAR.equals(type) || TYPE_CLASSPATH_JAR.equals(type)) {
            return TYPE_JAR;
        }
        return type;
    }
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency that)) return false;
        return groupId_.equals(that.groupId_) &&
            artifactId_.equals(that.artifactId_) &&
            classifier_.equals(that.classifier_) &&
            normalizedJarType(type_).equals(normalizedJarType(that.type_));
    }

    public int hashCode() {
        return Objects.hash(groupId_, artifactId_, classifier_, normalizedJarType(type_));
    }
}
