/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.VersionNumber;
import rife.tools.FileUtils;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

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
        assertThrows(IllegalStateException.class, project::mainClass);

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
}
