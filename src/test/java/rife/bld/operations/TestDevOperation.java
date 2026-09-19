/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.WebProject;
import rife.tools.FileUtils;

import rife.bld.operations.exceptions.ExitStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestDevOperation {
    @Test
    void testInstantiation() {
        var operation = new DevOperation();
        assertNotNull(operation.workDirectory());
        assertTrue(operation.workDirectory().exists());
        assertTrue(operation.workDirectory().isDirectory());
        assertTrue(operation.workDirectory().canWrite());
        assertNotNull(operation.javaTool());
        assertTrue(operation.javaOptions().isEmpty());
        assertTrue(operation.classpath().isEmpty());
        assertNull(operation.mainClass());
        assertTrue(operation.runOptions().isEmpty());
        assertTrue(operation.sourceDirectories().isEmpty());
        assertNull(operation.compileOperation());
        assertNull(operation.debugPort());
    }

    @Test
    void testCommandList()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            var operation = new DevOperation()
                .workDirectory(work_directory)
                .javaTool("javatool")
                .javaOptions(List.of("-Xmx1g"))
                .classpath(List.of("classes", "lib.jar"))
                .mainClass("com.example.AppSite")
                .runOptions("one", "two");
            assertEquals(List.of("javatool", "-Xmx1g", "-cp", FileUtils.joinPaths(List.of("classes", "lib.jar")),
                    DevOperation.RELOADER_CLASS, "com.example.AppSite", "one", "two"),
                operation.executeConstructProcessCommandList());

            operation.debugPort("");
            assertTrue(operation.executeConstructProcessCommandList().contains("-D" + DevOperation.RELOAD_DEBUG_PROPERTY));

            operation.debugPort("5006");
            assertTrue(operation.executeConstructProcessCommandList().contains("-D" + DevOperation.RELOAD_DEBUG_PROPERTY + "=5006"));
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    @Test
    void testFromProjectParsesOptions()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            var arguments = new ArrayList<>(List.of(DevOperation.DEBUG_OPTION + "=5007", DevOperation.ARGS_OPTION + "a b", "other"));
            var project = new WebProject() {
                {
                    workDirectory = work_directory;
                    pkg = "test.pkg";
                    name = "test";
                    mainClass = "test.pkg.AppSite";
                    version = version(0, 0, 1);
                    compileOperation().compileOptions().add("-parameters");
                }

                public List<String> arguments() {
                    return arguments;
                }
            };

            var operation = new DevOperation().runOptions("configured").fromProject(project);
            assertEquals("5007", operation.debugPort());
            assertEquals(List.of("a", "b", "configured"), operation.runOptions());
            assertEquals(List.of("other"), arguments);
            assertEquals("test.pkg.AppSite", operation.mainClass());
            assertEquals(List.of(project.srcMainJavaDirectory()), operation.sourceDirectories());
            assertNotNull(operation.compileOperation());
            assertTrue(operation.compileOperation().get().compileOptions().contains("-parameters"));
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    @Test
    void testFromProjectDefaultDebugPort()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            var arguments = new ArrayList<>(List.of(DevOperation.DEBUG_OPTION));
            var project = new WebProject() {
                {
                    workDirectory = work_directory;
                    pkg = "test.pkg";
                    name = "test";
                    mainClass = "test.pkg.AppSite";
                    version = version(0, 0, 1);
                }

                public List<String> arguments() {
                    return arguments;
                }
            };

            assertEquals("", new DevOperation().fromProject(project).debugPort());
            assertTrue(arguments.isEmpty());
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    @Test
    void testReloaderAvailability()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            assertFalse(DevOperation.reloaderAvailable(List.of(work_directory.getAbsolutePath())));

            var classes = new File(work_directory, "classes");
            var reloader = new File(classes, "rife/engine/Reloader.class");
            reloader.getParentFile().mkdirs();
            FileUtils.writeString("", reloader);
            assertTrue(DevOperation.reloaderAvailable(List.of(classes.getAbsolutePath())));

            var jar = new File(work_directory, "rife2.jar");
            try (var out = new JarOutputStream(new FileOutputStream(jar))) {
                out.putNextEntry(new JarEntry("rife/engine/Reloader.class"));
                out.closeEntry();
            }
            assertTrue(DevOperation.reloaderAvailable(List.of(jar.getAbsolutePath())));

            var other = new File(work_directory, "other.jar");
            try (var out = new JarOutputStream(new FileOutputStream(other))) {
                out.putNextEntry(new JarEntry("rife/engine/Other.class"));
                out.closeEntry();
            }
            assertFalse(DevOperation.reloaderAvailable(List.of(other.getAbsolutePath())));

            assertFalse(new DevOperation().classpath(List.of(other.getAbsolutePath())).reloaderAvailable());
            assertTrue(new DevOperation().classpath(List.of(other.getAbsolutePath())).modulePath(List.of(jar.getAbsolutePath())).reloaderAvailable());
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    static File compileStubReloader(File directory)
    throws Exception {
        var source = new File(directory, "Reloader.java");
        FileUtils.writeString("""
            package rife.engine;
            public class Reloader {
                public static void main(String[] args)
                throws Exception {
                    System.out.println("reloader " + String.join(" ", args));
                    var stop = new java.io.File(System.getProperty("stop"));
                    while (!stop.exists()) {
                        Thread.sleep(50);
                    }
                    System.exit(Integer.parseInt(System.getProperty("code", "0")));
                }
            }
            """, source);
        var classes = new File(directory, "classes");
        var compile_operation = new CompileOperation()
            .buildMainDirectory(classes)
            .mainSourceFiles(List.of(source));
        compile_operation.execute();
        assertTrue(compile_operation.diagnostics().isEmpty());
        return classes;
    }

    static DevOperation stubbedOperation(File classes, File stop, String code, StringBuilder output) {
        return new DevOperation()
            .javaOptions(List.of("-Dstop=" + stop.getAbsolutePath(), "-Dcode=" + code))
            .classpath(List.of(classes.getAbsolutePath()))
            .mainClass("com.example.AppSite")
            .runOptions("8080")
            .outputProcessor(s -> {
                synchronized (output) {
                    output.append(s);
                }
                return true;
            });
    }

    static void await(String reason, BooleanSupplier condition)
    throws InterruptedException {
        var deadline = System.currentTimeMillis() + 20000;
        while (!condition.getAsBoolean()) {
            assertTrue(System.currentTimeMillis() < deadline, reason);
            Thread.sleep(50);
        }
    }

    @Test
    void testExecutePassesTheExitStatusOn()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            var classes = compileStubReloader(work_directory);
            var stop = new File(work_directory, "stop");
            FileUtils.writeString("", stop);

            var output = new StringBuilder();
            stubbedOperation(classes, stop, "0", output).execute();
            assertEquals("reloader com.example.AppSite 8080", output.toString());

            assertThrows(ExitStatusException.class, () -> stubbedOperation(classes, stop, "3", new StringBuilder()).execute());
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    @Test
    void testExecuteStopsWhenTheFirstCompilationFails()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            var classes = compileStubReloader(work_directory);
            var stop = new File(work_directory, "stop");
            FileUtils.writeString("", stop);

            var output = new StringBuilder();
            var operation = stubbedOperation(classes, stop, "0", output)
                .compileOperation(() -> new CompileOperation() {
                    public void execute()
                    throws ExitStatusException {
                        throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
                    }
                });
            assertThrows(ExitStatusException.class, operation::execute);
            assertEquals("", output.toString(), "the application isn't started when its sources don't compile");
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    @Test
    void testExecuteCompilesOnceAfterSourcesChange()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        var err = System.err;
        var errors = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errors, true));
        try {
            var classes = compileStubReloader(work_directory);
            var stop = new File(work_directory, "stop");
            var sources = new File(work_directory, "src");
            sources.mkdirs();
            FileUtils.writeString("class A {}", new File(sources, "A.java"));

            var compilations = new AtomicInteger();
            var output = new StringBuilder();
            var operation = stubbedOperation(classes, stop, "0", output)
                .sourceDirectories(List.of(sources))
                .compileOperation(() -> new CompileOperation() {
                    public void execute()
                    throws ExitStatusException {
                        if (compilations.incrementAndGet() == 2) {
                            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
                        }
                    }
                });

            var failure = new AtomicReference<Throwable>();
            var runner = new Thread(() -> {
                try {
                    operation.execute();
                } catch (Throwable e) {
                    failure.set(e);
                }
            });
            runner.start();
            try {
                await("the sources compile before the application starts", () -> output.toString().startsWith("reloader"));
                assertEquals(1, compilations.get());

                FileUtils.writeString("class A { int a; }", new File(sources, "A.java"));
                Thread.sleep(100);
                FileUtils.writeString("class B {}", new File(sources, "B.java"));
                await("changed sources are compiled", () -> compilations.get() == 2);
                Thread.sleep(2000);
                assertEquals(2, compilations.get(), "a batch of changes compiles once");
                assertTrue(errors.toString().contains("The compilation failed, the application keeps running with its previous classes until the sources compile again."), errors.toString());

                new File(sources, "B.java").delete();
                await("deleted sources are compiled", () -> compilations.get() == 3);
            } finally {
                FileUtils.writeString("", stop);
                runner.join(20000);
            }
            assertFalse(runner.isAlive());
            assertNull(failure.get());
        } finally {
            System.setErr(err);
            FileUtils.deleteDirectory(work_directory);
        }
    }
}
