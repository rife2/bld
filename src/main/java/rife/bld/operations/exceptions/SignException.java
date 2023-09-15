/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations.exceptions;

import java.io.File;
import java.io.Serial;

/**
 * When thrown, indicates that something went wrong during signing
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.19
 */
public class SignException extends RuntimeException {
    @Serial private static final long serialVersionUID = -7352738410475855871L;

    private final File file_;
    private final String reason_;

    public SignException(File file) {
        this(file, null);
    }

    public SignException(File file, Throwable reason) {
        super("An error occurred while signing '" + file, reason);
        file_ = file;
        if (reason.getCause() != null && reason.getCause().getMessage() != null)
            reason_ = reason.getCause().getMessage();
        else reason_ = "";
    }

    public File getFile() {
        return file_;
    }

    public String getReason() {
        return reason_;
    }
}