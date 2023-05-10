/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.NamedFile;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TestJarOperation {
    @Test
    void testInstantiation() {
        var operation = new JarOperation();
        assertTrue(operation.manifestAttributes().isEmpty());
        assertTrue(operation.sourceDirectories().isEmpty());
        assertTrue(operation.sourceFiles().isEmpty());
        assertNull(operation.destinationDirectory());
        assertNull(operation.destinationFileName());
        assertTrue(operation.included().isEmpty());
        assertTrue(operation.excluded().isEmpty());
    }

    @Test
    void testPopulation() {
        var manifest_attribute_1a = Attributes.Name.MANIFEST_VERSION;
        var manifest_attribute_1b = "manifestAttribute1";
        var manifest_attribute_2a = Attributes.Name.MAIN_CLASS;
        var manifest_Attribute_2b = "manifestAttribute2";
        var source_directory1 = new File("sourceDirectory1");
        var source_directory2 = new File("sourceDirectory2");
        var source_file1 = new NamedFile("sourceFile1", new File("sourceFile1"));
        var source_file2 = new NamedFile("sourceFile2", new File("sourceFile2"));
        var destination_directory = new File("destinationDirectory");
        var destination_fileName = "destinationFileName";
        var included1 = Pattern.compile("included1");
        var included2 = Pattern.compile("included2");
        var excluded1 = Pattern.compile("excluded1");
        var excluded2 = Pattern.compile("excluded2");

        var operation1 = new JarOperation()
            .manifestAttributes(Map.of(manifest_attribute_1a, manifest_attribute_1b, manifest_attribute_2a, manifest_Attribute_2b))
            .sourceDirectories(List.of(source_directory1, source_directory2))
            .sourceFiles(List.of(source_file1, source_file2))
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName)
            .included(List.of(included1, included2))
            .excluded(List.of(excluded1, excluded2));

        assertEquals(manifest_attribute_1b, operation1.manifestAttributes().get(manifest_attribute_1a));
        assertEquals(manifest_Attribute_2b, operation1.manifestAttributes().get(manifest_attribute_2a));
        assertTrue(operation1.sourceDirectories().contains(source_directory1));
        assertTrue(operation1.sourceDirectories().contains(source_directory2));
        assertTrue(operation1.sourceFiles().contains(source_file1));
        assertTrue(operation1.sourceFiles().contains(source_file2));
        assertEquals(destination_directory, operation1.destinationDirectory());
        assertEquals(destination_fileName, operation1.destinationFileName());
        assertEquals(new File(destination_directory, destination_fileName), operation1.destinationFile());
        assertTrue(operation1.included().contains(included1));
        assertTrue(operation1.included().contains(included2));
        assertTrue(operation1.excluded().contains(excluded1));
        assertTrue(operation1.excluded().contains(excluded2));

        var operation2 = new JarOperation()
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName);
        operation2.manifestAttributes().put(manifest_attribute_1a, manifest_attribute_1b);
        operation2.manifestAttributes().put(manifest_attribute_2a, manifest_Attribute_2b);
        operation2.sourceDirectories().add(source_directory1);
        operation2.sourceDirectories().add(source_directory2);
        operation2.sourceFiles().add(source_file1);
        operation2.sourceFiles().add(source_file2);
        operation2.included().add(included1);
        operation2.included().add(included2);
        operation2.excluded().add(excluded1);
        operation2.excluded().add(excluded2);

        assertEquals(manifest_attribute_1b, operation2.manifestAttributes().get(manifest_attribute_1a));
        assertEquals(manifest_Attribute_2b, operation2.manifestAttributes().get(manifest_attribute_2a));
        assertTrue(operation2.sourceDirectories().contains(source_directory1));
        assertTrue(operation2.sourceDirectories().contains(source_directory2));
        assertTrue(operation2.sourceFiles().contains(source_file1));
        assertTrue(operation2.sourceFiles().contains(source_file2));
        assertEquals(destination_directory, operation2.destinationDirectory());
        assertEquals(destination_fileName, operation2.destinationFileName());
        assertTrue(operation2.included().contains(included1));
        assertTrue(operation2.included().contains(included2));
        assertTrue(operation2.excluded().contains(excluded1));
        assertTrue(operation2.excluded().contains(excluded2));

        var operation3 = new JarOperation()
            .manifestAttribute(manifest_attribute_1a, manifest_attribute_1b)
            .manifestAttribute(manifest_attribute_2a, manifest_Attribute_2b)
            .sourceDirectories(source_directory1, source_directory2)
            .sourceFiles(source_file1, source_file2)
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName)
            .included(included1, included2)
            .excluded(excluded1, excluded2);

        assertEquals(manifest_attribute_1b, operation3.manifestAttributes().get(manifest_attribute_1a));
        assertEquals(manifest_Attribute_2b, operation3.manifestAttributes().get(manifest_attribute_2a));
        assertTrue(operation3.sourceDirectories().contains(source_directory1));
        assertTrue(operation3.sourceDirectories().contains(source_directory2));
        assertTrue(operation3.sourceFiles().contains(source_file1));
        assertTrue(operation3.sourceFiles().contains(source_file2));
        assertEquals(destination_directory, operation3.destinationDirectory());
        assertEquals(destination_fileName, operation3.destinationFileName());
        assertTrue(operation3.included().contains(included1));
        assertTrue(operation3.included().contains(included2));
        assertTrue(operation3.excluded().contains(excluded1));
        assertTrue(operation3.excluded().contains(excluded2));
    }

    @Test
    void testExecute()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var source_dir = new File(tmp, "source");
            var destination_dir = new File(tmp, "destination");
            var destination_name = "archive.jar";

            source_dir.mkdirs();
            FileUtils.writeString("source1", new File(source_dir, "source1.text"));
            FileUtils.writeString("source2", new File(source_dir, "source2.text"));
            FileUtils.writeString("source3", new File(source_dir, "source3.text"));
            FileUtils.writeString("source4", new File(source_dir, "source4.txt"));
            var source5 = new File(tmp, "source5.text");
            var source6 = new File(tmp, "source6.text");
            FileUtils.writeString("source5", source5);
            FileUtils.writeString("source6", source6);

            new JarOperation()
                .sourceDirectories(List.of(source_dir))
                .sourceFiles(List.of(
                    new NamedFile("src5.txt", source5),
                    new NamedFile("src6.txt", source6)))
                .destinationDirectory(destination_dir)
                .destinationFileName(destination_name)
                .included("source.*\\.text")
                .excluded("source5.*")
                .execute();

            var jar_archive = new File(destination_dir, destination_name);
            assertTrue(jar_archive.exists());

            var content = new StringBuilder();
            try (var jar = new JarFile(jar_archive)) {
                var e = jar.entries();
                while (e.hasMoreElements()) {
                    var jar_entry = e.nextElement();
                    content.append(jar_entry.getName());
                    content.append("\n");
                }
            }

            assertEquals("""
                META-INF/MANIFEST.MF
                source1.text
                source2.text
                source3.text
                src6.txt
                """, content.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testFromProject()
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

            var jar_operation = new JarOperation()
                .fromProject(create_operation.project());
            jar_operation.execute();

            var content = new StringBuilder();
            try (var jar = new JarFile(new File(jar_operation.destinationDirectory(), jar_operation.destinationFileName()))) {
                var e = jar.entries();
                while (e.hasMoreElements()) {
                    var jar_entry = e.nextElement();
                    content.append(jar_entry.getName());
                    content.append("\n");
                }
            }

            assertEquals("""
                META-INF/MANIFEST.MF
                tst/AppMain.class
                """, content.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
