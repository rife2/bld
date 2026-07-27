/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.Dependency;
import rife.bld.dependencies.Repository;
import rife.bld.dependencies.VersionNumber;
import rife.bld.operations.DependencyTreeOperation;
import rife.bld.operations.DownloadOperation;
import rife.bld.wrapper.WrapperExtensionResolver;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.TestClasspathJars.*;
import static rife.bld.dependencies.Scope.provided;

/**
 * Real world lifecycles of the caching in {@code bld.cache}: real jars
 * are served, downloaded by the real operations, tools are launched in
 * their own JVM, and consecutive builds are simulated with fresh project
 * instances, asserting with a request counter that the repeats resolve
 * and transfer nothing.
 */
public class TestBldCacheLifecycle {
    static class LifecycleProject extends Project {
        LifecycleProject(File tmp, Repository repository) {
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "lifecycle_project";
            version = new VersionNumber(0, 0, 1);
            repositories = List.of(repository);
        }

        void enableAutoDownloadPurge() {
            autoDownloadPurge = true;
        }
    }

    private record ToolJars(byte[] tool, byte[] liba) {
    }

    private static ToolJars buildToolJars(File tmp)
    throws Exception {
        var src = new File(tmp, "src");
        src.mkdirs();
        var liba_src = new File(src, "LibA.java");
        FileUtils.writeString("""
            package com.example.liba;
            public class LibA {
                public static String marker() { return "liba-ok"; }
            }""", liba_src);
        var tool_src = new File(src, "ToolMain.java");
        FileUtils.writeString("""
            package com.example.tool;
            import com.example.liba.LibA;
            public class ToolMain {
                public static void main(String[] args) {
                    System.out.println("marker=" + LibA.marker());
                }
            }""", tool_src);

        var liba_classes = compile(new File(tmp, "classes_liba"), null, liba_src);
        var tool_classes = compile(new File(tmp, "classes_tool"), liba_classes.getAbsolutePath(), tool_src);
        var liba_jar = new File(tmp, "liba.jar");
        var tool_jar = new File(tmp, "tool.jar");
        createJar(liba_classes, liba_jar);
        createJar(tool_classes, tool_jar);
        return new ToolJars(FileUtils.readBytes(tool_jar), FileUtils.readBytes(liba_jar));
    }

    @Test
    void testDependencyClasspathLifecycle() throws Exception {
        var requests = new AtomicInteger();
        var scratch = Files.createTempDirectory("cachelifecycle").toFile();
        try {
            var jars = buildToolJars(scratch);
            var server = createArtifactServer(Map.of(
                    "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
                    "liba:1.1.0", pom("liba", "1.1.0", "")),
                Map.of("tool:1.0.0", jars.tool(), "liba:1.1.0", jars.liba()),
                requests);
            server.start();
            try {
                var tmp = new File(scratch, "project");
                tmp.mkdirs();

                // build one: download the real jars, look up the tool
                // classpath, and launch the tool with it
                var build1 = new LifecycleProject(tmp, serverRepository(server));
                build1.dependencies().scope(provided)
                    .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
                new DownloadOperation().fromProject(build1).execute();
                assertTrue(new File(build1.libProvidedDirectory(), "tool-1.0.0.jar").exists());
                assertTrue(new File(build1.libProvidedDirectory(), "liba-1.1.0.jar").exists());

                var operation1 = new TestClasspathJars.ToolOperation().fromProject(build1);
                operation1.execute();
                assertTrue(operation1.output_.contains("marker=liba-ok"), operation1.output_.toString());
                var settled = settledRequests(requests);

                // build two: a fresh project answers the classpath from the
                // cache without a single request, and the tool still runs
                var build2 = new LifecycleProject(tmp, serverRepository(server));
                build2.dependencies().scope(provided)
                    .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
                var operation2 = new TestClasspathJars.ToolOperation().fromProject(build2);
                operation2.execute();
                assertTrue(operation2.output_.contains("marker=liba-ok"), operation2.output_.toString());
                assertEquals(settled, requests.get());
            } finally {
                server.stop(0);
            }
        } finally {
            FileUtils.deleteDirectory(scratch);
        }
    }

