/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import java.util.*;

/**
 * Provides the properties information for publication.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.9.2
 */
public class PublishProperties extends LinkedHashMap<String, String> {
    private static final String MAVEN_COMPILER_SOURCE = "maven.compiler.source";
    private static final String MAVEN_COMPILER_TARGET = "maven.compiler.target";

    /**
     * Sets the value of the 'maven.compiler.source' property.
     *
     * @param value the value to be set for the 'maven.compiler.source' property
     * @return this {@code PomProperties} instance
     * @since 1.9.2
     */
    public PublishProperties mavenCompilerSource(Integer value) {
        if (value == null) {
            remove(MAVEN_COMPILER_SOURCE);
        }
        else {
            put(MAVEN_COMPILER_SOURCE, String.valueOf(value));
        }
        return this;
    }

    /**
     * Retrieves the value of the 'maven.compiler.source' property.
     *
     * @return the value of the 'maven.compiler.source' property
     * @since 1.9.2
     */
    public Integer mavenCompilerSource() {
        var value = get(MAVEN_COMPILER_SOURCE);
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value);
    }

    /**
     * Sets the value of the 'maven.compiler.target' property.
     *
     * @param value the value to be set for the 'maven.compiler.target' property
     * @return this {@code PomProperties} instance
     * @since 1.9.2
     */
    public PublishProperties mavenCompilerTarget(Integer value) {
        if (value == null) {
            remove(MAVEN_COMPILER_TARGET);
        }
        else {
            put(MAVEN_COMPILER_TARGET, String.valueOf(value));
        }
        return this;
    }

    /**
     * Retrieves the value of the 'maven.compiler.target' property.
     *
     * @return the value of the 'maven.compiler.target' property
     * @since 1.9.2
     */
    public Integer mavenCompilerTarget() {
        var value = get(MAVEN_COMPILER_TARGET);
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value);
    }
}