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
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWarOperation {
    @Test
    void testInstantiation() {
        var operation = new WarOperation();
        assertTrue(operation.libSourceDirectories().isEmpty());
        assertTrue(operation.classesSourceDirectories().isEmpty());
        assertTrue(operation.jarSourceFiles().isEmpty());
        assertNull(operation.webappDirectory());
        assertNull(operation.webXmlFile());
        assertNull(operation.destinationDirectory());
        assertNull(operation.destinationFileName());
    }

    @Test
    void testPopulation() {
        var lib_source_directory1 = new File("libSourceDirectory1");
        var lib_source_directory2 = new File("libSourceDirectory2");
        var classes_source_directory1 = new File("classesSourceDirectory1");
        var classes_source_directory2 = new File("classesSourceDirectory2");
        var jar_source_file1 = new NamedFile("jarSourceFile1", new File("jarSourceFile1"));
        var jar_source_file2 = new NamedFile("jarSourceFile2", new File("jarSourceFile2"));
        var webapp_directory = new File("webappDirectory");
        var web_xml_file = new File("webXmlFile");
        var destination_directory = new File("destinationDirectory");
        var destination_fileName = "destinationFileName";

        var operation1 = new WarOperation()
            .libSourceDirectories(List.of(lib_source_directory1, lib_source_directory2))
            .classesSourceDirectories(List.of(classes_source_directory1, classes_source_directory2))
            .jarSourceFiles(List.of(jar_source_file1, jar_source_file2))
            .webappDirectory(webapp_directory)
            .webXmlFile(web_xml_file)
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName);

        assertTrue(operation1.libSourceDirectories().contains(lib_source_directory1));
        assertTrue(operation1.libSourceDirectories().contains(lib_source_directory2));
        assertTrue(operation1.classesSourceDirectories().contains(classes_source_directory1));
        assertTrue(operation1.classesSourceDirectories().contains(classes_source_directory2));
        assertTrue(operation1.jarSourceFiles().contains(jar_source_file1));
        assertTrue(operation1.jarSourceFiles().contains(jar_source_file2));
        assertEquals(webapp_directory, operation1.webappDirectory());
        assertEquals(web_xml_file, operation1.webXmlFile());
        assertEquals(destination_directory, operation1.destinationDirectory());
        assertEquals(destination_fileName, operation1.destinationFileName());

        var operation2 = new WarOperation()
            .webappDirectory(webapp_directory)
            .webXmlFile(web_xml_file)
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName);
        operation2.libSourceDirectories().add(lib_source_directory1);
        operation2.libSourceDirectories().add(lib_source_directory2);
        operation2.classesSourceDirectories().add(classes_source_directory1);
        operation2.classesSourceDirectories().add(classes_source_directory2);
        operation2.jarSourceFiles().add(jar_source_file1);
        operation2.jarSourceFiles().add(jar_source_file2);

        assertTrue(operation2.libSourceDirectories().contains(lib_source_directory1));
        assertTrue(operation2.libSourceDirectories().contains(lib_source_directory2));
        assertTrue(operation2.classesSourceDirectories().contains(classes_source_directory1));
        assertTrue(operation2.classesSourceDirectories().contains(classes_source_directory2));
        assertTrue(operation2.jarSourceFiles().contains(jar_source_file1));
        assertTrue(operation2.jarSourceFiles().contains(jar_source_file2));
        assertEquals(webapp_directory, operation2.webappDirectory());
        assertEquals(web_xml_file, operation2.webXmlFile());
        assertEquals(destination_directory, operation2.destinationDirectory());
        assertEquals(destination_fileName, operation2.destinationFileName());

        var operation3 = new WarOperation()
            .libSourceDirectories(lib_source_directory1, lib_source_directory2)
            .classesSourceDirectories(classes_source_directory1, classes_source_directory2)
            .jarSourceFiles(jar_source_file1, jar_source_file2)
            .webappDirectory(webapp_directory)
            .webXmlFile(web_xml_file)
            .destinationDirectory(destination_directory)
            .destinationFileName(destination_fileName);

        assertTrue(operation3.libSourceDirectories().contains(lib_source_directory1));
        assertTrue(operation3.libSourceDirectories().contains(lib_source_directory2));
        assertTrue(operation3.classesSourceDirectories().contains(classes_source_directory1));
        assertTrue(operation3.classesSourceDirectories().contains(classes_source_directory2));
        assertTrue(operation3.jarSourceFiles().contains(jar_source_file1));
        assertTrue(operation3.jarSourceFiles().contains(jar_source_file2));
        assertEquals(webapp_directory, operation3.webappDirectory());
        assertEquals(web_xml_file, operation3.webXmlFile());
        assertEquals(destination_directory, operation3.destinationDirectory());
        assertEquals(destination_fileName, operation3.destinationFileName());
    }

    @Test
    void testFromProject()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
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

            new JarOperation()
                .fromProject(create_operation.project())
                .execute();

            var war_operation = new WarOperation()
                .fromProject(create_operation.project());
            war_operation.execute();

            var war_file = new File(war_operation.destinationDirectory(), war_operation.destinationFileName());
            var content = new StringBuilder();
            try (var jar = new JarFile(war_file)) {
                var e = jar.entries();
                while (e.hasMoreElements()) {
                    var jar_entry = e.nextElement();
                    content.append(jar_entry.getName());
                    content.append("\n");
                }
            }

            assertTrue(Pattern.compile("""
                META-INF/MANIFEST\\.MF
                WEB-INF/lib/app-0\\.0\\.1\\.jar
                WEB-INF/lib/rife2-.+\\.jar
                WEB-INF/web\\.xml
                css/style\\.css
                """).matcher(content.toString()).matches());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
