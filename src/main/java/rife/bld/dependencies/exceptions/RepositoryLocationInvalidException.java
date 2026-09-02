/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies.exceptions;

import java.io.Serial;

/**
 * Thrown when a property that declares a repository doesn't hold a location.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 3.0
 */
public class RepositoryLocationInvalidException extends DependencyException {
    @Serial private static final long serialVersionUID = 8341744937383567285L;

    private final String property_;
    private final String location_;

    public RepositoryLocationInvalidException(String property, String location) {
        super("The property '" + property + "' has to hold a repository location, not '" + location + "'. A location " +
              "is a URL, like https://repo.example.com/releases/, or an absolute path on the file system.");

        property_ = property;
        location_ = location;
    }

    /**
     * Returns the property that declares the repository.
     *
     * @return the property name
     * @since 3.0
     */
    public String getProperty() {
        return property_;
    }

    /**
     * Returns the location it holds.
     *
     * @return the invalid location
     * @since 3.0
     */
    public String getLocation() {
        return location_;
    }
}
