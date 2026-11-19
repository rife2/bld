/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import rife.ioc.HierarchicalProperties;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.Scope.*;

/**
 * A pom or a maven-metadata.xml that starts with a UTF-8 byte order mark
 * is read like any other, as Maven does. Maven Central has such poms, for
 * instance dk.brics.automaton:automaton:1.11-8.
 */
public class TestPomByteOrderMark {
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    private DependencyResolver resolver(HttpServer server, Dependency dependency) {
        return new DependencyResolver(new VersionResolution(new HierarchicalProperties()), ArtifactRetriever.instance(),
            List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/")), dependency);
    }

    @Test
    void testPomWithByteOrderMark() throws Exception {
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", BYTE_ORDER_MARK + pom("liba", "1.1.0", dependency("libb", "1.2.0")),
            "libb:1.2.0", pom("libb", "1.2.0", "")), Map.of());
        server.start();
        try {
            var dependencies = resolver(server, new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)))
                .getAllDependencies(compile);
            assertEquals("""
                com.example:tool:1.0.0
                com.example:liba:1.1.0
                com.example:libb:1.2.0""", String.join("\n", dependencies.stream().map(Dependency::toString).toList()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testMetadataWithByteOrderMark() throws Exception {
        var server = createArtifactServer(Map.of("tool:1.0.0", pom("tool", "1.0.0", "")),
            Map.of("tool", BYTE_ORDER_MARK + metadata("tool", "1.0.0")));
        server.start();
        try {
            assertEquals(Version.parse("1.0.0"), resolver(server, new Dependency("com.example", "tool")).latestVersion());
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer createArtifactServer(Map<String, String> poms, Map<String, String> metadata)
    throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            String content = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
            } else if (filename.equals("maven-metadata.xml") && segments.length >= 2) {
                content = metadata.get(segments[segments.length - 2]);
            }
            if (content == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                var body = content.getBytes(StandardCharsets.UTF_8);
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
