/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations.exceptions;

import java.io.Serial;

/**
 * When thrown, indicates that wrong options were provided to the operation.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class OperationOptionException extends RuntimeException {
    @Serial private static final long serialVersionUID = 5577728010329494164L;

    public OperationOptionException(String message) {
        super(message);
    }
}