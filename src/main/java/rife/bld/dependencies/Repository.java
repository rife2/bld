/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.ioc.HierarchicalProperties;
import rife.tools.StringEncryptor;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

/**
 * Contains the information required to locate a Maven-compatible repository.
 *
 * @param location the base location of the repository
 * @param username the username to access the repository
 * @param password the password to access the repository
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public record Repository(String location, String username, String password) {
    public static Repository MAVEN_LOCAL = null;
    public static final Repository MAVEN_CENTRAL = new Repository("https://repo1.maven.org/maven2/");
    public static final Repository SONATYPE_RELEASES = new Repository("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/");
    public static final Repository SONATYPE_SNAPSHOTS = new Repository("https://s01.oss.sonatype.org/content/repositories/snapshots/");
    public static final Repository SONATYPE_SNAPSHOTS_LEGACY = new Repository("https://oss.sonatype.org/content/repositories/snapshots/");
    public static final Repository APACHE = new Repository("https://repo.maven.apache.org/maven2/");
    public static final Repository RIFE2_RELEASES = new Repository("https://repo.rife2.com/releases/");
    public static final Repository RIFE2_SNAPSHOTS = new Repository("https://repo.rife2.com/snapshots/");

    private static final String MAVEN_LOCAL_REPO_PROPERTY = "maven.repo.local";

    public static final String PROPERTY_BLD_REPO_PREFIX = "bld.repo.";
    public static final String PROPERTY_BLD_REPO_USERNAME_SUFFIX = ".username";
    public static final String PROPERTY_BLD_REPO_PASSWORD_SUFFIX = ".password";

    /**
     * This method will be called as soon as hierarchical properties
     * are initialized in the build executor. It is not intended to be called
     * manually.
     *
     * @param properties the hierarchical properties to use for resolving
     *                   the maven local repository
     * @since 1.5.12
     */
    public static void resolveMavenLocal(HierarchicalProperties properties) {
        var user_home = properties.getValueString("user.home");
        if (user_home == null) {
            user_home = System.getProperty("user.home");
        }
        var maven_local = properties.getValueString(
            MAVEN_LOCAL_REPO_PROPERTY,
            Path.of(user_home, ".m2", "repository").toString());
        MAVEN_LOCAL = new Repository(maven_local);
    }

    /**
     * Resolves the repository in the provided hierarchical properties.
     * <p>
     * For instance, using the name {@code myrepo} will look for the following properties:<br>
     * {@code bld.repo.myrepo}<br>
     * {@code bld.repo.myrepo.username} (optional)<br>
     * {@code bld.repo.myrepo.password} (optional)
     * <p>
     * If the {@code bld.repo.myrepo} property isn't found, the {@code locationOrName}
     * parameter will be used as a location instead.
     *
     * @param properties     the hierarchical properties to look into
     * @param locationOrName the text to resolve a repository name or to be used as a location
     * @return the repository instance
     * @since 1.5.12
     */
    public static Repository resolveRepository(HierarchicalProperties properties, String locationOrName) {
        if (properties != null && properties.contains(PROPERTY_BLD_REPO_PREFIX + locationOrName)) {
            var location = properties.getValueString(PROPERTY_BLD_REPO_PREFIX + locationOrName);
            var username = properties.getValueString(PROPERTY_BLD_REPO_PREFIX + locationOrName + PROPERTY_BLD_REPO_USERNAME_SUFFIX);
            var password = properties.getValueString(PROPERTY_BLD_REPO_PREFIX + locationOrName + PROPERTY_BLD_REPO_PASSWORD_SUFFIX);
            return new Repository(location, username, password);
        }

        return switch (locationOrName) {
            case "MAVEN_LOCAL" -> Repository.MAVEN_LOCAL;
            case "MAVEN_CENTRAL" -> Repository.MAVEN_CENTRAL;
            case "SONATYPE_RELEASES" -> Repository.SONATYPE_RELEASES;
            case "SONATYPE_SNAPSHOTS" -> Repository.SONATYPE_SNAPSHOTS;
            case "SONATYPE_SNAPSHOTS_LEGACY" -> Repository.SONATYPE_SNAPSHOTS_LEGACY;
            case "APACHE" -> Repository.APACHE;
            case "RIFE2_RELEASES" -> Repository.RIFE2_RELEASES;
            case "RIFE2_SNAPSHOTS" -> Repository.RIFE2_SNAPSHOTS;
            default -> new Repository(locationOrName);
        };
    }

    /**
     * Creates a new repository with only a location.
     *
     * @param location the location to create the repository for
     * @since 1.5
     */
    public Repository(String location) {
        this(location, null, null);
    }

    /**
     * Indicates whether this repository is local.
     *
     * @return {@code true} when this repository is local; or
     * {@code false} otherwise
     * @since 1.5.10
     */
    public boolean isLocal() {
        return location().startsWith("/") || location().startsWith("file:");
    }

    /**
     * Creates a new repository instance of the same location, but with
     * different credentials.
     *
     * @param username the username to access the repository
     * @param password the password to access the repository
     * @return the new repository
     * @since 1.5.10
     */
    public Repository withCredentials(String username, String password) {
        return new Repository(location(), username, password);
    }

    /**
     * Constructs the location for a dependency if it would be located in this repository.
     *
     * @param dependency the dependency to create the location for
     * @return the constructed location
     * @since 1.5.10
     */
    public String getArtifactLocation(Dependency dependency) {
        return getArtifactLocation(dependency.groupId(), dependency.artifactId());
    }

    /**
     * Constructs the location for a dependency if it would be located in this repository.
     *
     * @param groupId    the groupId dependency to create the location for
     * @param artifactId the artifactId dependency to create the location for
     * @return the constructed location
     * @since 1.5.10
     */
    public String getArtifactLocation(String groupId, String artifactId) {
        var group_path = groupId.replace(".", "/");
        var result = new StringBuilder();
        if (isLocal()) {
            if (location().startsWith("file://")) {
                result.append(location().substring("file://".length()));
            } else {
                result.append(location());
            }
        } else {
            result.append(location());
        }
        if (!location().endsWith("/")) {
            result.append("/");
        }
        return result.append(group_path).append("/").append(artifactId).append("/").toString();
    }

    /**
     * Returns the appropriate metadata name.
     *
     * @return the metadata name for this repository.
     * @since 1.5.10
     */
    public String getMetadataName() {
        if (isLocal()) {
            return "maven-metadata-local.xml";
        } else {
            return "maven-metadata.xml";
        }
    }

    public String toString() {
        var result = new StringBuilder(location);
        if (username() != null) {
            result.append(":");
            try {
                result.append(StringEncryptor.MD5HLO.performEncryption(username(), null));
                if (password() != null) {
                    result.append(":");
                    result.append(StringEncryptor.MD5HLO.performEncryption(password(), null));
                }
            } catch (NoSuchAlgorithmException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        }
        return result.toString();
    }
}