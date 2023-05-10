/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

/**
 * Describes an SCM in {@link PublishInfo}.
 *
 * @since 1.5.7
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 */
public class PublishScm {
    private String connection_;
    private String developerConnection_;
    private String url_;

    /**
     * Provides the SCM's connection.
     *
     * @param connection the SCM's connection
     * @return this {@code PublishScm} instance
     * @since 1.5.7
     */
    public PublishScm connection(String connection) {
        connection_ = connection;
        return this;
    }

    /**
     * Retrieves the SCM's connection.
     *
     * @return the SCM's connection
     * @since 1.5.7
     */
    public String connection() {
        return connection_;
    }

    /**
     * Provides the SCM's developer connection.
     *
     * @param connection the SCM's developer connection
     * @return this {@code PublishScm} instance
     * @since 1.5.7
     */
    public PublishScm developerConnection(String connection) {
        developerConnection_ = connection;
        return this;
    }

    /**
     * Retrieves the SCM's developer connection.
     *
     * @return the SCM's developer connection
     * @since 1.5.7
     */
    public String developerConnection() {
        return developerConnection_;
    }

    /**
     * Provides the SCM's URL.
     *
     * @param url the SCM's URL
     * @return this {@code PublishScm} instance
     * @since 1.5.7
     */
    public PublishScm url(String url) {
        url_ = url;
        return this;
    }

    /**
     * Retrieves the SCM's URL.
     *
     * @return the SCM's URL
     * @since 1.5.7
     */
    public String url() {
        return url_;
    }
}
