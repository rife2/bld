/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

/**
 * Describes a developer in {@link PublishInfo}.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.7
 */
public class PublishDeveloper {
    private String id_;
    private String name_;
    private String email_;
    private String url_;

    /**
     * Provides the developer's ID.
     *
     * @param id the developer's ID
     * @return this {@code PublishDeveloper} instance
     * @since 1.5.7
     */
    public PublishDeveloper id(String id) {
        id_ = id;
        return this;
    }

    /**
     * Retrieves the developer's ID.
     *
     * @return the developer's ID
     * @since 1.5.7
     */
    public String id() {
        return id_;
    }

    /**
     * Provides the developer's name.
     *
     * @param name the developer's name
     * @return this {@code PublishDeveloper} instance
     * @since 1.5.7
     */
    public PublishDeveloper name(String name) {
        name_ = name;
        return this;
    }

    /**
     * Retrieves the developer's name.
     *
     * @return the developer's name
     * @since 1.5.7
     */
    public String name() {
        return name_;
    }

    /**
     * Provides the developer's email.
     *
     * @param email the developer's email
     * @return this {@code PublishDeveloper} instance
     * @since 1.5.7
     */
    public PublishDeveloper email(String email) {
        email_ = email;
        return this;
    }

    /**
     * Retrieves the developer's email.
     *
     * @return the developer's email
     * @since 1.5.7
     */
    public String email() {
        return email_;
    }

    /**
     * Provides the developer's URL.
     *
     * @param url the developer's URL
     * @return this {@code PublishDeveloper} instance
     * @since 1.5.7
     */
    public PublishDeveloper url(String url) {
        url_ = url;
        return this;
    }

    /**
     * Retrieves the developer's URL.
     *
     * @return the developer's URL
     * @since 1.5.7
     */
    public String url() {
        return url_;
    }
}
