/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import org.junit.Test;
import rife.bld.operations.exceptions.ExitStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestJlinkOperation {
    @Test
    public void TestNoArguments() {
        var jlink = new JlinkOperation();
        assertThrows(ExitStatusException.class, jlink::execute);
    }

    @Test
    public void TestVersion() {
        var jlink = new JlinkOperation().toolArg("--version");
        assertDoesNotThrow(jlink::execute);
    }
}
