/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.bld.dependencies.exceptions.*;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves a dependency within a list of Maven-compatible repositories.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class DependencyResolver {
    private final ArtifactRetriever retriever_;
    private final List<Repository> repositories_;
    private final Dependency dependency_;

    private MavenMetadata metadata_ = null;
    private MavenMetadata snapshotMetadata_ = null;

    /**
     * Creates a new resolver for a particular dependency.
     * <p>
     * The repositories will be checked in the order they're listed.
     *
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the resolution
     * @param dependency   the dependency to resolve
     * @since 1.5.18
     */
    public DependencyResolver(ArtifactRetriever retriever, List<Repository> repositories, Dependency dependency) {
        retriever_ = retriever;
        if (repositories == null) {
            repositories = Collections.emptyList();
        }
        repositories_ = repositories;
        dependency_ = dependency;
    }

    /**
     * Checks whether the dependency exists in any of the provided repositories.
     *
     * @return {@code true} if the dependency exists; {@code false} otherwise
     * @since 1.5
     */
    public boolean exists() {
        try {
            if (getMavenMetadata() == null) {
                return false;
            }
            if (!dependency_.version().equals(VersionNumber.UNKNOWN)) {
                return getMavenMetadata().getVersions().contains(dependency_.version());
            }
            return true;
        } catch (ArtifactNotFoundException | ArtifactRetrievalErrorException e) {
            return false;
        }
    }


    /**
     * Resolves the dependency version in the provided repositories.
     * <p>
     * When the dependency was defined without a specific version number,
     * the latest version number will be returned if the dependency exists.
     * If the dependency couldn't be found and no specific version number
     * was provided, {@linkplain VersionNumber#UNKNOWN} will be returned.
     *
     * @return the resolved version
     * @since 1.5
     */
    public VersionNumber resolveVersion() {
        var version = dependency_.version();
        if (version.equals(VersionNumber.UNKNOWN)) {
            return latestVersion();
        }
        return version;
    }

    /**
     * Retrieves the direct dependencies of the resolved dependency for the
     * provided scopes.
     *
     * @param scopes the scopes to return the direct dependencies for
     * @return the requested direct dependencies; or an empty {@linkplain DependencySet}
     * if no direct dependencies could be found or the dependency doesn't exist in
     * the provided repositories
     * @since 1.5
     */
    public DependencySet getDirectDependencies(Scope... scopes) {
        var pom_dependencies = getMavenPom(dependency_).getDependencies(scopes);
        var result = new DependencySet();
        for (var dependency : pom_dependencies) {
            result.add(dependency.convertToDependency());
        }
        return result;
    }

    /**
     * Retrieves all the transitive dependencies of the resolved dependency for the
     * provided scopes. This includes the dependency of this resolver also.
     * <p>
     * This can be a slow and expensive operation since querying continues through
     * the complete POM hierarchy until all transitive dependencies have been found.
     *
     * @param scopes the scopes to return the transitive dependencies for
     * @return the requested transitive dependencies; or an empty {@linkplain DependencySet}
     * if no transitive dependencies could be found or the dependency doesn't exist in
     * the provided repositories
     * @since 1.5
     */
    public DependencySet getAllDependencies(Scope... scopes) {
        var result = new DependencySet();
        result.add(dependency_);

        var dependency_queue = new ArrayList<PomDependency>();

        var parent = dependency_;
        var next_dependencies = getMavenPom(parent).getDependencies(scopes);

        while (parent != null && next_dependencies != null) {
            // remove any next dependencies that are already queued
            dependency_queue.forEach(next_dependencies::remove);
            // remove any next dependencies that match the current exclusion context
            final var exclusion_context = parent;
            next_dependencies.removeIf(it -> matchesExclusions(exclusion_context, it));
            // add all next dependencies to the queue
            dependency_queue.addAll(next_dependencies);

            // unless we find a next set of dependencies to add, stop resolving
            parent = null;
            next_dependencies = null;

            // iterate through the dependency queue until we find one that isn't
            // part of the results yet
            while (!dependency_queue.isEmpty()) {
                var candidate = dependency_queue.remove(0);
                var dependency = candidate.convertToDependency();
                if (!result.contains(dependency)) {
                    result.add(dependency);

                    // we found a dependency that was added to the result, get its
                    // dependencies so that they can be added to the queue after
                    // filtering
                    parent = dependency;
                    next_dependencies = new DependencyResolver(retriever_, repositories_, dependency).getMavenPom(parent).getDependencies(scopes);
                    break;
                }
            }
        }
        return result;
    }

    private boolean matchesExclusions(Dependency context, PomDependency checked) {
        while (context != null) {
            if (context.exclusions() != null) {
                for (var exclusion : context.exclusions()) {
                    if (exclusion.matches(checked)) {
                        return true;
                    }
                }
            }
            context = context.parent();
        }

        if (dependency_.exclusions() != null) {
            for (var exclusion : dependency_.exclusions()) {
                if (exclusion.matches(checked)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Retrieves all the versions of the resolved dependency.
     *
     * @return this dependency's version list; or an empty list if the dependency
     * couldn't be found in the provided repositories
     * @since 1.5
     */
    public List<VersionNumber> listVersions() {
        return getMavenMetadata().getVersions();
    }

    /**
     * Retrieves the latest version of the resolved dependency.
     *
     * @return this dependency's latest version; or {@link VersionNumber#UNKNOWN}
     * if the dependency couldn't be found in the provided repositories
     * @since 1.5
     */
    public VersionNumber latestVersion() {
        return getMavenMetadata().getLatest();
    }

    /**
     * Retrieves the release version of the resolved dependency.
     *
     * @return this dependency's release version; or {@link VersionNumber#UNKNOWN}
     * if the dependency couldn't be found in the provided repositories
     * @since 1.5
     */
    public VersionNumber releaseVersion() {
        return getMavenMetadata().getRelease();
    }

    /**
     * Transfers the artifacts for the resolved dependency into the provided directory.
     * <p>
     * The destination directory must exist and be writable.
     *
     * @param directory the directory to transfer the dependency artifact into
     * @return the artifact that was successfully transferred; or {@code null} if no artifact was transferred
     * @throws DependencyTransferException when an error occurred during the transfer
     * @since 1.5.10
     */
    public RepositoryArtifact transferIntoDirectory(File directory)
    throws DependencyTransferException {
        for (var artifact : getTransferArtifacts()) {
            try {
                if (retriever_.transferIntoDirectory(artifact, directory)) {
                    return artifact;
                }
            } catch (IOException e) {
                throw new DependencyTransferException(dependency_, artifact.location(), directory, e);
            }
        }
        return null;
    }

    /**
     * Retrieve the repositories that are used by this dependency resolver.
     *
     * @return the dependency resolver's repositories
     * @since 1.5
     */
    public List<Repository> repositories() {
        return repositories_;
    }

    /**
     * Retrieve the dependency that is resolved.
     *
     * @return the resolved dependency
     * @since 1.5
     */
    public Dependency dependency() {
        return dependency_;
    }

    /**
     * Retrieves all the potential locations for the dependency
     * within the provided repositories.
     *
     * @return a list of potential transfer locations
     * @since 1.5.10
     */
    public List<String> getTransferLocations() {
        return getTransferArtifacts().stream().map(RepositoryArtifact::location).toList();
    }

    /**
     * Returns the main Maven metadata for this dependency
     *
     * @return this dependency's metadata
     * @since 1.5.8
     */
    public MavenMetadata getMavenMetadata() {
        if (metadata_ == null) {
            var locations = getMetadataLocations();
            metadata_ = parseMavenMetadata(locations);
        }

        return metadata_;
    }

    /**
     * THe Maven metadata with snapshot information when this is a snapshot version.
     *
     * @return Maven metadata if this is a snapshot dependency version; or
     * {@code null} if this version is not a snapshot
     * @since 1.5.8
     */
    public MavenMetadata getSnapshotMavenMetadata() {
        if (snapshotMetadata_ == null) {
            final var version = resolveVersion();
            if (version.isSnapshot()) {
                var locations = getSnapshotMetadataLocations();
                snapshotMetadata_ = parseMavenMetadata(locations);
            }
        }

        return snapshotMetadata_;
    }

    private List<RepositoryArtifact> getTransferArtifacts() {
        final var version = resolveVersion();
        final VersionNumber pom_version;
        if (version.isSnapshot()) {
            var metadata = getSnapshotMavenMetadata();
            pom_version = metadata.getSnapshot();
        } else {
            pom_version = version;
        }

        return getArtifactLocations().stream().map(a -> {
            var result = new StringBuilder();
            result.append(version).append("/").append(dependency_.artifactId()).append("-").append(pom_version);
            if (!dependency_.classifier().isEmpty()) {
                result.append("-").append(dependency_.classifier());
            }
            var type = dependency_.type();
            if (type == null) {
                type = "jar";
            }
            result.append(".").append(type);

            return a.appendPath(result.toString());
        }).toList();
    }

    private List<RepositoryArtifact> getArtifactLocations() {
        return repositories_.stream().map(repository -> new RepositoryArtifact(repository, repository.getArtifactLocation(dependency_))).toList();
    }

    private List<RepositoryArtifact> getMetadataLocations() {
        return getArtifactLocations().stream().map(a -> a.appendPath(a.repository().getMetadataName())).toList();
    }

    private List<RepositoryArtifact> getSnapshotMetadataLocations() {
        var version = resolveVersion();
        return getArtifactLocations().stream().map(a -> a.appendPath(version + "/" + a.repository().getMetadataName())).toList();
    }

    private MavenMetadata parseMavenMetadata(List<RepositoryArtifact> artifacts) {
        RepositoryArtifact retrieved_artifact = null;
        String metadata = null;
        for (var artifact : artifacts) {
            try {
                var content = retriever_.readString(artifact);
                if (content == null) {
                    throw new ArtifactNotFoundException(dependency_, artifact.location());
                }

                retrieved_artifact = artifact;
                metadata = content;

                break;
            } catch (FileUtilsErrorException e) {
                if (e.getCause() instanceof FileNotFoundException) {
                    continue;
                }
                throw new ArtifactRetrievalErrorException(dependency_, artifact.location(), e);
            }
        }

        if (metadata == null) {
            throw new ArtifactNotFoundException(dependency_, artifacts.stream().map(RepositoryArtifact::location).collect(Collectors.joining(", ")));
        }

        var xml = new Xml2MavenMetadata();
        if (!xml.processXml(metadata)) {
            throw new DependencyXmlParsingErrorException(dependency_, retrieved_artifact.location(), xml.getErrors());
        }

        return xml;
    }

    private List<RepositoryArtifact> getPomLocations() {
        final var version = resolveVersion();
        final VersionNumber pom_version;
        if (version.isSnapshot()) {
            var metadata = getSnapshotMavenMetadata();
            pom_version = metadata.getSnapshot();
        } else {
            pom_version = version;
        }

        return getArtifactLocations().stream().map(a -> a.appendPath(version + "/" + dependency_.artifactId() + "-" + pom_version + ".pom")).toList();
    }

    Xml2MavenPom getMavenPom(Dependency parent) {
        RepositoryArtifact retrieved_artifact = null;
        String pom = null;
        var artifacts = getPomLocations();

        for (var artifact : artifacts) {
            try {
                var content = retriever_.readString(artifact);
                if (content == null) {
                    throw new ArtifactNotFoundException(dependency_, artifact.location());
                }

                retrieved_artifact = artifact;
                pom = content;

                break;
            } catch (FileUtilsErrorException e) {
                if (e.getCause() instanceof FileNotFoundException) {
                    continue;
                }
                throw new ArtifactRetrievalErrorException(dependency_, artifact.location(), e);
            }
        }

        if (pom == null) {
            throw new ArtifactNotFoundException(dependency_, artifacts.stream().map(RepositoryArtifact::location).collect(Collectors.joining(", ")));
        }

        var xml = new Xml2MavenPom(parent, retriever_, repositories_);
        if (!xml.processXml(pom)) {
            throw new DependencyXmlParsingErrorException(dependency_, retrieved_artifact.location(), xml.getErrors());
        }

        return xml;
    }
}
