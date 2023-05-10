/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies.exceptions;

import rife.bld.dependencies.Dependency;
import rife.tools.StringUtils;

import java.io.Serial;
import java.util.Set;

public class DependencyXmlParsingErrorException extends DependencyException {
    @Serial private static final long serialVersionUID = -1050469071912675264L;

    private final Dependency dependency_;
    private final String location_;
    private final Set<String> errors_;

    public DependencyXmlParsingErrorException(Dependency dependency, String location, Set<String> errors) {
        super("Unable to parse artifact document for dependency '" + dependency + "' from '" + location + "' :\n" + StringUtils.join(errors, "\n"));

        dependency_ = dependency;
        location_ = location;
        errors_ = errors;
    }

    public Dependency getDependency() {
        return dependency_;
    }

    public String getLocation() {
        return location_;
    }

    public Set<String> getErrors() {
        return errors_;
    }
}