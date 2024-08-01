/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.operations.exceptions.ExitStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestJlinkOperation {
    @Test
    void testNoArguments() {
        var jlink = new JlinkOperation();
        assertThrows(ExitStatusException.class, jlink::execute);
    }

    @Test
    void testVersion() {
        var jlink = new JlinkOperation().toolArg("--version");
        assertDoesNotThrow(jlink::execute);
    }
}
