/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies.exceptions;

import rife.bld.dependencies.Dependency;

import java.io.Serial;

public class ArtifactNotFoundException extends DependencyException {
    @Serial private static final long serialVersionUID = 3137804373567469249L;

    private final Dependency dependency_;
    private final String location_;

    public ArtifactNotFoundException(Dependency dependency, String location) {
        super("Couldn't find artifact for dependency '" + dependency + "' at " + location);

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