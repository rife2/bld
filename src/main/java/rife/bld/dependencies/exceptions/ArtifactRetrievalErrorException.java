/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies.exceptions;

import rife.bld.dependencies.Dependency;

import java.io.Serial;

public class ArtifactRetrievalErrorException extends DependencyException {
    @Serial private static final long serialVersionUID = 5570184718213503548L;

    private final Dependency dependency_;
    private final String location_;

    public ArtifactRetrievalErrorException(Dependency dependency, String location, Throwable e) {
        super("Unexpected error while retrieving artifact for dependency '" + dependency + "' from '" + location + "'", e);

        dependency_ = dependency;
        location_ = location;
    }

    public Dependency getDependency() {
        return dependency_;
    }

    public String getLocation() {
        return location_;
    }
}