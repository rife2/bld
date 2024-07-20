/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

/**
 * Represents the basic functionality of a dependency version.
 *
 * @since 2.0
 */
public interface Version extends Comparable<Version> {
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
