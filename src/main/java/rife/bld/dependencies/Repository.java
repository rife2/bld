/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.bld.dependencies.exceptions.RepositoryLocationInvalidException;
import rife.ioc.HierarchicalProperties;
import rife.tools.StringEncryptor;

import java.io.File;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

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
    public static final Repository APACHE = new Repository("https://repo.maven.apache.org/maven2/");
    public static final Repository GOOGLE = new Repository("https://maven.google.com/");
    public static final Repository GOOGLE_MAVEN_CENTRAL = new Repository("https://maven-central.storage-download.googleapis.com/maven2/");
    public static final Repository GOOGLE_MAVEN_CENTRAL_EU = new Repository("https://maven-central-eu.storage-download.googleapis.com/maven2/");
    public static final Repository GOOGLE_MAVEN_CENTRAL_ASIA = new Repository("https://maven-central-asia.storage-download.googleapis.com/maven2/");
    public static final Repository MAVEN_CENTRAL = new Repository("https://repo1.maven.org/maven2/");
    public static final Repository SECURECHAIN_REBUILT = new Repository("https://nexus-repo.corp.cloudlinux.com/repository/tuxcare_rebuilt");
    public static final Repository SECURECHAIN_VETTED = new Repository("https://nexus-repo.corp.cloudlinux.com/repository/tuxcare_vetted");
    public static final String OSSRH_STAGING_API_DOMAIN = "ossrh-staging-api.central.sonatype.com";
    public static final Repository CENTRAL_RELEASES = new Repository("https://" + OSSRH_STAGING_API_DOMAIN + "/service/local/staging/deploy/maven2/");
    public static final Repository CENTRAL_SNAPSHOTS = new Repository("https://central.sonatype.com/repository/maven-snapshots/");
    public static final Repository RIFE2_RELEASES = new Repository("https://repo.rife2.com/releases/");
    public static final Repository RIFE2_SNAPSHOTS = new Repository("https://repo.rife2.com/snapshots/");
    /**
     * The repository a name resolves to when nothing declares it. It has no
     * location, so it can be put in a list without breaking it, and it is
     * left out wherever repositories are actually used.
     *
     * @since 3.0
     */
    public static final Repository UNRESOLVED = new Repository(null);

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
     * @return the repository instance; or {@link #UNRESOLVED} when the name
     * doesn't resolve to anything and isn't a location itself, which is
     * reported as a warning so that the rest of the build carries on without it
     * @throws RepositoryLocationInvalidException when the property that declares it doesn't hold a location
     * @since 1.5.12
     */
    public static Repository resolveRepository(HierarchicalProperties properties, String locationOrName) {
        if (properties != null && properties.contains(PROPERTY_BLD_REPO_PREFIX + locationOrName)) {
            var location = properties.getValueString(PROPERTY_BLD_REPO_PREFIX + locationOrName);
            if (!isLocation(location)) {
                throw new RepositoryLocationInvalidException(PROPERTY_BLD_REPO_PREFIX + locationOrName, location);
            }
            var username = properties.getValueString(PROPERTY_BLD_REPO_PREFIX + locationOrName + PROPERTY_BLD_REPO_USERNAME_SUFFIX);
            var password = properties.getValueString(PROPERTY_BLD_REPO_PREFIX + locationOrName + PROPERTY_BLD_REPO_PASSWORD_SUFFIX);
            return new Repository(location, username, password);
        }

        return switch (locationOrName) {
            case "APACHE" -> Repository.APACHE;
            case "GOOGLE" -> Repository.GOOGLE;
            case "GOOGLE_MAVEN_CENTRAL" -> Repository.GOOGLE_MAVEN_CENTRAL;
            case "GOOGLE_MAVEN_CENTRAL_EU" -> Repository.GOOGLE_MAVEN_CENTRAL_EU;
            case "GOOGLE_MAVEN_CENTRAL_ASIA" -> Repository.GOOGLE_MAVEN_CENTRAL_ASIA;
            case "MAVEN_CENTRAL" -> Repository.MAVEN_CENTRAL;
            case "MAVEN_LOCAL" -> Repository.MAVEN_LOCAL;
            case "RIFE2_RELEASES" -> Repository.RIFE2_RELEASES;
            case "RIFE2_SNAPSHOTS" -> Repository.RIFE2_SNAPSHOTS;
            case "SECURECHAIN_REBUILT" -> SECURECHAIN_REBUILT;
            case "SECURECHAIN_VETTED" -> SECURECHAIN_VETTED;
            case "CENTRAL_RELEASES" -> Repository.CENTRAL_RELEASES;
            case "CENTRAL_SNAPSHOTS" -> Repository.CENTRAL_SNAPSHOTS;
            default -> {
                // a name that resolves to nothing used to become a repository
                // at that relative path, which publishes into a directory
                // instead of where it was meant to go
                if (!isLocation(locationOrName)) {
                    System.out.println("WARNING: '" + locationOrName + "' isn't a repository, declare it with a '" +
                                       PROPERTY_BLD_REPO_PREFIX + locationOrName + "' property, use one of the " +
                                       "built-in names, or give a location, skipping.");
                    yield UNRESOLVED;
                }
                yield new Repository(locationOrName);
            }
        };
    }

    /**
     * Keeps only the repositories that can actually be used, leaving out the
     * ones a name didn't resolve to. The warning was already given when the
     * name was resolved, so this is silent.
     *
     * @param repositories the repositories to filter, may be {@code null}
     * @return the repositories that have a location
     * @since 3.0
     */
    public static List<Repository> usable(List<Repository> repositories) {
        if (repositories == null) {
            return List.of();
        }
        return repositories.stream().filter(r -> r != null && !r.isUnresolved()).toList();
    }

    /**
     * Indicates whether this repository has no location, which is what a name
     * that nothing declares resolves to.
     *
     * @return {@code true} when the repository can't be used; or {@code false} otherwise
     * @since 3.0
     */
    public boolean isUnresolved() {
        return location() == null;
    }

    /**
     * Indicates whether text can be used as a repository location, which is
     * a URL with a scheme or an absolute path on the file system. Anything
     * else is a name that has to resolve to one.
     *
     * @param text the text to check
     * @return {@code true} when the text is a location; or {@code false} otherwise
     * @since 3.0
     */
    public static boolean isLocation(String text) {
        return text != null &&
               (text.contains("://") ||
                text.startsWith("file:") ||
                text.startsWith("/") ||
                WINDOWS_ABSOLUTE_PATH.matcher(text).find());
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

    private final static Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^\\p{L}:\\\\");

    private boolean isWindowsLocation() {
        return WINDOWS_ABSOLUTE_PATH.matcher(location()).find();
    }

    /**
     * Indicates whether this repository is local.
     *
     * @return {@code true} when this repository is local; or
     * {@code false} otherwise
     * @since 1.5.10
     */
    public boolean isLocal() {
        return location().startsWith("/") || location().startsWith("file:") || isWindowsLocation();
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
        var separator = "/";
        var result = new StringBuilder();
        if (isLocal()) {
            if (isWindowsLocation()) {
                separator = File.separator;
            }
            if (location().startsWith("file://")) {
                result.append(location().substring("file://".length()));
            } else {
                result.append(location());
            }
        } else {
            result.append(location());
        }
        if (!location().endsWith(separator)) {
            result.append(separator);
        }
        var group_path = groupId.replace(".", separator);
        return result.append(group_path).append(separator).append(artifactId).append(separator).toString();
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
            result.append(':');
            try {
                result.append(StringEncryptor.MD5HLO.performEncryption(username(), null));
                if (password() != null) {
                    result.append(':');
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