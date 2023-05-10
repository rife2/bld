/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.VersionNumber;
import rife.tools.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.*;

public class TestProject {
    @Test
    void testInstantiation() {
        var project = new Project();

        assertNotNull(project.workDirectory);
        assertNotNull(project.workDirectory());
        assertTrue(project.workDirectory().exists());
        assertTrue(project.workDirectory().isDirectory());
        assertNull(project.pkg);
        assertThrows(IllegalStateException.class, project::pkg);
        assertNull(project.name);
        assertThrows(IllegalStateException.class, project::name);
        assertNull(project.version);
        assertThrows(IllegalStateException.class, project::version);
        assertNull(project.mainClass);
        assertThrows(IllegalStateException.class, project::mainClass);

        assertNotNull(project.repositories);
        assertTrue(project.repositories.isEmpty());
        assertNotNull(project.dependencies);
        assertTrue(project.dependencies.isEmpty());

        assertNull(project.javaRelease);
        assertNull(project.javaTool);
        assertNotNull(project.javaTool());
        assertEquals("java", project.javaTool());
        assertNull(project.archiveBaseName);
        assertThrows(IllegalStateException.class, project::archiveBaseName);
        assertNull(project.jarFileName);
        assertThrows(IllegalStateException.class, project::jarFileName);
        assertNull(project.sourcesJarFileName);
        assertThrows(IllegalStateException.class, project::sourcesJarFileName);
        assertNull(project.javadocJarFileName);
        assertThrows(IllegalStateException.class, project::javadocJarFileName);
        assertNull(project.uberJarFileName);
        assertThrows(IllegalStateException.class, project::uberJarFileName);
        assertNull(project.uberJarMainClass);
        assertThrows(IllegalStateException.class, project::uberJarMainClass);

        assertNull(project.srcDirectory);
        assertNotNull(project.srcDirectory());
        assertNull(project.srcBldDirectory);
        assertNotNull(project.srcBldDirectory());
        assertNull(project.srcBldJavaDirectory);
        assertNotNull(project.srcBldJavaDirectory());
        assertNull(project.srcMainDirectory);
        assertNotNull(project.srcMainDirectory());
        assertNull(project.srcMainJavaDirectory);
        assertNotNull(project.srcMainJavaDirectory());
        assertNull(project.srcMainResourcesDirectory);
        assertNotNull(project.srcMainResourcesDirectory());
        assertNull(project.srcMainResourcesTemplatesDirectory);
        assertNotNull(project.srcMainResourcesTemplatesDirectory());
        assertNull(project.srcTestDirectory);
        assertNotNull(project.srcTestDirectory());
        assertNull(project.srcTestJavaDirectory);
        assertNotNull(project.srcTestJavaDirectory());
        assertNotNull(project.libBldDirectory());
        assertNull(project.libDirectory);
        assertNotNull(project.libDirectory());
        assertNull(project.libCompileDirectory);
        assertNotNull(project.libCompileDirectory());
        assertNull(project.libRuntimeDirectory);
        assertNotNull(project.libRuntimeDirectory());
        assertNull(project.libStandaloneDirectory);
        assertNull(project.libStandaloneDirectory());
        assertNull(project.libTestDirectory);
        assertNotNull(project.libTestDirectory());
        assertNull(project.buildDirectory);
        assertNotNull(project.buildDirectory());
        assertNull(project.buildBldDirectory);
        assertNotNull(project.buildBldDirectory());
        assertNull(project.buildDistDirectory);
        assertNotNull(project.buildDistDirectory());
        assertNull(project.buildJavadocDirectory);
        assertNotNull(project.buildJavadocDirectory());
        assertNull(project.buildMainDirectory);
        assertNotNull(project.buildMainDirectory());
        assertNull(project.buildTemplatesDirectory);
        assertNotNull(project.buildTemplatesDirectory());
        assertNull(project.buildTestDirectory);
        assertNotNull(project.buildTestDirectory());

        assertNotNull(project.mainSourceFiles());
        assertNotNull(project.testSourceFiles());
        assertNotNull(project.compileClasspathJars());
        assertNotNull(project.runtimeClasspathJars());
        assertNotNull(project.standaloneClasspathJars());
        assertNotNull(project.testClasspathJars());

        assertNotNull(project.compileMainClasspath());
        assertNotNull(project.compileTestClasspath());
        assertNotNull(project.runClasspath());
        assertNotNull(project.testClasspath());
    }

