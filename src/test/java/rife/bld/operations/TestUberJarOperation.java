/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.NamedFile;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUberJarOperation {
    @Test
    void testInstantiation() {
        var operation = new UberJarOperation();
        assertTrue(operation.jarSourceFiles().isEmpty());
        assertTrue(operation.sourceDirectories().isEmpty());
        assertNull(operation.destinationDirectory());
        assertNull(operation.destinationFileName());
        assertNull(operation.mainClass());
    }

    @Test
    void testPopulation() {
        var jar_source_file1 = new File("jarSourceFile1");
        var jar_source_file2 = new File("jarSourceFile2");
        var resource_source_directory1 = new NamedFile("resourceSourceDirectory1", new File("resourceSourceDirectory1"));
        var resource_source_directory2 = new NamedFile("resourceSourceDirectory2", new File("resourceSourceDirectory2"));
        var destination_directory = new File("destinationDirectory");
        var destination_fileName = "destinationFileName";
        var main_class = "mainClass";

        var operation1 = new UberJarOperation()
            .jarSourceFiles(List.of(jar_source_file1, jar_source_file2))
            .sourceDirectories(List.of(resource_source_directory1, resource_source_directory2))
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName)
            .mainClass(main_class);

        assertTrue(operation1.jarSourceFiles().contains(jar_source_file1));
        assertTrue(operation1.jarSourceFiles().contains(jar_source_file2));
        assertTrue(operation1.sourceDirectories().contains(resource_source_directory1));
        assertTrue(operation1.sourceDirectories().contains(resource_source_directory2));
        assertEquals(destination_directory, operation1.destinationDirectory());
        assertEquals(destination_fileName, operation1.destinationFileName());
        assertEquals(main_class, operation1.mainClass());

        var operation2 = new UberJarOperation()
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName)
            .mainClass(main_class);
        operation2.jarSourceFiles().add(jar_source_file1);
        operation2.jarSourceFiles().add(jar_source_file2);
        operation2.sourceDirectories().add(resource_source_directory1);
        operation2.sourceDirectories().add(resource_source_directory2);
        assertEquals(main_class, operation1.mainClass());

        assertTrue(operation2.jarSourceFiles().contains(jar_source_file1));
        assertTrue(operation2.jarSourceFiles().contains(jar_source_file2));
        assertTrue(operation2.sourceDirectories().contains(resource_source_directory1));
        assertTrue(operation2.sourceDirectories().contains(resource_source_directory2));
        assertEquals(destination_directory, operation2.destinationDirectory());
        assertEquals(destination_fileName, operation2.destinationFileName());

        var operation3 = new UberJarOperation()
            .jarSourceFiles(jar_source_file1, jar_source_file2)
            .sourceDirectories(resource_source_directory1, resource_source_directory2)
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName)
            .mainClass(main_class);

        assertTrue(operation3.jarSourceFiles().contains(jar_source_file1));
        assertTrue(operation3.jarSourceFiles().contains(jar_source_file2));
        assertTrue(operation3.sourceDirectories().contains(resource_source_directory1));
        assertTrue(operation3.sourceDirectories().contains(resource_source_directory2));
        assertEquals(destination_directory, operation3.destinationDirectory());
        assertEquals(destination_fileName, operation3.destinationFileName());
        assertEquals(main_class, operation3.mainClass());
    }

    @Test
    void testFromProjectBlank()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateBlankOperation()
                .workDirectory(tmp)
                .packageName("tst")
                .projectName("app")
                .downloadDependencies(true);
            create_operation.execute();

            new CompileOperation()
                .fromProject(create_operation.project())
                .execute();

            new JarOperation()
                .fromProject(create_operation.project())
                .execute();

            var uberjar_operation = new UberJarOperation()
                .fromProject(create_operation.project());
            uberjar_operation.execute();

            var uberjar_file = new File(uberjar_operation.destinationDirectory(), uberjar_operation.destinationFileName());
            try (var jar = new JarFile(uberjar_file)) {
                assertEquals(2, jar.size());
            }

            var check_result = new StringBuilder();
            new RunOperation()
                .javaOptions(List.of("-jar"))
                .mainClass(uberjar_file.getAbsolutePath())
                .outputProcessor(s -> {
                    check_result.append(s);
                    return true;
                })
                .execute();
            assertEquals("Hello World!", check_result.toString());

        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testFromProjectWeb()
    throws Exception {
        var tmp = Files.createTempDirectory("web").toFile();
        try {
            var create_operation = new CreateRife2Operation()
                .workDirectory(tmp)
                .packageName("tst")
                .projectName("app")
                .downloadDependencies(true);
            create_operation.execute();

            new CompileOperation()
                .fromProject(create_operation.project())
                .execute();

            create_operation.project().uberjar();

            var uberjar_file = new File(create_operation.project().buildDistDirectory(), create_operation.project().uberJarFileName());
            try (var jar = new JarFile(uberjar_file)) {
                assertTrue(jar.size() > 1300);
            }

            var check_result = new StringBuilder();
            var run_operation = new RunOperation()
                .javaOptions(List.of("-jar"))
                .mainClass(uberjar_file.getAbsolutePath());
            var executor = Executors.newSingleThreadScheduledExecutor();
            var checked_url = new URL("http://localhost:8080");
            executor.schedule(() -> {
                try {
                    check_result.append(FileUtils.readString(checked_url));
                } catch (FileUtilsErrorException e) {
                    throw new RuntimeException(e);
                }
            }, 1, TimeUnit.SECONDS);
            executor.schedule(() -> run_operation.process().destroy(), 2, TimeUnit.SECONDS);
            assertThrows(ExitStatusException.class, run_operation::execute);

            assertTrue(check_result.toString().contains("<p>Hello World App</p>"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
