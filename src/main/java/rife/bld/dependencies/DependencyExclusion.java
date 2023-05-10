/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.Objects;

/**
 * Contains the information to describe a dependency exclusion.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public record DependencyExclusion(String groupId, String artifactId) {
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyExclusion that = (DependencyExclusion) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId);
    }

    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    boolean matches(PomDependency dependency) {
        return (groupId().equals("*") && artifactId().equals("*")) ||
               (groupId().equals("*") && artifactId().equals(dependency.artifactId())) ||
               (groupId().equals(dependency.groupId()) && artifactId().equals("*")) ||
               (groupId().equals(dependency.groupId()) && artifactId().equals(dependency.artifactId()));

    }
}
