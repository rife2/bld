/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations.exceptions;

import rife.tools.HttpUtils;

import java.io.Serial;

/**
 * When thrown, indicates that something went wrong during upload
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.7
 */
public class UploadException extends RuntimeException {
    @Serial private static final long serialVersionUID = 8815214756135684019L;

    private final String url_;

    public UploadException(String url, int status) {
        super("An error occurred while uploading to '" + url + "'\nHTTP status code " + status + " : " + HttpUtils.statusReason(status));
        url_ = url;
    }

    public UploadException(String url, Throwable cause) {
        super("An error occurred while uploading to '" + url + "'", cause);
        url_ = url;
    }

    public String getUrl() {
        return url_;
    }
}