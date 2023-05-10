/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Contains the information required to describe a dependency version number.
 * <p>
 * This operates according to the versioning scheme specified by Maven.
 * <p>
 * When the version number is undefined, {@link VersionNumber#UNKNOWN} should be used.
 *
 * @param major     the major version component
 * @param minor     the minor version component
 * @param revision  the revision of the version
 * @param qualifier a string qualifier for the version
 * @param separator the separator used to separate the qualifier from the version number
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public record VersionNumber(Integer major, Integer minor, Integer revision, String qualifier, String separator) implements Comparable<VersionNumber> {
    public static final String SNAPSHOT_QUALIFIER = "SNAPSHOT";

    /**
     * Singleton to use when the version is not specified.
     *
     * @since 1.5
     */
    public static final VersionNumber UNKNOWN = new VersionNumber(0, 0, 0, "");

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(?<major>\\d+)(?:\\.(?<minor>\\d+)(?:\\.(?<revision>\\d+))?)?(?:(?<separator>[.\\-])(?<qualifier>.*[^.\\-]))??$");

    /**
     * Parses a version number from a string representation.
     * <p>
     * If the string can't be successfully parsed, {@link VersionNumber#UNKNOWN} will be returned.
     *
     * @param version the version string to parse
     * @return a parsed instance of {@code VersionNumber}; or
     * {@link VersionNumber#UNKNOWN} when the string couldn't be parsed
     * @since 1.5
     */
    public static VersionNumber parse(String version) {
        if (version == null || version.isEmpty()) {
            return UNKNOWN;
        }

        var matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return UNKNOWN;
        }

        var major = matcher.group("major");
        var minor = matcher.group("minor");
        var revision = matcher.group("revision");

        var major_integer = (major != null ? Integer.parseInt(major) : null);
        var minor_integer = (minor != null ? Integer.parseInt(minor) : null);
        var revision_integer = (revision != null ? Integer.parseInt(revision) : null);

        var qualifier = matcher.group("qualifier");
        var separator = matcher.group("separator");

        return new VersionNumber(major_integer, minor_integer, revision_integer, qualifier, separator);
    }

    /**
     * Constructs a version number with only a major component.
     *
     * @param major the major version component
     * @since 1.5
     */
    public VersionNumber(Integer major) {
        this(major, null, null, "");
    }

    /**
     * Constructs a version number with a major and minor component.
     *
     * @param major the major version component
     * @param minor the minor version component
     * @since 1.5
     */
    public VersionNumber(Integer major, Integer minor) {
        this(major, minor, null, "");
    }

    /**
     * Constructs a version number with major, minor and revision components.
     *
     * @param major    the major version component
     * @param minor    the minor version component
     * @param revision the version revision component
     * @since 1.5
     */
    public VersionNumber(Integer major, Integer minor, Integer revision) {
        this(major, minor, revision, "");
    }

    /**
     * Constructs a complete version number with qualifier, the separator will default to "{@code -}".
     *
     * @param major     the major version component
     * @param minor     the minor version component
     * @param revision  the version revision component
     * @param qualifier the version qualifier
     * @since 1.5
     */
    public VersionNumber(Integer major, Integer minor, Integer revision, String qualifier) {
        this(major, minor, revision, qualifier, "-");
    }

    /**
     * Constructs a complete version number with qualifier.
     *
     * @param major     the major version component
     * @param minor     the minor version component
     * @param revision  the version revision component
     * @param qualifier the version qualifier
     * @param separator the separator for the version qualifier
     * @since 1.5
     */
    public VersionNumber(Integer major, Integer minor, Integer revision, String qualifier, String separator) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.qualifier = (qualifier == null ? "" : qualifier);
        this.separator = separator;
    }

    /**
     * Retrieves the base version number without the qualifier.
     *
     * @return the base version number instance
     * @since 1.5
     */
    public VersionNumber getBaseVersion() {
        return new VersionNumber(major, minor, revision, null);
    }

    /**
     * Retrieves the version number with a different qualifier.
     *
     * @return this version number with a different qualifier
     * @since 1.5.8
     */
    public VersionNumber withQualifier(String qualifier) {
        return new VersionNumber(major, minor, revision, qualifier);
    }

    /**
     * Returns a primitive integer for the major version component.
     *
     * @return the major version component as an {@code int}
     * @since 1.5
     */
    public int majorInt() {
        return major == null ? 0 : major;
    }

    /**
     * Returns a primitive integer for the minor version component.
     *
     * @return the minor version component as an {@code int}
     * @since 1.5
     */
    public int minorInt() {
        return minor == null ? 0 : minor;
    }

    /**
     * Returns a primitive integer for the version revision component.
     *
     * @return the version revision component as an {@code int}
     * @since 1.5
     */
    public int revisionInt() {
        return revision == null ? 0 : revision;
    }

    /**
     * Indicates whether this is a snapshot version.
     *
     * @return {@code true} if this is a snapshot version; or
     * {@code false} otherwise
     * @since 1.5.8
     */
    public boolean isSnapshot() {
        return qualifier().toUpperCase().contains(SNAPSHOT_QUALIFIER);
    }

    public int compareTo(VersionNumber other) {
        if (majorInt() != other.majorInt()) {
            return majorInt() - other.majorInt();
        }
        if (minorInt() != other.minorInt()) {
            return minorInt() - other.minorInt();
        }
        if (revisionInt() != other.revisionInt()) {
            return revisionInt() - other.revisionInt();
        }

        if (qualifier.equals(other.qualifier)) {
            return 0;
        } else if (qualifier.isEmpty()) {
            return 1;
        } else if (other.qualifier.isEmpty()) {
            return -1;
        }

        return qualifier.toLowerCase().compareTo(other.qualifier.toLowerCase());
    }

    public String toString() {
        var version = new StringBuilder();
        version.append(majorInt());
        if (minor != null || revision != null) {
            version.append(".");
            version.append(minorInt());
        }
        if (revision != null) {
            version.append(".");
            version.append(revisionInt());
        }
        if (qualifier != null && !qualifier.isEmpty()) {
            version.append(separator);
            version.append(qualifier);
        }
        return version.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VersionNumber && compareTo((VersionNumber) other) == 0;
    }

    @Override
    public int hashCode() {
        int result = majorInt();
        result = 31 * result + minorInt();
        result = 31 * result + revisionInt();
        result = 31 * result + Objects.hashCode(qualifier.toLowerCase());
        return result;
    }
}
