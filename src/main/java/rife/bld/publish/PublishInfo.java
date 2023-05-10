/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import rife.bld.dependencies.VersionNumber;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the information to perform a publish operation.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.7
 */
public class PublishInfo {
    private String groupId_ = null;
    private String artifactId_ = null;
    private VersionNumber version_ = null;
    private String name_ = null;
    private String description_ = null;
    private String url_ = null;
    private String signGpgPath_ = null;
    private String signKey_ = null;
    private String signPassphrase_ = null;

    private final List<PublishLicense> licenses_ = new ArrayList<>();
    private final List<PublishDeveloper> developers_ = new ArrayList<>();
    private PublishScm scm_ = null;

    /**
     * Provides the project's group ID.
     *
     * @param groupId the project's group ID.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo groupId(String groupId) {
        groupId_ = groupId;
        return this;
    }

    /**
     * Retrieves the project's group ID.
     *
     * @return the project's group ID.
     * @since 1.5.7
     */
    public String groupId() {
        return groupId_;
    }

    /**
     * Provides the project's artifact ID.
     *
     * @param artifactId the project's artifact ID.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo artifactId(String artifactId) {
        artifactId_ = artifactId;
        return this;
    }

    /**
     * Retrieves the project's artifact ID.
     *
     * @return the project's artifact ID.
     * @since 1.5.7
     */
    public String artifactId() {
        return artifactId_;
    }

    /**
     * Provides the project's version.
     *
     * @param version the project's version.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo version(VersionNumber version) {
        version_ = version;
        return this;
    }

    /**
     * Retrieves the project's version.
     *
     * @return the project's version.
     * @since 1.5.7
     */
    public VersionNumber version() {
        return version_;
    }

    /**
     * Provides the project's name.
     *
     * @param name the project's name.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo name(String name) {
        name_ = name;
        return this;
    }

    /**
     * Retrieves the project's name.
     *
     * @return the project's name.
     * @since 1.5.7
     */
    public String name() {
        return name_;
    }

    /**
     * Provides the project's description.
     *
     * @param description the project's description.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo description(String description) {
        description_ = description;
        return this;
    }

    /**
     * Retrieves the project's description.
     *
     * @return the project's description.
     * @since 1.5.7
     */
    public String description() {
        return description_;
    }

    /**
     * Provides the project's URL.
     *
     * @param url the project's URL.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo url(String url) {
        url_ = url;
        return this;
    }

    /**
     * Retrieves the project's URL.
     *
     * @return the project's URL.
     * @since 1.5.7
     */
    public String url() {
        return url_;
    }

    /**
     * Provides the custompath to the {@code gpg} executable used for signing.
     * <p>
     * By default, {@code gpg} will be used.
     *
     * @param path the {@code gpg} executable path
     * @return this {@code PublishInfo} instance
     * @since 1.5.8
     */
    public PublishInfo signGpgPath(String path) {
        signGpgPath_ = path;
        return this;
    }

    /**
     * Retrieves the custom path of the {@code gpg} executable.
     *
     * @return the custom path of the {@code gpg} executable; or
     * {@code null} if no custom path was set
     * @since 1.5.8
     */
    public String signGpgPath() {
        return signGpgPath_;
    }

    /**
     * Provides the sign key used to create a signature for each published artifact.
     * <p>
     * When the sign key is provided, signing will activate.
     * When the sign key is not set, publishing will proceed without signed artifacts.
     *
     * @param key the sign key
     * @return this {@code PublishInfo} instance
     * @since 1.5.8
     */
    public PublishInfo signKey(String key) {
        signKey_ = key;
        return this;
    }

    /**
     * Retrieves the sign key.
     *
     * @return the sign key; or
     * {@code null} if no sign key was provided
     * @since 1.5.8
     */
    public String signKey() {
        return signKey_;
    }

    /**
     * Provides the passphrase used to create a signature for each published artifact.
     * <p>
     * If a GPG agent is running locally, the passphrase might not be necessary.
     *
     * @param passphrase the passphrase used for signing
     * @return this {@code PublishInfo} instance
     * @since 1.5.8
     */
    public PublishInfo signPassphrase(String passphrase) {
        signPassphrase_ = passphrase;
        return this;
    }

    /**
     * Retrieves the passphrase used to create a signature for each published artifact.
     * @return the passphrase; or
     * {@code null} if no passphrase was provided
     * @since 1.5.8
     */
    public String signPassphrase() {
        return signPassphrase_;
    }

    /**
     * Adds a project developer.
     *
     * @param developer a project developer.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo developer(PublishDeveloper developer) {
        developers_.add(developer);
        return this;
    }

    /**
     * Provides project developer.
     *
     * @param developers project developers.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo developers(List<PublishDeveloper> developers) {
        developers_.addAll(developers);
        return this;
    }

    /**
     * Retrieves the project's developers.
     *
     * @return the project's developers.
     * @since 1.5.7
     */
    public List<PublishDeveloper> developers() {
        return developers_;
    }

    /**
     * Adds a project license.
     *
     * @param license a project license.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo license(PublishLicense license) {
        licenses_.add(license);
        return this;
    }

    /**
     * Provides project licenses.
     *
     * @param licenses project licenses.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo licenses(List<PublishLicense> licenses) {
        licenses_.addAll(licenses);
        return this;
    }

    /**
     * Retrieves the project's licenses.
     *
     * @return the project's licenses.
     * @since 1.5.7
     */
    public List<PublishLicense> licenses() {
        return licenses_;
    }

    /**
     * Provides the project's SCM info.
     *
     * @param scm the project's SCM info.
     * @return this {@code PublishInfo} instance
     * @since 1.5.7
     */
    public PublishInfo scm(PublishScm scm) {
        scm_ = scm;
        return this;
    }

    /**
     * Retrieves the project's SCM info.
     *
     * @return the project's SCM info.
     * @since 1.5.7
     */
    public PublishScm scm() {
        return scm_;
    }
}
