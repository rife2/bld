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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestJUnitOperation {
    @Test
    void testInstantiation() {
        var operation = new JUnitOperation();
        assertNotNull(operation.workDirectory());
        assertTrue(operation.workDirectory().exists());
        assertTrue(operation.workDirectory().isDirectory());
        assertTrue(operation.workDirectory().canWrite());
        assertEquals("java", operation.javaTool());
        assertTrue(operation.javaOptions().isEmpty());
        assertTrue(operation.classpath().isEmpty());
        assertNull(operation.mainClass());
        assertNull(operation.outputProcessor());
        assertNull(operation.errorProcessor());
        assertNull(operation.process());
    }

    @Test
    void testPopulation()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            var java_tool = "javatool";
            var test_java_option1 = "testJavaOption1";
            var test_java_option2 = "testJavaOption2";
            var test_classpath1 = "testClasspath1";
            var test_classpath2 = "testClasspath2";
            var test_tool_main_class = "testToolMainClass";
            var test_tool_option1 = "testToolOption1";
            var test_tool_option2 = "testToolOption2";
            Function<String, Boolean> test_output_consumer = (String) -> true;
            Function<String, Boolean> test_error_consumer = (String) -> true;

            var operation1 = new JUnitOperation();
            operation1
                .workDirectory(work_directory)
                .javaTool(java_tool)
                .javaOptions(List.of(test_java_option1, test_java_option2))
                .testToolOptions(List.of(test_tool_option1, test_tool_option2))
                .classpath(List.of(test_classpath1, test_classpath2))
                .mainClass(test_tool_main_class)
                .outputProcessor(test_output_consumer)
                .errorProcessor(test_error_consumer);

            assertEquals(work_directory, operation1.workDirectory());
            assertEquals(java_tool, operation1.javaTool());
            assertTrue(operation1.javaOptions().contains(test_java_option1));
            assertTrue(operation1.javaOptions().contains(test_java_option2));
            assertTrue(operation1.testToolOptions().contains(test_tool_option1));
            assertTrue(operation1.testToolOptions().contains(test_tool_option2));
            assertTrue(operation1.classpath().contains(test_classpath1));
            assertTrue(operation1.classpath().contains(test_classpath2));
            assertEquals(test_tool_main_class, operation1.mainClass());
            assertSame(test_output_consumer, operation1.outputProcessor());
            assertSame(test_error_consumer, operation1.errorProcessor());

            var operation2 = new JUnitOperation();
            operation2.workDirectory(work_directory);
            operation2.javaTool(java_tool);
            operation2.javaOptions().add(test_java_option1);
            operation2.javaOptions().add(test_java_option2);
            operation2.testToolOptions().add(test_tool_option1);
            operation2.testToolOptions().add(test_tool_option2);
            operation2.classpath().add(test_classpath1);
            operation2.classpath().add(test_classpath2);
            operation2.mainClass(test_tool_main_class);
            operation2.outputProcessor(test_output_consumer);
            operation2.errorProcessor(test_error_consumer);

            assertEquals(work_directory, operation2.workDirectory());
            assertEquals(java_tool, operation2.javaTool());
            assertTrue(operation2.javaOptions().contains(test_java_option1));
            assertTrue(operation2.javaOptions().contains(test_java_option2));
            assertTrue(operation2.testToolOptions().contains(test_tool_option1));
            assertTrue(operation2.testToolOptions().contains(test_tool_option2));
            assertTrue(operation2.classpath().contains(test_classpath1));
            assertTrue(operation2.classpath().contains(test_classpath2));
            assertEquals(test_tool_main_class, operation2.mainClass());
            assertSame(test_output_consumer, operation2.outputProcessor());
            assertSame(test_error_consumer, operation2.errorProcessor());

            var operation3 = new JUnitOperation();
            operation3
                .classpath(test_classpath1, test_classpath2)
                .testToolOptions(test_tool_option1, test_tool_option2);

            assertTrue(operation3.classpath().contains(test_classpath1));
            assertTrue(operation3.classpath().contains(test_classpath2));
            assertTrue(operation3.testToolOptions().contains(test_tool_option1));
            assertTrue(operation3.testToolOptions().contains(test_tool_option2));
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    @Test
    void testExecute()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var source_file1 = new File(tmp, "Source1.java");
            var source_file2 = new File(tmp, "Source2.java");

            FileUtils.writeString("""
                public class Source1 {
                    public final String name_;
                    public Source1() {
                        name_ = "source1";
                    }
                    
                    public static void main(String[] arguments)
                    throws Exception {
                        System.out.print(new Source1().name_);
                    }
                }
                """, source_file1);
            FileUtils.writeString("""
                public class Source2 {
                    public static void main(String[] arguments)
                    throws Exception {
                        System.out.print(new Source1().name_.equals("source1"));
                    }
                }
                """, source_file2);
            var build_main = new File(tmp, "buildMain");
            var build_test = new File(tmp, "buildTest");

            var compile_operation = new CompileOperation()
                .buildMainDirectory(build_main)
                .buildTestDirectory(build_test)
                .compileMainClasspath(List.of(build_main.getAbsolutePath()))
                .compileTestClasspath(List.of(build_main.getAbsolutePath(), build_test.getAbsolutePath()))
                .mainSourceFiles(List.of(source_file1))
                .testSourceFiles(List.of(source_file2));
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());

            var output = new StringBuilder();
            var test_operation = new JUnitOperation()
                .mainClass("Source2")
                .classpath(List.of(build_main.getAbsolutePath(), build_test.getAbsolutePath()))
                .outputProcessor(s -> {
                    output.append(s);
                    return true;
                });
            test_operation.execute();

            assertEquals("true", output.toString());
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
                .packageName("com.example")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation.execute();

            new CompileOperation()
                .fromProject(create_operation.project()).execute();

            var check_result = new StringBuilder();
            new JUnitOperation()
                .fromProject(create_operation.project())
                .outputProcessor(s -> {
                    check_result.append(s).append(System.lineSeparator());
                    return true;
                })
                .execute();
            assertTrue(check_result.toString().contains("""
                [         2 containers found      ]
                [         0 containers skipped    ]
                [         2 containers started    ]
                [         0 containers aborted    ]
                [         2 containers successful ]
                [         0 containers failed     ]
                [         1 tests found           ]
                [         0 tests skipped         ]
                [         1 tests started         ]
                [         0 tests aborted         ]
                [         1 tests successful      ]
                [         0 tests failed          ]
                """));

        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
