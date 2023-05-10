/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

/**
 * Represents an artifact location in a repository.
 *
 * @param repository the repository of the artifact
 * @param location   the location of the artifact in the repository
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.6
 */
public record RepositoryArtifact(Repository repository, String location) {
    public RepositoryArtifact appendPath(String path) {
        return new RepositoryArtifact(repository, location + path);
    }

    public String toString() {
        return repository + ":" + location;
    }
}
