/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.WebProject;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestCleanOperation {
    @Test
    void testInstantiation() {
        var operation = new CleanOperation();
        assertTrue(operation.directories().isEmpty());
    }

    @Test
    void testPopulation() {
        var dir1 = new File("dir1");
        var dir2 = new File("dir2");
        var dir3 = new File("dir3");

        var operation1 = new CleanOperation();
        operation1.directories(List.of(dir1, dir2, dir3));
        assertFalse(operation1.directories().isEmpty());
        assertTrue(operation1.directories().contains(dir1));
        assertTrue(operation1.directories().contains(dir2));
        assertTrue(operation1.directories().contains(dir3));

        var operation2 = new CleanOperation();
        operation2.directories().add(dir1);
        operation2.directories().add(dir2);
        operation2.directories().add(dir3);
        assertFalse(operation2.directories().isEmpty());
        assertTrue(operation2.directories().contains(dir1));
        assertTrue(operation2.directories().contains(dir2));
        assertTrue(operation2.directories().contains(dir3));

        var operation3 = new CleanOperation();
        operation3.directories(dir1, dir2, dir3);
        assertFalse(operation3.directories().isEmpty());
        assertTrue(operation3.directories().contains(dir1));
        assertTrue(operation3.directories().contains(dir2));
        assertTrue(operation3.directories().contains(dir3));
    }

    @Test
    void testExecute()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var dir1 = new File(tmp, "dir1");
            var dir2 = new File(tmp, "dir2");
            var dir3 = new File(tmp, "dir3");
            dir1.mkdirs();
            dir2.mkdirs();
            dir3.mkdirs();

            var file1 = new File(dir1, "file1");
            var file2 = new File(dir2, "file2");
            var file3 = new File(dir3, "file3");
            file1.createNewFile();
            file2.createNewFile();
            file3.createNewFile();

            FileUtils.writeString("content1", file1);
            FileUtils.writeString("content2", file2);
            FileUtils.writeString("content3", file3);

            assertEquals(1, dir1.list().length);
            assertEquals(1, dir2.list().length);
            assertEquals(1, dir3.list().length);

            new CleanOperation().directories(List.of(dir1, dir2)).execute();

            assertFalse(dir1.exists());
            assertFalse(dir2.exists());
            assertTrue(dir3.exists());

            assertEquals(1, dir3.list().length);
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
            project.createBuildStructure();
            assertEquals("""
                    /build
                    /build/bld
                    /build/dist
                    /build/javadoc
                    /build/main
                    /build/test
                    /lib
                    /lib/bld
                    /lib/compile
                    /lib/runtime
                    /lib/standalone
                    /lib/test
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

            new CleanOperation().fromProject(project).execute();
            assertEquals("""
                    /build
                    /build/bld
                    /lib
                    /lib/bld
                    /lib/compile
                    /lib/runtime
                    /lib/standalone
                    /lib/test
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
