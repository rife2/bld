/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestPrecompileOperation {
    @Test
    void testInstantiation() {
        var operation = new PrecompileOperation();
        assertTrue(operation.templateTypes().isEmpty());
        assertTrue(operation.sourceDirectories().isEmpty());
        assertNull(operation.destinationDirectory());
    }

    @Test
    void testPopulation() {
        var source_directory1 = new File("sourceDirectory1");
        var source_directory2 = new File("sourceDirectory2");
        var destination_directory = new File("destinationDirectory");

        var operation1 = new PrecompileOperation()
            .templateTypes(List.of(TemplateType.HTML, TemplateType.JSON))
            .sourceDirectories(List.of(source_directory1, source_directory2))
            .destinationDirectory(destination_directory);

        assertTrue(operation1.templateTypes().contains(TemplateType.HTML));
        assertTrue(operation1.templateTypes().contains(TemplateType.JSON));
        assertTrue(operation1.sourceDirectories().contains(source_directory1));
        assertTrue(operation1.sourceDirectories().contains(source_directory2));
        assertEquals(destination_directory, operation1.destinationDirectory());

        var operation2 = new PrecompileOperation()
            .destinationDirectory(destination_directory);
        operation2.templateTypes().add(TemplateType.HTML);
        operation2.templateTypes().add(TemplateType.JSON);
        operation2.sourceDirectories().add(source_directory1);
        operation2.sourceDirectories().add(source_directory2);

        assertTrue(operation2.templateTypes().contains(TemplateType.HTML));
        assertTrue(operation2.templateTypes().contains(TemplateType.JSON));
        assertTrue(operation2.sourceDirectories().contains(source_directory1));
        assertTrue(operation2.sourceDirectories().contains(source_directory2));
        assertEquals(destination_directory, operation2.destinationDirectory());

        var operation3 = new PrecompileOperation()
            .templateTypes(TemplateType.HTML, TemplateType.JSON)
            .sourceDirectories(source_directory1, source_directory2)
            .destinationDirectory(destination_directory);

        assertTrue(operation3.templateTypes().contains(TemplateType.HTML));
        assertTrue(operation3.templateTypes().contains(TemplateType.JSON));
        assertTrue(operation3.sourceDirectories().contains(source_directory1));
        assertTrue(operation3.sourceDirectories().contains(source_directory2));
        assertEquals(destination_directory, operation3.destinationDirectory());
    }

    @Test
    void testExecute()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var source1 = new File(tmp, "source1");
            var source2 = new File(tmp, "source2");
            var destination = new File(tmp, "destination");
            source1.mkdirs();
            source2.mkdirs();

            FileUtils.writeString("""
                <p><!--v test1a/--></p>
                """, new File(source1, "source1a.html"));
            FileUtils.writeString("""
                {
                    "test1b": "{{v test1b/}}"
                }
                """, new File(source1, "source1b.json"));
            FileUtils.writeString("""
                <div><!--v test2/--></div>
                """, new File(source2, "source2.html"));

            var precompile_operation = new PrecompileOperation()
                .templateTypes(List.of(TemplateType.HTML, TemplateType.JSON))
                .sourceDirectories(List.of(source1, source2))
                .destinationDirectory(destination);
            precompile_operation.execute();

            assertEquals("""
                    /rife
                    /rife/template
                    /rife/template/html
                    /rife/template/html/source1a.class
                    /rife/template/html/source2.class
                    /rife/template/json
                    /rife/template/json/source1b.class""",
                FileUtils.generateDirectoryListing(destination));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
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

            var precompile_operation = new PrecompileOperation()
                .templateTypes(List.of(TemplateType.HTML))
                .fromProject(create_operation.project());
            precompile_operation.execute();

            assertEquals("""
                    /rife
                    /rife/template
                    /rife/template/html
                    /rife/template/html/hello.class""",
                FileUtils.generateDirectoryListing(precompile_operation.destinationDirectory()));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
