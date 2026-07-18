/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provides a local artifact server and dependency fixtures for testing
 * transfers without relying on remote repositories.
 */
abstract class TransferTestHelper {
    static HttpServer createTransferServer(AtomicInteger maxConcurrentTransfers)
    throws IOException {
        var active_transfers = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var active = active_transfers.incrementAndGet();
            maxConcurrentTransfers.accumulateAndGet(active, Math::max);
            try {
                // delay the response so that parallel transfers overlap
                Thread.sleep(200);

                var body = exchange.getRequestURI().getPath().getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                active_transfers.decrementAndGet();
            }
        });
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    static Repository transferRepository(HttpServer server) {
        return new Repository("http://localhost:" + server.getAddress().getPort() + "/");
    }

    static DependencySet createTransferDependencies(int from, int to) {
        var dependencies = new DependencySet();
        for (var i = from; i <= to; i++) {
            dependencies.include(new Dependency("com.example", "artifact" + i, new VersionNumber(1, 0, 0)));
        }
        return dependencies;
    }

    static void assertTransferredArtifacts(DependencySet dependencies, List<RepositoryArtifact> artifacts, File directory) {
        assertEquals(dependencies.size(), artifacts.size());
        var index = 0;
        for (var dependency : dependencies) {
            var filename = dependency.artifactId() + "-" + dependency.version() + ".jar";
            assertTrue(artifacts.get(index).location().endsWith(filename), "expected artifact " + filename + " at index " + index);
            assertTrue(new File(directory, filename).exists(), "expected file " + filename + " to be transferred");
            ++index;
        }
    }
}
