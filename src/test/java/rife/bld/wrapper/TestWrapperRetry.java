/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.wrapper;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestWrapperRetry {
    @Test
    void testSucceedsWithoutRetry() throws Exception {
        var attempts = new AtomicInteger();
        var result = Wrapper.retryOnTransientIoIssues(() -> {
            attempts.incrementAndGet();
            return "ok";
        }, 3, 1);
        assertEquals("ok", result);
        assertEquals(1, attempts.get());
    }

    @Test
    void testRetriesTransientIoIssues() throws Exception {
        // the first two attempts fail with a transient issue, the third
        // succeeds
        var attempts = new AtomicInteger();
        var result = Wrapper.retryOnTransientIoIssues(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IOException("Connection timed out");
            }
            return "ok";
        }, 3, 1);
        assertEquals("ok", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void testThrowsAfterExhaustedAttempts() {
        var attempts = new AtomicInteger();
        var exception = assertThrows(IOException.class, () ->
            Wrapper.retryOnTransientIoIssues(() -> {
                attempts.incrementAndGet();
                throw new IOException("Connection timed out");
            }, 3, 1));
        assertEquals("Connection timed out", exception.getMessage());
        assertEquals(3, attempts.get());
    }

    @Test
    void testDoesntRetryMissingResources() {
        // a 404 is not transient, retrying it would only slow down the
        // fallback paths that rely on a quick failure
        var attempts = new AtomicInteger();
        assertThrows(FileNotFoundException.class, () ->
            Wrapper.retryOnTransientIoIssues(() -> {
                attempts.incrementAndGet();
                throw new FileNotFoundException("http://localhost/absent");
            }, 3, 1));
        assertEquals(1, attempts.get());
    }
}
