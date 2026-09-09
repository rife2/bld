/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Helper for the tests that run a generated project and check what it serves.
 */
public final class RunOperationTestHelper {
    private static final long TIMEOUT_SECONDS = 60;
    private static final long INTERVAL_MILLIS = 100;

    private RunOperationTestHelper() {
    }

    /**
     * Retrieves a URL as soon as the project that is about to run starts
     * serving it, then stops that project.
     * <p>
     * Waiting a fixed amount of time instead races the JVM startup and the
     * bind of the server, which is lost as soon as the machine is loaded.
     *
     * @param url the URL to retrieve once it responds
     * @param operation the run operation to stop afterwards, it should be
     *                  executed after this method returns
     * @return the content that was served
     */
    public static CompletableFuture<String> serveThenStop(URL url, RunOperation operation) {
        var served = new CompletableFuture<String>();
        var poller = new Thread(() -> {
            var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
            FileUtilsErrorException last_failure = null;
            while (System.nanoTime() < deadline) {
                try {
                    var content = FileUtils.readString(url);
                    if (content != null && !content.isEmpty()) {
                        served.complete(content);
                        break;
                    }
                } catch (FileUtilsErrorException e) {
                    // the server isn't accepting connections yet
                    last_failure = e;
                }
                try {
                    Thread.sleep(INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (!served.isDone()) {
                served.completeExceptionally(new IllegalStateException(
                    url + " wasn't served within " + TIMEOUT_SECONDS + " seconds", last_failure));
            }
            // stopping the process is what makes the run operation return
            var process = operation.process();
            if (process != null) {
                process.destroy();
            }
        }, "serve-then-stop");
        poller.setDaemon(true);
        poller.start();
        return served;
    }
}
