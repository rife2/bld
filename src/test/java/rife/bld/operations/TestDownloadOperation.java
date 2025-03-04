/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.WebProject;
import rife.bld.dependencies.*;
import rife.bld.dependencies.Module;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDownloadOperation {
    @Test
    void testInstantiation() {
        var operation = new DownloadOperation();
        assertFalse(operation.offline());
        assertTrue(operation.dependencies().isEmpty());
        assertTrue(operation.repositories().isEmpty());
        assertNull(operation.libCompileDirectory());
        assertNull(operation.libCompileModulesDirectory());
        assertNull(operation.libProvidedDirectory());
        assertNull(operation.libProvidedModulesDirectory());
        assertNull(operation.libRuntimeDirectory());
        assertNull(operation.libRuntimeModulesDirectory());
        assertNull(operation.libStandaloneDirectory());
        assertNull(operation.libStandaloneModulesDirectory());
        assertNull(operation.libTestDirectory());
        assertNull(operation.libTestModulesDirectory());
    }

    @Test
    void testPopulation() {
        var repository1 = new Repository("repository1");
        var repository2 = new Repository("repository2");
        var dependency1 = new Dependency("group1", "artifact1");
        var dependency2 = new Dependency("group2", "artifact2");
        var dir1 = new File("dir1");
        var dir2 = new File("dir2");
        var dir3 = new File("dir3");
        var dir4 = new File("dir4");
        var dir5 = new File("dir5");
        var dir6 = new File("dir6");
        var dir7 = new File("dir7");
        var dir8 = new File("dir8");
        var dir9 = new File("dir9");
        var dir10 = new File("dir10");

        var operation1 = new DownloadOperation()
            .repositories(List.of(repository1, repository2))
            .libCompileDirectory(dir1)
            .libProvidedDirectory(dir2)
            .libRuntimeDirectory(dir3)
            .libStandaloneDirectory(dir4)
            .libTestDirectory(dir5)
            .libCompileModulesDirectory(dir6)
            .libProvidedModulesDirectory(dir7)
            .libRuntimeModulesDirectory(dir8)
            .libStandaloneModulesDirectory(dir9)
            .libTestModulesDirectory(dir10);
        var dependency_scopes = new DependencyScopes();
        dependency_scopes.scope(Scope.compile).include(dependency1).include(dependency2);
        operation1.dependencies(dependency_scopes);
        assertTrue(operation1.repositories().contains(repository1));
        assertTrue(operation1.repositories().contains(repository2));
        assertTrue(operation1.dependencies().scope(Scope.compile).contains(dependency1));
        assertTrue(operation1.dependencies().scope(Scope.compile).contains(dependency2));
        assertEquals(dir1, operation1.libCompileDirectory());
        assertEquals(dir2, operation1.libProvidedDirectory());
        assertEquals(dir3, operation1.libRuntimeDirectory());
        assertEquals(dir4, operation1.libStandaloneDirectory());
        assertEquals(dir5, operation1.libTestDirectory());
        assertEquals(dir6, operation1.libCompileModulesDirectory());
        assertEquals(dir7, operation1.libProvidedModulesDirectory());
        assertEquals(dir8, operation1.libRuntimeModulesDirectory());
        assertEquals(dir9, operation1.libStandaloneModulesDirectory());
        assertEquals(dir10, operation1.libTestModulesDirectory());

        var operation2 = new DownloadOperation()
            .libCompileDirectory(dir1)
            .libProvidedDirectory(dir2)
            .libRuntimeDirectory(dir3)
            .libStandaloneDirectory(dir4)
            .libTestDirectory(dir5)
            .libCompileModulesDirectory(dir6)
            .libProvidedModulesDirectory(dir7)
            .libRuntimeModulesDirectory(dir8)
            .libStandaloneModulesDirectory(dir9)
            .libTestModulesDirectory(dir10);
        operation2.repositories().add(repository1);
        operation2.repositories().add(repository2);
        operation2.dependencies().scope(Scope.compile).include(dependency1).include(dependency2);
        operation2.dependencies(dependency_scopes);
        assertTrue(operation2.repositories().contains(repository1));
        assertTrue(operation2.repositories().contains(repository2));
        assertTrue(operation2.dependencies().scope(Scope.compile).contains(dependency1));
        assertTrue(operation2.dependencies().scope(Scope.compile).contains(dependency2));
        assertEquals(dir1, operation2.libCompileDirectory());
        assertEquals(dir2, operation2.libProvidedDirectory());
        assertEquals(dir3, operation2.libRuntimeDirectory());
        assertEquals(dir4, operation2.libStandaloneDirectory());
        assertEquals(dir5, operation2.libTestDirectory());
        assertEquals(dir6, operation2.libCompileModulesDirectory());
        assertEquals(dir7, operation2.libProvidedModulesDirectory());
        assertEquals(dir8, operation2.libRuntimeModulesDirectory());
        assertEquals(dir9, operation2.libStandaloneModulesDirectory());
        assertEquals(dir10, operation2.libTestModulesDirectory());

        var operation3 = new DownloadOperation()
            .offline(true)
            .repositories(repository1, repository2);
        assertTrue(operation3.offline());
        assertTrue(operation3.repositories().contains(repository1));
        assertTrue(operation3.repositories().contains(repository2));
    }

    @Test
    void testExecution()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var dir1 = new File(tmp, "dir1");
            var dir2 = new File(tmp, "dir2");
            var dir8 = new File(tmp, "dir8");
            var dir3 = new File(dir8, "dir3");
            var dir4 = new File(tmp, "dir4");
            var dir5 = new File(tmp, "dir5");
            var dir6 = new File(tmp, "dir6");
            var dir7 = new File(tmp, "dir7");
            var dir9 = new File(dir4, "dir9");
            var dir10 = new File(dir5, "dir10");

            var operation = new DownloadOperation()
                .repositories(List.of(Repository.MAVEN_CENTRAL))
                .libCompileDirectory(dir1)
                .libProvidedDirectory(dir2)
                .libRuntimeDirectory(dir3)
                .libStandaloneDirectory(dir4)
                .libTestDirectory(dir5)
                .libCompileModulesDirectory(dir6)
                .libProvidedModulesDirectory(dir7)
                .libRuntimeModulesDirectory(dir8)
                .libStandaloneModulesDirectory(dir9)
                .libTestModulesDirectory(dir10);
            operation.dependencies().scope(Scope.compile)
                .include(new Dependency("org.apache.commons", "commons-lang3", new VersionNumber(3,12,0)))
                .include(new Module("org.json", "json", new VersionNumber(20240303)));
            operation.dependencies().scope(Scope.provided)
                .include(new Dependency("commons-codec", "commons-codec", new VersionNumber(1,17,0)))
                .include(new Module("com.google.zxing", "javase", new VersionNumber(3,5,3)));
            operation.dependencies().scope(Scope.runtime)
                .include(new Dependency("org.apache.commons", "commons-collections4", new VersionNumber(4,4)))
                .include(new Module("org.postgresql", "postgresql", new VersionNumber(42,7,3)));
            operation.dependencies().scope(Scope.standalone)
                .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(2,0,6)))
                .include(new Module("org.eclipse.jetty.ee10", "jetty-ee10-servlet", new VersionNumber(12,0,16)));
            operation.dependencies().scope(Scope.test)
                .include(new Dependency("org.apache.httpcomponents.client5", "httpclient5", new VersionNumber(5,2,1)))
                .include(new Module("org.jsoup", "jsoup", new VersionNumber(1,18,1)));

            operation.execute();

            assertEquals("""
                    /dir1
                    /dir1/commons-lang3-3.12.0.jar
                    /dir2
                    /dir2/commons-codec-1.17.0.jar
                    /dir4
                    /dir4/dir9
                    /dir4/dir9/jakarta.servlet-api-6.0.0.jar
                    /dir4/dir9/jetty-ee10-servlet-12.0.16.jar
                    /dir4/dir9/jetty-http-12.0.16.jar
                    /dir4/dir9/jetty-io-12.0.16.jar
                    /dir4/dir9/jetty-security-12.0.16.jar
                    /dir4/dir9/jetty-server-12.0.16.jar
                    /dir4/dir9/jetty-session-12.0.16.jar
                    /dir4/dir9/jetty-util-12.0.16.jar
                    /dir4/dir9/slf4j-api-2.0.16.jar
                    /dir4/slf4j-simple-2.0.6.jar
                    /dir5
                    /dir5/dir10
                    /dir5/dir10/jsoup-1.18.1.jar
                    /dir5/httpclient5-5.2.1.jar
                    /dir5/httpcore5-5.2.jar
                    /dir5/httpcore5-h2-5.2.jar
                    /dir5/slf4j-api-1.7.36.jar
                    /dir6
                    /dir6/json-20240303.jar
                    /dir7
                    /dir7/core-3.5.3.jar
                    /dir7/jai-imageio-core-1.4.0.jar
                    /dir7/javase-3.5.3.jar
                    /dir7/jcommander-1.82.jar
                    /dir8
                    /dir8/checker-qual-3.42.0.jar
                    /dir8/dir3
                    /dir8/dir3/commons-collections4-4.4.jar
                    /dir8/postgresql-42.7.3.jar""",
                FileUtils.generateDirectoryListing(tmp));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecutionAdditionalSourcesJavadoc()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var dir1 = new File(tmp, "dir1");
            var dir2 = new File(tmp, "dir2");
            var dir8 = new File(tmp, "dir8");
            var dir3 = new File(dir8, "dir3");
            var dir4 = new File(tmp, "dir4");
            var dir5 = new File(tmp, "dir5");
            var dir6 = new File(tmp, "dir6");
            var dir7 = new File(tmp, "dir7");
            var dir9 = new File(dir4, "dir9");
            var dir10 = new File(dir5, "dir10");

            var operation = new DownloadOperation()
                .repositories(List.of(Repository.MAVEN_CENTRAL))
                .libCompileDirectory(dir1)
                .libProvidedDirectory(dir2)
                .libRuntimeDirectory(dir3)
                .libStandaloneDirectory(dir4)
                .libTestDirectory(dir5)
                .libCompileModulesDirectory(dir6)
                .libProvidedModulesDirectory(dir7)
                .libRuntimeModulesDirectory(dir8)
                .libStandaloneModulesDirectory(dir9)
                .libTestModulesDirectory(dir10)
                .downloadJavadoc(true)
                .downloadSources(true);
            operation.dependencies().scope(Scope.compile)
                .include(new Dependency("org.apache.commons", "commons-lang3", new VersionNumber(3,12,0)))
                .include(new Module("org.json", "json", new VersionNumber(20240303)).excludeSources());
            operation.dependencies().scope(Scope.provided)
                .include(new Dependency("commons-codec", "commons-codec", new VersionNumber(1,17,0)))
                .include(new Module("com.google.zxing", "javase", new VersionNumber(3,5,3)).excludeJavadoc());
            operation.dependencies().scope(Scope.runtime)
                .include(new Dependency("org.apache.commons", "commons-collections4", new VersionNumber(4,4)))
                .include(new Module("org.postgresql", "postgresql", new VersionNumber(42,7,3)));
            operation.dependencies().scope(Scope.standalone)
                .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(2,0,6)))
                .include(new Module("org.eclipse.jetty.ee10", "jetty-ee10-servlet", new VersionNumber(12,0,16)));
            operation.dependencies().scope(Scope.test)
                .include(new Dependency("org.apache.httpcomponents.client5", "httpclient5", new VersionNumber(5,2,1)))
                .include(new Module("org.jsoup", "jsoup", new VersionNumber(1,18,1)).excludeSources().excludeJavadoc());

            operation.execute();

            assertEquals("""
                    /dir1
                    /dir1/commons-lang3-3.12.0-javadoc.jar
                    /dir1/commons-lang3-3.12.0-sources.jar
                    /dir1/commons-lang3-3.12.0.jar
                    /dir2
                    /dir2/commons-codec-1.17.0-javadoc.jar
                    /dir2/commons-codec-1.17.0-sources.jar
                    /dir2/commons-codec-1.17.0.jar
                    /dir4
                    /dir4/dir9
                    /dir4/dir9/jakarta.servlet-api-6.0.0-javadoc.jar
                    /dir4/dir9/jakarta.servlet-api-6.0.0-sources.jar
                    /dir4/dir9/jakarta.servlet-api-6.0.0.jar
                    /dir4/dir9/jetty-ee10-servlet-12.0.16-javadoc.jar
                    /dir4/dir9/jetty-ee10-servlet-12.0.16-sources.jar
                    /dir4/dir9/jetty-ee10-servlet-12.0.16.jar
                    /dir4/dir9/jetty-http-12.0.16-javadoc.jar
                    /dir4/dir9/jetty-http-12.0.16-sources.jar
                    /dir4/dir9/jetty-http-12.0.16.jar
                    /dir4/dir9/jetty-io-12.0.16-javadoc.jar
                    /dir4/dir9/jetty-io-12.0.16-sources.jar
                    /dir4/dir9/jetty-io-12.0.16.jar
                    /dir4/dir9/jetty-security-12.0.16-javadoc.jar
                    /dir4/dir9/jetty-security-12.0.16-sources.jar
                    /dir4/dir9/jetty-security-12.0.16.jar
                    /dir4/dir9/jetty-server-12.0.16-javadoc.jar
                    /dir4/dir9/jetty-server-12.0.16-sources.jar
                    /dir4/dir9/jetty-server-12.0.16.jar
                    /dir4/dir9/jetty-session-12.0.16-javadoc.jar
                    /dir4/dir9/jetty-session-12.0.16-sources.jar
                    /dir4/dir9/jetty-session-12.0.16.jar
                    /dir4/dir9/jetty-util-12.0.16-javadoc.jar
                    /dir4/dir9/jetty-util-12.0.16-sources.jar
                    /dir4/dir9/jetty-util-12.0.16.jar
                    /dir4/dir9/slf4j-api-2.0.16-javadoc.jar
                    /dir4/dir9/slf4j-api-2.0.16-sources.jar
                    /dir4/dir9/slf4j-api-2.0.16.jar
                    /dir4/slf4j-simple-2.0.6-javadoc.jar
                    /dir4/slf4j-simple-2.0.6-sources.jar
                    /dir4/slf4j-simple-2.0.6.jar
                    /dir5
                    /dir5/dir10
                    /dir5/dir10/jsoup-1.18.1.jar
                    /dir5/httpclient5-5.2.1-javadoc.jar
                    /dir5/httpclient5-5.2.1-sources.jar
                    /dir5/httpclient5-5.2.1.jar
                    /dir5/httpcore5-5.2-javadoc.jar
                    /dir5/httpcore5-5.2-sources.jar
                    /dir5/httpcore5-5.2.jar
                    /dir5/httpcore5-h2-5.2-javadoc.jar
                    /dir5/httpcore5-h2-5.2-sources.jar
                    /dir5/httpcore5-h2-5.2.jar
                    /dir5/slf4j-api-1.7.36-javadoc.jar
                    /dir5/slf4j-api-1.7.36-sources.jar
                    /dir5/slf4j-api-1.7.36.jar
                    /dir6
                    /dir6/json-20240303-javadoc.jar
                    /dir6/json-20240303.jar
                    /dir7
                    /dir7/core-3.5.3-javadoc.jar
                    /dir7/core-3.5.3-sources.jar
                    /dir7/core-3.5.3.jar
                    /dir7/jai-imageio-core-1.4.0-javadoc.jar
                    /dir7/jai-imageio-core-1.4.0-sources.jar
                    /dir7/jai-imageio-core-1.4.0.jar
                    /dir7/javase-3.5.3-sources.jar
                    /dir7/javase-3.5.3.jar
                    /dir7/jcommander-1.82-javadoc.jar
                    /dir7/jcommander-1.82-sources.jar
                    /dir7/jcommander-1.82.jar
                    /dir8
                    /dir8/checker-qual-3.42.0-javadoc.jar
                    /dir8/checker-qual-3.42.0-sources.jar
                    /dir8/checker-qual-3.42.0.jar
                    /dir8/dir3
                    /dir8/dir3/commons-collections4-4.4-javadoc.jar
                    /dir8/dir3/commons-collections4-4.4-sources.jar
                    /dir8/dir3/commons-collections4-4.4.jar
                    /dir8/postgresql-42.7.3-javadoc.jar
                    /dir8/postgresql-42.7.3-sources.jar
                    /dir8/postgresql-42.7.3.jar""",
                FileUtils.generateDirectoryListing(tmp));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class TestProject extends WebProject {
        public TestProject(File tmp) {
            workDirectory = tmp;
            pkg = "test.pkg";
            downloadSources = true;
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
                .include(new Dependency("org.apache.commons", "commons-lang3", new VersionNumber(3,12,0)))
                .include(new Module("org.json", "json", new VersionNumber(20240303)).excludeSources());
            project.dependencies().scope(Scope.provided)
                .include(new Dependency("commons-codec", "commons-codec", new VersionNumber(1,17,0)))
                .include(new Module("com.google.zxing", "javase", new VersionNumber(3,5,3)));
            project.dependencies().scope(Scope.runtime)
                .include(new Dependency("org.apache.commons", "commons-collections4", new VersionNumber(4,4)))
                .include(new Module("org.postgresql", "postgresql", new VersionNumber(42,7,3)));
            project.dependencies().scope(Scope.standalone)
                .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(2,0,6)))
                .include(new Module("org.eclipse.jetty.ee10", "jetty-ee10-servlet", new VersionNumber(12,0,16)));
            project.dependencies().scope(Scope.test)
                .include(new Dependency("org.apache.httpcomponents.client5", "httpclient5", new VersionNumber(5,2,1)))
                .include(new Module("org.jsoup", "jsoup", new VersionNumber(1,18,1)));

            var operation = new DownloadOperation()
                .fromProject(project);

            operation.execute();

            assertEquals("""
                    /lib
                    /lib/bld
                    /lib/compile
                    /lib/compile/commons-lang3-3.12.0-sources.jar
                    /lib/compile/commons-lang3-3.12.0.jar
                    /lib/compile/modules
                    /lib/compile/modules/json-20240303.jar
                    /lib/provided
                    /lib/provided/commons-codec-1.17.0-sources.jar
                    /lib/provided/commons-codec-1.17.0.jar
                    /lib/provided/modules
                    /lib/provided/modules/core-3.5.3-sources.jar
                    /lib/provided/modules/core-3.5.3.jar
                    /lib/provided/modules/jai-imageio-core-1.4.0-sources.jar
                    /lib/provided/modules/jai-imageio-core-1.4.0.jar
                    /lib/provided/modules/javase-3.5.3-sources.jar
                    /lib/provided/modules/javase-3.5.3.jar
                    /lib/provided/modules/jcommander-1.82-sources.jar
                    /lib/provided/modules/jcommander-1.82.jar
                    /lib/runtime
                    /lib/runtime/commons-collections4-4.4-sources.jar
                    /lib/runtime/commons-collections4-4.4.jar
                    /lib/runtime/modules
                    /lib/runtime/modules/checker-qual-3.42.0-sources.jar
                    /lib/runtime/modules/checker-qual-3.42.0.jar
                    /lib/runtime/modules/postgresql-42.7.3-sources.jar
                    /lib/runtime/modules/postgresql-42.7.3.jar
                    /lib/standalone
                    /lib/standalone/modules
                    /lib/standalone/modules/jakarta.servlet-api-6.0.0-sources.jar
                    /lib/standalone/modules/jakarta.servlet-api-6.0.0.jar
                    /lib/standalone/modules/jetty-ee10-servlet-12.0.16-sources.jar
                    /lib/standalone/modules/jetty-ee10-servlet-12.0.16.jar
                    /lib/standalone/modules/jetty-http-12.0.16-sources.jar
                    /lib/standalone/modules/jetty-http-12.0.16.jar
                    /lib/standalone/modules/jetty-io-12.0.16-sources.jar
                    /lib/standalone/modules/jetty-io-12.0.16.jar
                    /lib/standalone/modules/jetty-security-12.0.16-sources.jar
                    /lib/standalone/modules/jetty-security-12.0.16.jar
                    /lib/standalone/modules/jetty-server-12.0.16-sources.jar
                    /lib/standalone/modules/jetty-server-12.0.16.jar
                    /lib/standalone/modules/jetty-session-12.0.16-sources.jar
                    /lib/standalone/modules/jetty-session-12.0.16.jar
                    /lib/standalone/modules/jetty-util-12.0.16-sources.jar
                    /lib/standalone/modules/jetty-util-12.0.16.jar
                    /lib/standalone/modules/slf4j-api-2.0.16-sources.jar
                    /lib/standalone/modules/slf4j-api-2.0.16.jar
                    /lib/standalone/slf4j-simple-2.0.6-sources.jar
                    /lib/standalone/slf4j-simple-2.0.6.jar
                    /lib/test
                    /lib/test/httpclient5-5.2.1-sources.jar
                    /lib/test/httpclient5-5.2.1.jar
                    /lib/test/httpcore5-5.2-sources.jar
                    /lib/test/httpcore5-5.2.jar
                    /lib/test/httpcore5-h2-5.2-sources.jar
                    /lib/test/httpcore5-h2-5.2.jar
                    /lib/test/modules
                    /lib/test/modules/jsoup-1.18.1-sources.jar
                    /lib/test/modules/jsoup-1.18.1.jar
                    /lib/test/slf4j-api-1.7.36-sources.jar
                    /lib/test/slf4j-api-1.7.36.jar
                    /src
                    /src/bld
                    /src/bld/java
                    /src/bld/resources
                    /src/main
                    /src/main/java
                    /src/main/resources
                    /src/main/resources/templates
                    /src/test
                    /src/test/java
                    /src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testFromProject2()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var project = new TestProject(tmp);
            project.createProjectStructure();
            project.repositories().add(Repository.MAVEN_CENTRAL);
            project.dependencies().scope(Scope.compile)
                .include(new Dependency("com.stripe", "stripe-java", new VersionNumber(20,136,0)));
            project.dependencies().scope(Scope.runtime)
                .include(new Module("org.json", "json", new VersionNumber(20240303)));

            var operation = new DownloadOperation()
                .fromProject(project);

            operation.execute();

            assertEquals("""
                    /lib
                    /lib/bld
                    /lib/compile
                    /lib/compile/modules
                    /lib/compile/stripe-java-20.136.0-sources.jar
                    /lib/compile/stripe-java-20.136.0.jar
                    /lib/provided
                    /lib/provided/modules
                    /lib/runtime
                    /lib/runtime/gson-2.9.0-sources.jar
                    /lib/runtime/gson-2.9.0.jar
                    /lib/runtime/modules
                    /lib/runtime/modules/json-20240303-sources.jar
                    /lib/runtime/modules/json-20240303.jar
                    /lib/standalone
                    /lib/standalone/modules
                    /lib/test
                    /lib/test/modules
                    /src
                    /src/bld
                    /src/bld/java
                    /src/bld/resources
                    /src/main
                    /src/main/java
                    /src/main/resources
                    /src/main/resources/templates
                    /src/test
                    /src/test/java
                    /src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
