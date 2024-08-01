/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import org.junit.Test;
import rife.bld.operations.exceptions.ExitStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestJmodOperation {
    @Test
    public void TestNoArguments() {
        var jmod = new JmodOperation();
        assertThrows(ExitStatusException.class, jmod::execute);
    }

    @Test
    public void TestVersion() {
        var jmod = new JmodOperation().operationMode(JmodOperation.OperationMode.DESCRIBE).toolArg("--version");
        assertDoesNotThrow(jmod::execute);
    }
}
