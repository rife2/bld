/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavadocOperation {
    @Test
    void testInstantiation() {
        var operation = new JavadocOperation();
        assertNull(operation.buildDirectory());
        assertTrue(operation.classpath().isEmpty());
        assertTrue(operation.sourceFiles().isEmpty());
        assertTrue(operation.javadocOptions().isEmpty());
        assertTrue(operation.diagnostics().isEmpty());
        assertTrue(operation.included().isEmpty());
        assertTrue(operation.excluded().isEmpty());
    }

    @Test
    void testPopulation() {
        var build_directory = new File("buildDirectory");
        var classpath1 = "classpath1";
        var classpath2 = "classpath2";
        var source_file1 = new File("sourceFile1");
        var source_file2 = new File("sourceFile2");
        var source_dir1 = new File("sourceDir1");
        var source_dir2 = new File("sourceDir2");
        var javadoc_option1 = "javadocOption1";
        var javadoc_option2 = "javadocOption2";
        var included1 = Pattern.compile("included1");
        var included2 = Pattern.compile("included2");
        var excluded1 = Pattern.compile("excluded1");
        var excluded2 = Pattern.compile("excluded2");

        var operation1 = new JavadocOperation()
            .buildDirectory(build_directory)
            .classpath(List.of(classpath1, classpath2))
            .sourceFiles(List.of(source_file1, source_file2))
            .sourceDirectories(List.of(source_dir1, source_dir2))
            .javadocOptions(List.of(javadoc_option1, javadoc_option2))
            .included(List.of(included1, included2))
            .excluded(List.of(excluded1, excluded2));

        assertEquals(build_directory, operation1.buildDirectory());
        assertTrue(operation1.classpath().contains(classpath1));
        assertTrue(operation1.classpath().contains(classpath2));
        assertTrue(operation1.sourceFiles().contains(source_file1));
        assertTrue(operation1.sourceFiles().contains(source_file2));
        assertTrue(operation1.sourceDirectories().contains(source_dir1));
        assertTrue(operation1.sourceDirectories().contains(source_dir2));
        assertTrue(operation1.javadocOptions().contains(javadoc_option1));
        assertTrue(operation1.javadocOptions().contains(javadoc_option2));
        assertTrue(operation1.included().contains(included1));
        assertTrue(operation1.included().contains(included2));
        assertTrue(operation1.excluded().contains(excluded1));
        assertTrue(operation1.excluded().contains(excluded2));

        var operation2 = new JavadocOperation()
            .buildDirectory(build_directory);
        operation2.classpath().add(classpath1);
        operation2.classpath().add(classpath2);
        operation2.sourceFiles().add(source_file1);
        operation2.sourceFiles().add(source_file2);
        operation2.sourceDirectories().add(source_dir1);
        operation2.sourceDirectories().add(source_dir2);
        operation2.javadocOptions().add(javadoc_option1);
        operation2.javadocOptions().add(javadoc_option2);
        operation2.included().add(included1);
        operation2.included().add(included2);
        operation2.excluded().add(excluded1);
        operation2.excluded().add(excluded2);

        assertEquals(build_directory, operation2.buildDirectory());
        assertTrue(operation2.classpath().contains(classpath1));
        assertTrue(operation2.classpath().contains(classpath2));
        assertTrue(operation2.sourceFiles().contains(source_file1));
        assertTrue(operation2.sourceFiles().contains(source_file2));
        assertTrue(operation2.sourceDirectories().contains(source_dir1));
        assertTrue(operation2.sourceDirectories().contains(source_dir2));
        assertTrue(operation2.javadocOptions().contains(javadoc_option1));
        assertTrue(operation2.javadocOptions().contains(javadoc_option2));
        assertTrue(operation2.included().contains(included1));
        assertTrue(operation2.included().contains(included2));
        assertTrue(operation2.excluded().contains(excluded1));
        assertTrue(operation2.excluded().contains(excluded2));

        var operation3 = new JavadocOperation()
            .buildDirectory(build_directory)
            .classpath(classpath1, classpath2)
            .sourceFiles(source_file1, source_file2)
            .sourceDirectories(source_dir1, source_dir2)
            .javadocOptions(List.of(javadoc_option1, javadoc_option2))
            .included(included1, included2)
            .excluded(excluded1, excluded2);

        assertEquals(build_directory, operation3.buildDirectory());
        assertTrue(operation3.classpath().contains(classpath1));
        assertTrue(operation3.classpath().contains(classpath2));
        assertTrue(operation3.sourceFiles().contains(source_file1));
        assertTrue(operation3.sourceFiles().contains(source_file2));
        assertTrue(operation3.sourceDirectories().contains(source_dir1));
        assertTrue(operation3.sourceDirectories().contains(source_dir2));
        assertTrue(operation3.javadocOptions().contains(javadoc_option1));
        assertTrue(operation3.javadocOptions().contains(javadoc_option2));
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
            var source_file1 = new File(tmp, "Source1.java");
            var source_file2 = new File(tmp, "Source2.java");
            var source_file3 = new File(tmp, "Source3.java");

            FileUtils.writeString("""
                public class Source1 {
                    public final String name_;
                    public Source1() {
                        name_ = "source1";
                    }
                }
                """, source_file1);

            FileUtils.writeString("""
                public class Source2 {
                    public final String name_;
                    public Source2(Source1 source1) {
                        name_ = source1.name_;
                    }
                }
                """, source_file2);

            FileUtils.writeString("""
                public class Source3 {
                    public final String name1_;
                    public final String name2_;
                    public Source3(Source1 source1, Source2 source2) {
                        name1_ = source1.name_;
                        name2_ = source2.name_;
                    }
                }
                """, source_file3);
            var build = new File(tmp, "build");
            var build_html1 = new File(build, "Source1.html");
            var build_html2 = new File(build, "Source2.html");
            var build_html3 = new File(build, "Source3.html");
            var build_index_html = new File(build, "index.html");
            var build_index_all_html = new File(build, "index-all.html");

            assertFalse(build_html1.exists());
            assertFalse(build_html2.exists());
            assertFalse(build_html3.exists());
            assertFalse(build_index_html.exists());
            assertFalse(build_index_all_html.exists());

            var operation = new JavadocOperation()
                .buildDirectory(build)
                .classpath(List.of(build.getAbsolutePath()))
                .sourceFiles(List.of(source_file1, source_file2, source_file3));
            operation.execute();
            assertTrue(operation.diagnostics().isEmpty());

            assertTrue(build_html1.exists());
            assertTrue(build_html2.exists());
            assertTrue(build_html3.exists());
            assertTrue(build_index_html.exists());
            assertTrue(build_index_all_html.exists());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testIncludeExclude()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var source_file1 = new File(tmp, "Source1.java");
            var source_file2 = new File(tmp, "Source2.java");
            var source_file3 = new File(tmp, "Source3.java");

            FileUtils.writeString("""
                public class Source1 {
                    public final String name_;
                    public Source1() {
                        name_ = "source1";
                    }
                }
                """, source_file1);

            FileUtils.writeString("""
                public class Source2 {
                    public final String name_;
                    public Source2(String name) {
                        name_ = name;
                    }
                }
                """, source_file2);

            FileUtils.writeString("""
                public class Source3 {
                    public final String name1_;
                    public final String name2_;
                    public Source3(Source1 source1, Source2 source2) {
                        name1_ = source1.name_;
                        name2_ = source2.name_;
                    }
                }
                """, source_file3);
            var build = new File(tmp, "build");
            var build_html1 = new File(build, "Source1.html");
            var build_html2 = new File(build, "Source2.html");
            var build_html3 = new File(build, "Source3.html");
            var build_index_html = new File(build, "index.html");
            var build_index_all_html = new File(build, "index-all.html");

            assertFalse(build_html1.exists());
            assertFalse(build_html2.exists());
            assertFalse(build_html3.exists());
            assertFalse(build_index_html.exists());
            assertFalse(build_index_all_html.exists());

            var operation = new JavadocOperation()
                .buildDirectory(build)
                .classpath(List.of(build.getAbsolutePath()))
                .sourceFiles(List.of(source_file1, source_file2, source_file3))
                .included("Source([12])")
                .excluded("Source1");
            operation.execute();
            assertTrue(operation.diagnostics().isEmpty());

            assertFalse(build_html1.exists());
            assertTrue(build_html2.exists());
            assertFalse(build_html3.exists());
            assertTrue(build_index_html.exists());
            assertTrue(build_index_all_html.exists());
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

            var javadoc_operation = new JavadocOperation()
                .fromProject(create_operation.project());

            var main_app_html = new File(new File(javadoc_operation.buildDirectory(), "tst"), "AppMain.html");
            var index_html = new File(javadoc_operation.buildDirectory(), "index.html");
            var index_all_html = new File(javadoc_operation.buildDirectory(), "index-all.html");
            assertFalse(main_app_html.exists());
            assertFalse(index_html.exists());
            assertFalse(index_all_html.exists());

            javadoc_operation.execute();
            assertTrue(javadoc_operation.diagnostics().isEmpty());

            assertTrue(main_app_html.exists());
            assertTrue(index_html.exists());
            assertTrue(index_all_html.exists());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteGenerationErrors()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var source_file1 = new File(tmp, "Source1.java");
            var source_file2 = new File(tmp, "Source2.java");
            var source_file3 = new File(tmp, "Source3.java");

            FileUtils.writeString("""
                public class Source1 {
                    public final String;
                    public Source1() {
                        name_ = "source1";
                    }
                }
                """, source_file1);

            FileUtils.writeString("""
                public class Source2 {
                    public final String name_;
                    public Source2(Source1B source1) {
                        noName_ = source1.name_;
                    }
                }
                """, source_file2);

            FileUtils.writeString("""
                public class Source3 {
                    public final String name1_;
                    public final String name2_;
                    public Source3(Source1 source1, Source2 source2) {
                        name_ = source1.name_;
                        name_ = source2.name_;
                    }
                }
                """, source_file3);
            var build = new File(tmp, "build");
            var build_html1 = new File(build, "Source1.html");
            var build_html2 = new File(build, "Source2.html");
            var build_html3 = new File(build, "Source3.html");
            var build_index_html = new File(build, "index.html");
            var build_index_all_html = new File(build, "index-all.html");

            assertFalse(build_html1.exists());
            assertFalse(build_html2.exists());
            assertFalse(build_html3.exists());
            assertFalse(build_index_html.exists());
            assertFalse(build_index_all_html.exists());

            var operation = new JavadocOperation() {
                public void executeProcessDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
                    // don't output diagnostics
                }
            };
            operation.buildDirectory(build)
                .classpath(List.of(build.getAbsolutePath()))
                .sourceFiles(List.of(source_file1, source_file2, source_file3));
            assertThrows(ExitStatusException.class, operation::execute);
            assertEquals(2, operation.diagnostics().size());

            var diagnostic1 = operation.diagnostics().get(0);

            assertEquals("/Source1.java", diagnostic1.getSource().toUri().getPath().substring(tmp.getAbsolutePath().length()));

            assertFalse(build_html1.exists());
            assertFalse(build_html2.exists());
            assertFalse(build_html3.exists());
            assertFalse(build_index_html.exists());
            assertFalse(build_index_all_html.exists());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
