/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.*;

/**
 * Contains the information required to describe a dependency with all
 * the details stored in a Maven POM descriptor.
 * <p>
 * This is used by the {@linkplain DependencyResolver} while traversing
 * the dependency graph, eventually resulting into fully resolved
 * {@linkplain Dependency} instances.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public record PomDependency(String groupId, String artifactId, String version, String classifier, String type,
                            String scope, String optional, ExclusionSet exclusions, Dependency parent) {
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PomDependency that = (PomDependency) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(artifactId, that.artifactId) && Objects.equals(classifier, that.classifier) && Objects.equals(type, that.type);
    }

    boolean isPomImport() {
        return "pom".equals(type()) && "import".equals(scope());
    }

    Dependency convertToDependency() {
        return new Dependency(
            groupId(),
            artifactId(),
            VersionNumber.parse(version()),
            classifier(),
            type(),
            exclusions(),
            parent());
    }

    public int hashCode() {
        return Objects.hash(groupId, artifactId, classifier, type);
    }
}
