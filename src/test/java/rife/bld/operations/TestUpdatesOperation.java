/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import rife.bld.WebProject;
import rife.bld.dependencies.*;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestUpdatesOperation {
    @Test
    void testInstantiation() {
        var operation = new UpdatesOperation();
        assertTrue(operation.dependencies().isEmpty());
        assertTrue(operation.repositories().isEmpty());
    }

    @Test
    void testPopulation() {
        var repository1 = new Repository("repository1");
        var repository2 = new Repository("repository2");
        var dependency1 = new Dependency("group1", "artifact1");
        var dependency2 = new Dependency("group2", "artifact2");

        var operation1 = new UpdatesOperation()
            .repositories(List.of(repository1, repository2));
        var dependency_scopes = new DependencyScopes();
        dependency_scopes.scope(Scope.compile).include(dependency1).include(dependency2);
        operation1.dependencies(dependency_scopes);
        assertTrue(operation1.repositories().contains(repository1));
        assertTrue(operation1.repositories().contains(repository2));
        assertTrue(operation1.dependencies().scope(Scope.compile).contains(dependency1));
        assertTrue(operation1.dependencies().scope(Scope.compile).contains(dependency2));

        var operation2 = new UpdatesOperation();
        operation2.repositories().add(repository1);
        operation2.repositories().add(repository2);
        operation2.dependencies().scope(Scope.compile).include(dependency1).include(dependency2);
        operation2.dependencies(dependency_scopes);
        assertTrue(operation2.repositories().contains(repository1));
        assertTrue(operation2.repositories().contains(repository2));
        assertTrue(operation2.dependencies().scope(Scope.compile).contains(dependency1));
        assertTrue(operation2.dependencies().scope(Scope.compile).contains(dependency2));

        var operation3 = new UpdatesOperation()
            .repositories(repository1, repository2);
        assertTrue(operation3.repositories().contains(repository1));
        assertTrue(operation3.repositories().contains(repository2));
    }

    @Test
    void testExecution()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var operation = new UpdatesOperation()
                .repositories(RepositoryTestHelper.getNextRepositories(1));
            operation.dependencies().scope(Scope.compile)
                .include(new Dependency("org.apache.commons", "commons-lang3", new VersionNumber(3, 10, 0)));
            operation.dependencies().scope(Scope.runtime)
                .include(new Dependency("org.apache.commons", "commons-collections4", new VersionNumber(4, 0)));
            operation.dependencies().scope(Scope.standalone)
                .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(1, 0, 6)));
            operation.dependencies().scope(Scope.test)
                .include(new Dependency("org.apache.httpcomponents.client5", "httpclient5", new VersionNumber(5, 0, 1)));

            operation.execute();

            var results = operation.updates();
            assertFalse(results.isEmpty());

            assertEquals(1, results.get(Scope.compile).size());
            assertEquals(1, results.get(Scope.runtime).size());
            assertEquals(1, results.get(Scope.standalone).size());
            assertEquals(1, results.get(Scope.test).size());
            results.get(Scope.compile).forEach(dependency -> {
                assertEquals("org.apache.commons", dependency.groupId());
                assertEquals("commons-lang3", dependency.artifactId());
                assertNotEquals(new VersionNumber(3, 10, 0), dependency.version());
            });
            results.get(Scope.runtime).forEach(dependency -> {
                assertEquals("org.apache.commons", dependency.groupId());
                assertEquals("commons-collections4", dependency.artifactId());
                assertNotEquals(new VersionNumber(4, 0), dependency.version());
            });
            results.get(Scope.standalone).forEach(dependency -> {
                assertEquals("org.slf4j", dependency.groupId());
                assertEquals("slf4j-simple", dependency.artifactId());
                assertNotEquals(new VersionNumber(1, 0, 6), dependency.version());
            });
            results.get(Scope.test).forEach(dependency -> {
                assertEquals("org.apache.httpcomponents.client5", dependency.groupId());
                assertEquals("httpclient5", dependency.artifactId());
                assertNotEquals(new VersionNumber(5, 0, 1), dependency.version());
            });

        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class TestProject extends WebProject {
        public TestProject(File tmp) {
            workDirectory = tmp;
            pkg = "test.pkg";
        }
    }

    @Test
    void testFromProject()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var project = new TestProject(tmp);
            project.createProjectStructure();
            project.repositories().add(RepositoryTestHelper.getNextRepository());
            project.dependencies().scope(Scope.compile)
                .include(new Dependency("org.apache.commons", "commons-lang3", new VersionNumber(3, 10, 0)));
            project.dependencies().scope(Scope.runtime)
                .include(new Dependency("org.apache.commons", "commons-collections4", new VersionNumber(4, 0)));
            project.dependencies().scope(Scope.standalone)
                .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(1, 0, 6)));
            project.dependencies().scope(Scope.test)
                .include(new Dependency("org.apache.httpcomponents.client5", "httpclient5", new VersionNumber(5, 0, 1)));

            var operation = new UpdatesOperation()
                .fromProject(project);

            operation.execute();

            var results = operation.updates();
            assertFalse(results.isEmpty());

            assertEquals(1, results.get(Scope.compile).size());
            assertEquals(1, results.get(Scope.runtime).size());
            assertEquals(1, results.get(Scope.standalone).size());
            assertEquals(1, results.get(Scope.test).size());
            results.get(Scope.compile).forEach(dependency -> {
                assertEquals("org.apache.commons", dependency.groupId());
                assertEquals("commons-lang3", dependency.artifactId());
                assertNotEquals(new VersionNumber(3, 10, 0), dependency.version());
            });
            results.get(Scope.runtime).forEach(dependency -> {
                assertEquals("org.apache.commons", dependency.groupId());
                assertEquals("commons-collections4", dependency.artifactId());
                assertNotEquals(new VersionNumber(4, 0), dependency.version());
            });
            results.get(Scope.standalone).forEach(dependency -> {
                assertEquals("org.slf4j", dependency.groupId());
                assertEquals("slf4j-simple", dependency.artifactId());
                assertNotEquals(new VersionNumber(1, 0, 6), dependency.version());
            });
            results.get(Scope.test).forEach(dependency -> {
                assertEquals("org.apache.httpcomponents.client5", dependency.groupId());
                assertEquals("httpclient5", dependency.artifactId());
                assertNotEquals(new VersionNumber(5, 0, 1), dependency.version());
            });

        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testBomUpdateReported()
    throws Exception {
        var server = createArtifactServer(
            Map.of("bom1:1.0.0", bomPom("bom1", "1.0.0", "<dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>1.4.0</version></dependency>")),
            Map.of("bom1", metadata("bom1", "2.0.0", "1.0.0", "2.0.0"),
                   "a", metadata("a", "9.9.9", "1.4.0", "9.9.9")));
        server.start();
        try {
            var operation = new UpdatesOperation()
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .repositories(List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/")));
            operation.dependencies().scope(Scope.compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            operation.execute();

            // the newer BOM version is reported
            var boms = operation.updates().scope(Scope.compile).boms();
            assertEquals(1, boms.size());
            assertEquals(new Bom("com.example", "bom1", new VersionNumber(2, 0, 0)), boms.iterator().next());

            // the version-less dependency covered by the BOM is not reported,
            // even though newer versions exist in the metadata
            assertTrue(operation.updates().scope(Scope.compile).isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testExplicitVersionCheckedDespiteBom()
    throws Exception {
        var server = createArtifactServer(
            Map.of("bom1:1.0.0", bomPom("bom1", "1.0.0", "<dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>1.4.0</version></dependency>")),
            Map.of("bom1", metadata("bom1", "1.0.0", "1.0.0"),
                   "a", metadata("a", "2.0.0", "1.0.0", "2.0.0")));
        server.start();
        try {
            var operation = new UpdatesOperation()
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .repositories(List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/")));
            operation.dependencies().scope(Scope.compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            operation.execute();

            // the BOM is up to date and not reported
            assertTrue(operation.updates().scope(Scope.compile).boms().isEmpty());

            // the explicitly versioned dependency is still checked for
            // updates, even though the BOM covers it
            var dependencies = operation.updates().scope(Scope.compile);
            assertEquals(1, dependencies.size());
            assertEquals(new VersionNumber(2, 0, 0), dependencies.iterator().next().version());
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
                var body = content.getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        return server;
    }

    private static String bomPom(String artifact, String version, String managedDependencies) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                    <dependencies>%s</dependencies>
                </dependencyManagement>
            </project>""".formatted(artifact, version, managedDependencies);
    }

    private static String metadata(String artifact, String latest, String... versions) {
        var versions_xml = new StringBuilder();
        for (var version : versions) {
            versions_xml.append("<version>").append(version).append("</version>");
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <versioning>
                    <latest>%s</latest>
                    <release>%s</release>
                    <versions>%s</versions>
                </versioning>
            </metadata>""".formatted(artifact, latest, latest, versions_xml);
    }
}
