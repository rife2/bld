/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.VersionNumber;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.*;
import static rife.bld.dependencies.Scope.compile;

public class TestWebProject {
    @Test
    void testInstantiation() {
        var project = new WebProject();

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
        assertNull(project.module);

        assertNotNull(project.repositories);
        assertTrue(project.repositories.isEmpty());
        assertNotNull(project.dependencies);
        assertTrue(project.dependencies.isEmpty());

        assertNull(project.javaTool);
        assertNotNull(project.javaTool());
        assertEquals("java", project.javaTool());
        assertNull(project.archiveBaseName);
        assertThrows(IllegalStateException.class, project::archiveBaseName);
        assertNull(project.jarFileName);
        assertThrows(IllegalStateException.class, project::jarFileName);
        assertNull(project.uberJarFileName);
        assertThrows(IllegalStateException.class, project::uberJarFileName);
        assertNull(project.uberJarMainClass);
        assertThrows(IllegalStateException.class, project::uberJarMainClass);
        assertNull(project.warFileName);
        assertThrows(IllegalStateException.class, project::warFileName);

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
        assertNull(project.srcMainWebappDirectory);
        assertNotNull(project.srcMainWebappDirectory());
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
        assertNotNull(project.libStandaloneDirectory());
        assertNull(project.libTestDirectory);
        assertNotNull(project.libTestDirectory());
        assertNull(project.buildDirectory);
        assertNotNull(project.buildDirectory());
        assertNull(project.buildBldDirectory);
        assertNotNull(project.buildBldDirectory());
        assertNull(project.buildDistDirectory);
        assertNotNull(project.buildDistDirectory());
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

    @Test
    void testCustomCommand()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var result = new StringBuilder();
            var project = new WebProject() {
                @BuildCommand
                public void newcommand() {
                    result.append("newcommand");
                }
            };
            project.workDirectory = tmp;
            project.pkg = "test.pkg";
            project.name = "my_project";
            project.version = new VersionNumber(0, 0, 1);

            project.execute(new String[]{"newcommand"});
            assertEquals("newcommand", result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testCustomCommandLambda()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var result = new StringBuilder();
            var project = new WebProject();
            project.buildCommands().put("newcommand", () -> result.append("newcommand"));
            project.workDirectory = tmp;
            project.pkg = "test.pkg";
            project.name = "my_project";
            project.version = new VersionNumber(0, 0, 1);

            project.execute(new String[]{"newcommand"});
            assertEquals("newcommand", result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class CustomWebProjectAutoPurge extends WebProject {
        CustomWebProjectAutoPurge(File tmp) {
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
            var project = new CustomWebProjectAutoPurge(tmp);
            project.execute(new String[]{"version"});

            assertEquals("", FileUtils.generateDirectoryListing(tmp));

            project = new CustomWebProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.execute(new String[]{"version"});

            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld.cache
                /lib/compile
                /lib/compile/rife2-1.5.11.jar
                /lib/standalone
                /lib/standalone/jetty-http-11.0.14.jar
                /lib/standalone/jetty-io-11.0.14.jar
                /lib/standalone/jetty-jakarta-servlet-api-5.0.2.jar
                /lib/standalone/jetty-security-11.0.14.jar
                /lib/standalone/jetty-server-11.0.14.jar
                /lib/standalone/jetty-servlet-11.0.14.jar
                /lib/standalone/jetty-util-11.0.14.jar
                /lib/standalone/slf4j-api-2.0.7.jar
                /lib/standalone/slf4j-simple-2.0.7.jar
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
            FileUtils.deleteDirectory(new File(tmp, "lib/standalone"));
            FileUtils.deleteDirectory(new File(tmp, "lib/test"));
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld.cache""", FileUtils.generateDirectoryListing(tmp));

            project = new CustomWebProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.execute(new String[]{"version"});
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld.cache""", FileUtils.generateDirectoryListing(tmp));

            project = new CustomWebProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.increaseRife2Version();
            project.execute(new String[]{"version"});
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld.cache
                /lib/compile
                /lib/compile/rife2-1.5.12.jar
                /lib/standalone
                /lib/standalone/jetty-http-11.0.14.jar
                /lib/standalone/jetty-io-11.0.14.jar
                /lib/standalone/jetty-jakarta-servlet-api-5.0.2.jar
                /lib/standalone/jetty-security-11.0.14.jar
                /lib/standalone/jetty-server-11.0.14.jar
                /lib/standalone/jetty-servlet-11.0.14.jar
                /lib/standalone/jetty-util-11.0.14.jar
                /lib/standalone/slf4j-api-2.0.7.jar
                /lib/standalone/slf4j-simple-2.0.7.jar
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

            project = new CustomWebProjectAutoPurge(tmp);
            project.enableAutoDownloadPurge();
            project.increaseRife2VersionMore();
            project.execute(new String[]{"version"});
            assertEquals("""
                /lib
                /lib/bld
                /lib/bld/bld.cache
                /lib/compile
                /lib/compile/rife2-1.5.15.jar
                /lib/standalone
                /lib/standalone/jetty-http-11.0.14.jar
                /lib/standalone/jetty-io-11.0.14.jar
                /lib/standalone/jetty-jakarta-servlet-api-5.0.2.jar
                /lib/standalone/jetty-security-11.0.14.jar
                /lib/standalone/jetty-server-11.0.14.jar
                /lib/standalone/jetty-servlet-11.0.14.jar
                /lib/standalone/jetty-util-11.0.14.jar
                /lib/standalone/slf4j-api-2.0.7.jar
                /lib/standalone/slf4j-simple-2.0.7.jar
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
