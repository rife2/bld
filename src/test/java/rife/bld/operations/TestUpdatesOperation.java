/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.DependencyScopes;
import rife.bld.WebProject;
import rife.bld.dependencies.*;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

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
                .repositories(List.of(Repository.MAVEN_CENTRAL));
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
            project.repositories().add(Repository.MAVEN_CENTRAL);
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
}
