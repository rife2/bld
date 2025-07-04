/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations.exceptions;

import rife.tools.HttpUtils;

import java.io.Serial;

/**
 * When thrown, indicates that something went wrong during the use of a rest API call.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.2.2
 */
public class RestApiException extends RuntimeException {
    @Serial private static final long serialVersionUID = -6753423938407177328L;

    private final String url_;

    public RestApiException(String url, int status) {
        super("An error occurred while using rest API at '" + url + "'\nHTTP status code " + status + " : " + HttpUtils.statusReason(status));
        url_ = url;
    }

    public RestApiException(String url, Throwable cause) {
        super("An error occurred while using rest API at '" + url + "'", cause);
        url_ = url;
    }

    public RestApiException(String url) {
        super("An error occurred while using rest API at '" + url + "'");
        url_ = url;
    }

    public String getUrl() {
        return url_;
    }
}