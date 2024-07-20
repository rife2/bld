/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import static rife.bld.dependencies.VersionNumber.parseOrNull;

/**
 * Represents the basic functionality of a dependency version.
 *
 * @since 2.0
 */
public interface Version extends Comparable<Version> {
    /**
     * Parses a version from a string representation.
     * <p>
     * If the string can't be successfully parsed as a semantic {@link VersionNumber},
     * it will be parsed as a {@link VersionGeneric}.
     *
     * @param version the version string to parse
     * @return the parsed version instance
     * @since 2.0
     */
    static Version parse(String version) {
        if (version == null || version.isEmpty()) {
            return VersionNumber.UNKNOWN;
        }

        var result = parseOrNull(version);
        if (result != null) {
            return result;
        }

        // bld doesn't support version ranges at this time
        if (version.startsWith("[") || version.startsWith("(")) {
            return VersionNumber.UNKNOWN;
        }

        return new VersionGeneric(version);
    }

    /**
     * Retrieves the qualifier of the version.
     *
     * @return this version's qualifier
     * @since 2.0
     */
    String qualifier();

    /**
     * Retrieves the version number with a different qualifier.
     *
     * @return this version number with a different qualifier
     * @since 2.0
     */
    Version withQualifier(String qualifier);

    /**
     * Indicates whether this is a snapshot version.
     *
     * @return {@code true} if this is a snapshot version; or
     * {@code false} otherwise
     * @since 2.0
     */
    boolean isSnapshot();

    @Override
    int compareTo(Version other);

    @Override
    String toString();

    @Override
    boolean equals(Object other);

    @Override
    int hashCode();
}
