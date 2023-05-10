/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies.exceptions;

import rife.bld.dependencies.Dependency;

import java.io.File;
import java.io.Serial;

public class DependencyTransferException extends DependencyException {
    @Serial private static final long serialVersionUID = 2128741620203670830L;

    private final Dependency dependency_;
    private final String location_;
    private final File destination_;

    public DependencyTransferException(Dependency dependency, String location, File destination, Throwable e) {
        super("Unable to transfer dependency '" + dependency + "' from '" + location + "' into '" + destination + "'", e);

        dependency_ = dependency;
        location_ = location;
        destination_ = destination;
    }

    public Dependency getDependency() {
        return dependency_;
    }

    public String getLocation() {
        return location_;
    }

    public File getDestination() {
        return destination_;
    }
}