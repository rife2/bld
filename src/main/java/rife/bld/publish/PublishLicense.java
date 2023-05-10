/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

/**
 * Describes a license in {@link PublishInfo}.
 *
 * @since 1.5.7
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 */
public class PublishLicense {
    private String name_;
    private String url_;

    /**
     * Provides the license's name.
     *
     * @param name the license's name
     * @return this {@code PublishDeveloper} instance
     * @since 1.5.7
     */
    public PublishLicense name(String name) {
        name_ = name;
        return this;
    }

    /**
     * Retrieves the license's name.
     *
     * @return the license's name
     * @since 1.5.7
     */
    public String name() {
        return name_;
    }

    /**
     * Provides the license's URL.
     *
     * @param url the license's URL
     * @return this {@code PublishDeveloper} instance
     * @since 1.5.7
     */
    public PublishLicense url(String url) {
        url_ = url;
        return this;
    }

    /**
     * Retrieves the license's URL.
     *
     * @return the license's URL
     * @since 1.5.7
     */
    public String url() {
        return url_;
    }
}