    @Test
    void testExtensionClasspathLifecycle() throws Exception {
        var requests = new AtomicInteger();
        var scratch = Files.createTempDirectory("cachelifecycle").toFile();
        try {
            var jars = buildToolJars(scratch);
            var server = createArtifactServer(Map.of(
                    "ext:1.0.0", pom("ext", "1.0.0", dependency("tool", "1.0.0")),
                    "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
                    "liba:1.1.0", pom("liba", "1.1.0", "")),
                Map.of("tool:1.0.0", jars.tool(), "liba:1.1.0", jars.liba()),
                requests);
            server.start();
            try {
                var tmp = new File(scratch, "project");
                tmp.mkdirs();
                var repository = "http://localhost:" + server.getAddress().getPort() + "/";

                // build one: the wrapper resolves the extensions with the
                // real jars, an extension launches its tool with the
                // isolated classpath out of lib/bld
                var build1 = new LifecycleProject(tmp, serverRepository(server));
                writeWrapperProperties(build1, server, "com.example:ext:1.0.0");
                new WrapperExtensionResolver(tmp, build1.libBldDirectory(),
                    new Properties(), new Properties(),
                    List.of(repository), List.of("com.example:ext:1.0.0"),
                    false, false).updateExtensions();

                var launch1 = launchExtensionTool(build1);
                assertTrue(launch1.contains("marker=liba-ok"), launch1);
                var settled = settledRequests(requests);

                // build two: the wrapper transfers nothing, the classpath
                // answers from the cache, and the tool still runs
                new WrapperExtensionResolver(tmp, build1.libBldDirectory(),
                    new Properties(), new Properties(),
                    List.of(repository), List.of("com.example:ext:1.0.0"),
                    false, false).updateExtensions();
                var build2 = new LifecycleProject(tmp, serverRepository(server));
                var launch2 = launchExtensionTool(build2);
                assertTrue(launch2.contains("marker=liba-ok"), launch2);
                assertEquals(settled, requests.get());
            } finally {
                server.stop(0);
            }
        } finally {
            FileUtils.deleteDirectory(scratch);
        }
    }

    private static String launchExtensionTool(BaseProject project)
    throws Exception {
        var classpath = project.extensionClasspathJars("com.example", "tool").stream()
            .map(File::getAbsolutePath)
            .reduce((left, right) -> left + File.pathSeparator + right).orElseThrow();
        var process = new ProcessBuilder("java", "-cp", classpath, "com.example.tool.ToolMain")
            .redirectErrorStream(true)
            .start();
        var output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
        return output;
    }

    @Test
    void testAutoDownloadPurgeLifecycle() throws Exception {
        var requests = new AtomicInteger();
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", ""),
            "tool:2.0.0", pom("tool", "2.0.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("cachelifecycle").toFile();
        try {
            // build one: the automatic download resolves and transfers
            var build1 = new LifecycleProject(tmp, serverRepository(server));
            build1.enableAutoDownloadPurge();
            build1.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            build1.performAutoDownloadPurgeIfEnabled();
            assertTrue(new File(build1.libProvidedDirectory(), "tool-1.0.0.jar").exists());
            var settled = settledRequests(requests);

            // build two: the hash is unchanged, nothing is resolved
            var build2 = new LifecycleProject(tmp, serverRepository(server));
            build2.enableAutoDownloadPurge();
            build2.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            build2.performAutoDownloadPurgeIfEnabled();
            assertEquals(settled, requests.get());

            // build three: a changed declaration downloads the new jar and
            // purges the old one
            var build3 = new LifecycleProject(tmp, serverRepository(server));
            build3.enableAutoDownloadPurge();
            build3.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(2, 0, 0)));
            build3.performAutoDownloadPurgeIfEnabled();
            assertTrue(requests.get() > settled);
            assertTrue(new File(build3.libProvidedDirectory(), "tool-2.0.0.jar").exists());
            assertFalse(new File(build3.libProvidedDirectory(), "tool-1.0.0.jar").exists());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyTreeLifecycle() throws Exception {
        var requests = new AtomicInteger();
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("cachelifecycle").toFile();
        try {
            // build one: the tree is generated through resolution
            var build1 = new LifecycleProject(tmp, serverRepository(server));
            writeWrapperProperties(build1, server, "");
            build1.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            var operation1 = new DependencyTreeOperation().fromProject(build1);
            operation1.executeOnce();
            assertTrue(operation1.dependencyTree().contains("com.example:tool:1.0.0"));
            var settled = settledRequests(requests);

            // build two: the tree comes out of the cache without resolving
            var build2 = new LifecycleProject(tmp, serverRepository(server));
            build2.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            var operation2 = new DependencyTreeOperation().fromProject(build2);
            operation2.executeOnce();
            assertEquals(operation1.dependencyTree(), operation2.dependencyTree());
            assertEquals(settled, requests.get());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }
}
