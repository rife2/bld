/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies.exceptions;

import java.io.Serial;

/**
 * Thrown when a repository name doesn't resolve to a repository and isn't a
 * location itself.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 3.0
 */
public class RepositoryNotResolvedException extends DependencyException {
    @Serial private static final long serialVersionUID = 5716374338734893925L;

    private final String name_;
    private final String property_;

    public RepositoryNotResolvedException(String name, String property) {
        super("'" + name + "' isn't a repository. Declare it with a '" + property + "' property, use one of the " +
              "built-in names, or give a location: a URL, like https://repo.example.com/releases/, or an absolute " +
              "path on the file system.");

        name_ = name;
        property_ = property;
    }

    /**
     * Returns the name that couldn't be resolved.
     *
     * @return the unresolved name
     * @since 3.0
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the property that would declare it.
     *
     * @return the property name
     * @since 3.0
     */
    public String getProperty() {
        return property_;
    }
}