    static class CustomProject extends Project {
        StringBuilder result_;

        CustomProject(File tmp, StringBuilder result) {
            result_ = result;
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "my_project";
            version = new VersionNumber(0, 0, 1);
        }

        @BuildCommand
        public void newcommand() {
            assertEquals("newcommand", getCurrentCommandName());
            assertNotNull(getCurrentCommandDefinition());
            result_.append("newcommand");
        }
    }

    @Test
    void testCustomCommand()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var result = new StringBuilder();
            var project = new CustomProject(tmp, result);

            assertNull(project.getCurrentCommandName());
            assertNull(project.getCurrentCommandDefinition());

            project.execute(new String[]{"newcommand"});

            assertNull(project.getCurrentCommandName());
            assertNull(project.getCurrentCommandDefinition());

            assertEquals("newcommand", result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class CustomProjectInlineHelp extends Project {
        CustomProjectInlineHelp(File tmp) {
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "my_project";
            version = new VersionNumber(0, 0, 1);
        }

        @BuildCommand(summary = "mysummary", description = "mydescription")
        public void newcommand() {
        }
    }

    @Test
    void testCustomCommandInlineHelp()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        var orig_err = System.err;
        try {
            var out = new ByteArrayOutputStream();
            System.setErr(new PrintStream(out, true, StandardCharsets.UTF_8));

            var project = new CustomProjectInlineHelp(tmp);
            project.execute(new String[]{""});

            assertTrue(out.toString(StandardCharsets.UTF_8).contains("newcommand       mysummary"));
            out.reset();

            project.execute(new String[]{"help", "newcommand"});
            assertTrue(out.toString(StandardCharsets.UTF_8).contains("mydescription"));

        } finally {
            System.setErr(orig_err);
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class CustomProjectLambda extends Project {
        CustomProjectLambda(File tmp, StringBuilder result) {
            buildCommands().put("newcommand2", () -> {
                assertEquals("newcommand2", getCurrentCommandName());
                assertNotNull(getCurrentCommandDefinition());
                result.append("newcommand2");
            });
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "my_project";
            version = new VersionNumber(0, 0, 1);
        }
    }

    @Test
    void testCustomCommandLambda()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var result = new StringBuilder();
            var project = new CustomProjectLambda(tmp, result);

            assertNull(project.getCurrentCommandName());
            assertNull(project.getCurrentCommandDefinition());

            project.execute(new String[]{"newcommand"});

            assertNull(project.getCurrentCommandName());
            assertNull(project.getCurrentCommandDefinition());

            assertEquals("newcommand2", result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testCommandMatch()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var result = new StringBuilder();
            var project = new CustomProjectLambda(tmp, result);
            project.execute(new String[]{"ne2", "nc2", "n2"});
            assertEquals("newcommand2" +
                "newcommand2" +
                "newcommand2", result.toString());

            result = new StringBuilder();
            project.execute(new String[]{"c"});
            assertEquals("", result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class CustomProjectAutoPurge extends Project {
        CustomProjectAutoPurge(File tmp) {
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "my_project";
            version = new VersionNumber(0, 0, 1);

            repositories = List.of(MAVEN_CENTRAL);
            scope(compile)
                .include(dependency("com.uwyn.rife2", "rife2", version(1, 5, 11)));
            scope(test)
                .include(dependency("org.jsoup", "jsoup", version(1, 15, 4)))
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 9, 2)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 9, 2)));
            scope(standalone)
                .include(dependency("org.eclipse.jetty", "jetty-server", version(11, 0, 14)))
                .include(dependency("org.eclipse.jetty", "jetty-servlet", version(11, 0, 14)))
                .include(dependency("org.slf4j", "slf4j-simple", version(2, 0, 7)));
        }

        public void enableAutoDownloadPurge() {
            autoDownloadPurge = true;
        }

        public void increaseRife2Version() {
            scope(compile).clear();
            scope(compile)
                .include(dependency("com.uwyn.rife2", "rife2", version(1, 5, 12)));
        }

        public void increaseRife2VersionMore() {
            scope(compile).clear();
            scope(compile)
                .include(dependency("com.uwyn.rife2", "rife2", version(1, 5, 15)));
        }
    }

