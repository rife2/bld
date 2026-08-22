/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import rife.tools.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Transient server failures during artifact retrieval are retried,
 * everything that will fail the same way again is not.
 */
public class TestArtifactRetrieverRetry {
    private static final int ATTEMPTS = 3;
    private static final long TEST_DELAY_MS = 1L;

    private HttpServer server(AtomicInteger requests, java.util.function.IntUnaryOperator statusForRequest)
    throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var status = statusForRequest.applyAsInt(requests.incrementAndGet());
            if (status == 200) {
                var body = "artifact content".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.sendResponseHeaders(status, -1);
            }
            exchange.close();
        });
        return server;
    }

    private RepositoryArtifact artifact(HttpServer server) {
        var location = "http://localhost:" + server.getAddress().getPort() + "/artifact.pom";
        return new RepositoryArtifact(new Repository(location), location);
    }

    private String retrieve(HttpServer server)
    throws IOException {
        var connection = ArtifactRetriever.connectArtifact(artifact(server), ATTEMPTS, TEST_DELAY_MS, TEST_DELAY_MS);
        try (var input_stream = connection.getInputStream()) {
            return FileUtils.readString(input_stream);
        } catch (rife.tools.exceptions.FileUtilsErrorException e) {
            throw new IOException(e);
        }
    }

    @Test
    void testRateLimitedThenSuccess()
    throws Exception {
        var requests = new AtomicInteger();
        var server = server(requests, request -> request < 3 ? 429 : 200);
        server.start();
        try {
            assertEquals("artifact content", retrieve(server));
            assertEquals(3, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testServerErrorThenSuccess()
    throws Exception {
        var requests = new AtomicInteger();
        var server = server(requests, request -> request < 2 ? 503 : 200);
        server.start();
        try {
            assertEquals("artifact content", retrieve(server));
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testExhaustedAttempts()
    throws Exception {
        var requests = new AtomicInteger();
        var server = server(requests, request -> 429);
        server.start();
        try {
            assertThrows(IOException.class, () -> retrieve(server));
            assertEquals(ATTEMPTS, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testNotFoundIsNotRetried()
    throws Exception {
        var requests = new AtomicInteger();
        var server = server(requests, request -> 404);
        server.start();
        try {
            assertThrows(FileNotFoundException.class, () -> retrieve(server));
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testClientErrorIsNotRetried()
    throws Exception {
        var requests = new AtomicInteger();
        var server = server(requests, request -> 403);
        server.start();
        try {
            assertThrows(IOException.class, () -> retrieve(server));
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }
}
