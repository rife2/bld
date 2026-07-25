/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import rife.bld.dependencies.exceptions.ArtifactNotFoundException;
import rife.bld.dependencies.exceptions.ArtifactRetrievalErrorException;
import rife.bld.dependencies.exceptions.DependencyTransferException;
import rife.ioc.HierarchicalProperties;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.Scope.*;

/**
 * A repository that fails with a transient issue is skipped in favor of
 * the next one, and transient failures never degrade into not-found.
 */
public class TestRepositoryFallthrough {
    private DependencyResolver resolver(List<Repository> repositories, Dependency dependency) {
        return new DependencyResolver(new VersionResolution(new HierarchicalProperties()), ArtifactRetriever.instance(), repositories, dependency);
    }

    @Test
    void testMetadataFallsThroughTransientFailure() throws Exception {
        var failing = createFailingServer();
        var serving = createArtifactServer(Map.of("tool:1.0.0", pom("tool", "1.0.0", "")),
            Map.of("tool", metadata("tool", "1.0.0")));
        failing.start();
        serving.start();
        try {
            var resolver = resolver(List.of(serverRepository(failing), serverRepository(serving)),
                new Dependency("com.example", "tool"));
            assertEquals(Version.parse("1.0.0"), resolver.latestVersion());
        } finally {
            failing.stop(0);
            serving.stop(0);
        }
    }

    @Test
    void testMetadataAllTransientFailures() throws Exception {
        var failing1 = createFailingServer();
        var failing2 = createFailingServer();
        failing1.start();
        failing2.start();
        try {
            var resolver = resolver(List.of(serverRepository(failing1), serverRepository(failing2)),
                new Dependency("com.example", "tool"));
            var exception = assertThrows(ArtifactRetrievalErrorException.class, resolver::latestVersion);
            // the failure of the second repository is preserved as suppressed
            assertEquals(1, exception.getSuppressed().length);
        } finally {
            failing1.stop(0);
            failing2.stop(0);
        }
    }

    @Test
    void testMetadataNotFoundStaysNotFound() throws Exception {
        var empty1 = createArtifactServer(Map.of(), Map.of());
        var empty2 = createArtifactServer(Map.of(), Map.of());
        empty1.start();
        empty2.start();
        try {
            var resolver = resolver(List.of(serverRepository(empty1), serverRepository(empty2)),
                new Dependency("com.example", "tool"));
            assertThrows(ArtifactNotFoundException.class, resolver::latestVersion);
        } finally {
            empty1.stop(0);
            empty2.stop(0);
        }
    }

    @Test
    void testPomFallsThroughTransientFailure() throws Exception {
        var failing = createFailingServer();
        var serving = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", "")), Map.of());
        failing.start();
        serving.start();
        try {
            var resolver = resolver(List.of(serverRepository(failing), serverRepository(serving)),
                new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            var dependencies = resolver.getAllDependencies(compile);
            assertEquals(2, dependencies.size());
        } finally {
            failing.stop(0);
            serving.stop(0);
        }
    }

    @Test
    void testPomAllTransientFailures() throws Exception {
        var failing = createFailingServer();
        failing.start();
        try {
            var resolver = resolver(List.of(serverRepository(failing)),
                new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            assertThrows(ArtifactRetrievalErrorException.class, () -> resolver.getAllDependencies(compile));
        } finally {
            failing.stop(0);
        }
    }

    @Test
    void testTransferFallsThroughTransientFailure() throws Exception {
        var failing = createFailingServer();
        var serving = createArtifactServer(Map.of("tool:1.0.0", pom("tool", "1.0.0", "")), Map.of());
        failing.start();
        serving.start();
        var tmp = Files.createTempDirectory("fallthrough").toFile();
        try {
            var resolver = resolver(List.of(serverRepository(failing), serverRepository(serving)),
                new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            var artifact = resolver.transferIntoDirectory(tmp);
            assertNotNull(artifact);
            assertTrue(new File(tmp, "tool-1.0.0.jar").exists());
        } finally {
            failing.stop(0);
            serving.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testTransferAllTransientFailures() throws Exception {
        var failing = createFailingServer();
        failing.start();
        var tmp = Files.createTempDirectory("fallthrough").toFile();
        try {
            var resolver = resolver(List.of(serverRepository(failing)),
                new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            // a transient failure surfaces as an exception, not as the
            // null result that signals not-found
            assertThrows(DependencyTransferException.class, () -> resolver.transferIntoDirectory(tmp));
        } finally {
            failing.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testTransferNotFoundReturnsNull() throws Exception {
        var empty = createArtifactServer(Map.of(), Map.of());
        empty.start();
        var tmp = Files.createTempDirectory("fallthrough").toFile();
        try {
            var resolver = resolver(List.of(serverRepository(empty)),
                new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            assertNull(resolver.transferIntoDirectory(tmp));
        } finally {
            empty.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    private static Repository serverRepository(HttpServer server) {
        return new Repository("http://localhost:" + server.getAddress().getPort() + "/");
    }

    private static HttpServer createFailingServer()
    throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    private static HttpServer createArtifactServer(Map<String, String> poms, Map<String, String> metadata)
    throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            byte[] body = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                var content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
                if (content != null) {
                    body = content.getBytes();
                }
            } else if (filename.endsWith(".jar") && segments.length >= 3) {
                if (poms.containsKey(segments[segments.length - 3] + ":" + segments[segments.length - 2])) {
                    body = "jar".getBytes();
                }
            } else if (filename.equals("maven-metadata.xml") && segments.length >= 2) {
                var content = metadata.get(segments[segments.length - 2]);
                if (content != null) {
                    body = content.getBytes();
                }
            }
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    private static String dependency(String artifact, String version) {
        return "<dependency><groupId>com.example</groupId><artifactId>" + artifact + "</artifactId><version>" + version + "</version></dependency>";
    }

    private static String pom(String artifact, String version, String dependencies) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <dependencies>%s</dependencies>
            </project>""".formatted(artifact, version, dependencies);
    }

    private static String metadata(String artifact, String version) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <versioning>
                    <latest>%s</latest>
                    <release>%s</release>
                    <versions><version>%s</version></versions>
                </versioning>
            </metadata>""".formatted(artifact, version, version, version);
    }
}