    @Test
    void testAutoDownloadPurge()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var project = new CustomProjectAutoPurge(tmp);
            project.execute(new String[]{"version"});

            assertEquals("", FileUtils.generateDirectoryListing(tmp));

            project = new CustomProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.execute(new String[]{"version"});

            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld-build.hash
                /lib/compile
                /lib/compile/rife2-1.5.11.jar
                /lib/runtime
                /lib/test
                /lib/test/apiguardian-api-1.1.2.jar
                /lib/test/jsoup-1.15.4.jar
                /lib/test/junit-jupiter-5.9.2.jar
                /lib/test/junit-jupiter-api-5.9.2.jar
                /lib/test/junit-jupiter-engine-5.9.2.jar
                /lib/test/junit-jupiter-params-5.9.2.jar
                /lib/test/junit-platform-commons-1.9.2.jar
                /lib/test/junit-platform-console-standalone-1.9.2.jar
                /lib/test/junit-platform-engine-1.9.2.jar
                /lib/test/opentest4j-1.2.0.jar""", FileUtils.generateDirectoryListing(tmp));

            FileUtils.deleteDirectory(new File(tmp, "lib/compile"));
            FileUtils.deleteDirectory(new File(tmp, "lib/runtime"));
            FileUtils.deleteDirectory(new File(tmp, "lib/test"));
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld-build.hash""", FileUtils.generateDirectoryListing(tmp));

            project = new CustomProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.execute(new String[]{"version"});
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld-build.hash""", FileUtils.generateDirectoryListing(tmp));

            project = new CustomProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.increaseRife2Version();
            project.execute(new String[]{"version"});
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld-build.hash
                /lib/compile
                /lib/compile/rife2-1.5.12.jar
                /lib/runtime
                /lib/test
                /lib/test/apiguardian-api-1.1.2.jar
                /lib/test/jsoup-1.15.4.jar
                /lib/test/junit-jupiter-5.9.2.jar
                /lib/test/junit-jupiter-api-5.9.2.jar
                /lib/test/junit-jupiter-engine-5.9.2.jar
                /lib/test/junit-jupiter-params-5.9.2.jar
                /lib/test/junit-platform-commons-1.9.2.jar
                /lib/test/junit-platform-console-standalone-1.9.2.jar
                /lib/test/junit-platform-engine-1.9.2.jar
                /lib/test/opentest4j-1.2.0.jar""", FileUtils.generateDirectoryListing(tmp));

            project = new CustomProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.increaseRife2VersionMore();
            project.execute(new String[]{"version"});
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld-build.hash
                /lib/compile
                /lib/compile/rife2-1.5.15.jar
                /lib/runtime
                /lib/test
                /lib/test/apiguardian-api-1.1.2.jar
                /lib/test/jsoup-1.15.4.jar
                /lib/test/junit-jupiter-5.9.2.jar
                /lib/test/junit-jupiter-api-5.9.2.jar
                /lib/test/junit-jupiter-engine-5.9.2.jar
                /lib/test/junit-jupiter-params-5.9.2.jar
                /lib/test/junit-platform-commons-1.9.2.jar
                /lib/test/junit-platform-console-standalone-1.9.2.jar
                /lib/test/junit-platform-engine-1.9.2.jar
                /lib/test/opentest4j-1.2.0.jar""", FileUtils.generateDirectoryListing(tmp));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
