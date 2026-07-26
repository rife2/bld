/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import rife.bld.dependencies.*;
import rife.bld.operations.AbstractProcessOperation;
import rife.tools.FileUtils;

import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.Scope.*;

public class TestClasspathJars {
    static class JarsProject extends Project {
        JarsProject(File tmp, Repository repository) {
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "jars_project";
            version = new VersionNumber(0, 0, 1);
            repositories = List.of(repository);
        }
    }

    @Test
    void testDependencyClasspathJarsTransitives() throws Exception {
        // only the tool and its own transitive tree is returned, the
        // other dependencies that share the scope directory are not
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", dependency("libb", "2.0.0")),
            "libb:2.0.0", pom("libb", "2.0.0", ""),
            "other:3.0.0", pom("other", "3.0.0", dependency("othersub", "1.0.0")),
            "othersub:1.0.0", pom("othersub", "1.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "other", new VersionNumber(3, 0, 0)));

            var jars = project.dependencyClasspathJars(provided, "com.example", "tool");
            assertEquals(List.of("tool-1.0.0.jar", "liba-1.1.0.jar", "libb-2.0.0.jar"),
                jars.stream().map(File::getName).toList());
            for (var jar : jars) {
                assertEquals(project.libProvidedDirectory(), jar.getParentFile());
            }
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyClasspathJarsScopeDirectories() throws Exception {
        // the files are mapped into the lib directory of the scope the
        // dependency is declared in
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            for (var scope : List.of(compile, provided, runtime, standalone, test)) {
                project.dependencies().scope(scope)
                    .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            }

            assertEquals(project.libCompileDirectory(), project.dependencyClasspathJars(compile, "com.example", "tool").get(0).getParentFile());
            assertEquals(project.libProvidedDirectory(), project.dependencyClasspathJars(provided, "com.example", "tool").get(0).getParentFile());
            assertEquals(project.libRuntimeDirectory(), project.dependencyClasspathJars(runtime, "com.example", "tool").get(0).getParentFile());
            assertEquals(project.libStandaloneDirectory(), project.dependencyClasspathJars(standalone, "com.example", "tool").get(0).getParentFile());
            assertEquals(project.libTestDirectory(), project.dependencyClasspathJars(test, "com.example", "tool").get(0).getParentFile());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyClasspathJarsUsesDeclaredVersion() throws Exception {
        // the version comes from the scope declaration, a newer version
        // in the repository is not consulted
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", ""),
            "tool:9.9.9", pom("tool", "9.9.9", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));

            var jars = project.dependencyClasspathJars(test, "com.example", "tool");
            assertEquals(List.of("tool-1.0.0.jar"), jars.stream().map(File::getName).toList());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyClasspathJarsAppliesBoms() throws Exception {
        // a BOM that is effective for the scope pins the version of a
        // transitive dependency, matching the jar the download wrote
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", dependency("libb", "1.5.0")),
            "tool:1.0.0", pom("tool", "1.0.0", dependency("libb", "2.0.0")),
            "libb:1.5.0", pom("libb", "1.5.0", ""),
            "libb:2.0.0", pom("libb", "2.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(test)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));

            var jars = project.dependencyClasspathJars(test, "com.example", "tool");
            assertEquals(List.of("tool-1.0.0.jar", "libb-1.5.0.jar"),
                jars.stream().map(File::getName).toList());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyClasspathJarsUndeclared() throws Exception {
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, new Repository("http://localhost:1/"));
            project.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));

            var exception = assertThrows(IllegalArgumentException.class,
                () -> project.dependencyClasspathJars(test, "com.example", "unknown"));
            assertTrue(exception.getMessage().contains("isn't declared in the test scope"));

            // declared in another scope doesn't count
            var exception2 = assertThrows(IllegalArgumentException.class,
                () -> project.dependencyClasspathJars(compile, "com.example", "tool"));
            assertTrue(exception2.getMessage().contains("isn't declared in the compile scope"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testVersionResolutionAppliesBoms() throws Exception {
        // the version resolution accessor produces the same resolution
        // the build uses for a scope, including the BOMs effective for it
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", dependency("liba", "1.4.0"))));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(test)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "liba"));

            var resolution = project.versionResolution(test);
            assertEquals(new VersionNumber(1, 4, 0), resolution.overrideVersion(new Dependency("com.example", "liba")));

            // the compile scope doesn't declare the BOM, nothing is managed there
            assertEquals(VersionNumber.UNKNOWN, project.versionResolution(compile).overrideVersion(new Dependency("com.example", "liba")));
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExtensionClasspathJarsTransitiveTool() throws Exception {
        // the tool isn't declared as an extension itself, it's brought in
        // transitively by one, its version comes from the resolved set
        var server = createArtifactServer(Map.of(
            "ext:1.0.0", pom("ext", "1.0.0", dependency("tool", "1.0.0")),
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", ""),
            "ext2:1.0.0", pom("ext2", "1.0.0", dependency("unrelated", "2.0.0")),
            "unrelated:2.0.0", pom("unrelated", "2.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            writeWrapperProperties(project, server, "com.example:ext:1.0.0,com.example:ext2:1.0.0");

            var jars = project.extensionClasspathJars("com.example", "tool");
            assertEquals(List.of("tool-1.0.0.jar", "liba-1.1.0.jar"),
                jars.stream().map(File::getName).toList());
            for (var jar : jars) {
                assertEquals(project.libBldDirectory(), jar.getParentFile());
            }
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExtensionClasspathJarsDirectExtension() throws Exception {
        // a dependency that is declared as an extension itself is found too
        var server = createArtifactServer(Map.of(
            "tool:2.0.0", pom("tool", "2.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            writeWrapperProperties(project, server, "com.example:tool:2.0.0");

            var jars = project.extensionClasspathJars("com.example", "tool");
            assertEquals(List.of("tool-2.0.0.jar", "liba-1.1.0.jar"),
                jars.stream().map(File::getName).toList());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExtensionClasspathJarsConflictingVersions() throws Exception {
        // two extensions pull different versions of a shared dependency,
        // the returned jar matches the version that resolution picked and
        // that is actually on disk
        var server = createArtifactServer(Map.of(
            "ext:1.0.0", pom("ext", "1.0.0", dependency("shared", "1.0.0")),
            "ext2:1.0.0", pom("ext2", "1.0.0", dependency("shared", "2.0.0")),
            "shared:1.0.0", pom("shared", "1.0.0", ""),
            "shared:2.0.0", pom("shared", "2.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));

            // dependency resolution keeps the highest version of a
            // conflicting dependency, independent of declaration order
            writeWrapperProperties(project, server, "com.example:ext:1.0.0,com.example:ext2:1.0.0");
            var jars = project.extensionClasspathJars("com.example", "shared");
            assertEquals(List.of("shared-2.0.0.jar"), jars.stream().map(File::getName).toList());

            writeWrapperProperties(project, server, "com.example:ext2:1.0.0,com.example:ext:1.0.0");
            var jars_reversed = project.extensionClasspathJars("com.example", "shared");
            assertEquals(List.of("shared-2.0.0.jar"), jars_reversed.stream().map(File::getName).toList());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExtensionClasspathJarsUnknown() throws Exception {
        var server = createArtifactServer(Map.of(
            "ext:1.0.0", pom("ext", "1.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            writeWrapperProperties(project, server, "com.example:ext:1.0.0");

            var exception = assertThrows(IllegalArgumentException.class,
                () -> project.extensionClasspathJars("com.example", "unknown"));
            assertTrue(exception.getMessage().contains("isn't part of the extensions"));
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class ToolOperation extends AbstractProcessOperation<ToolOperation> {
        BaseProject project_;
        final List<String> output_ = new ArrayList<>();

        ToolOperation() {
            outputProcessor(line -> {
                output_.add(line);
                return true;
            });
        }

        @Override
        protected List<String> executeConstructProcessCommandList() {
            final List<String> args = new ArrayList<>();
            args.add("java");
            args.add("-cp");
            args.add(project_.dependencyClasspathJars(provided, "com.example", "tool").stream()
                .map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
            args.add("com.example.tool.ToolMain");
            return args;
        }

        @Override
        public ToolOperation fromProject(BaseProject project) {
            project_ = project;
            return this;
        }
    }

    @Test
    void testLaunchToolWithIsolatedClasspath() throws Exception {
        // Erik's use case end to end: an extension operation launches an
        // external tool in its own JVM with a classpath of just the tool
        // and its transitive dependencies, while another dependency that
        // shares the same lib directory stays off the classpath
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", ""),
            "other:3.0.0", pom("other", "3.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            // build real jars: the tool needs its transitive dependency to
            // run and probes whether the decoy leaked onto its classpath
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
                        try {
                            Class.forName("com.example.decoy.Decoy");
                            System.out.println("decoy=present");
                        } catch (ClassNotFoundException e) {
                            System.out.println("decoy=absent");
                        }
                    }
                }""", tool_src);
            var decoy_src = new File(src, "Decoy.java");
            FileUtils.writeString("""
                package com.example.decoy;
                public class Decoy {
                }""", decoy_src);

            var liba_classes = compile(new File(tmp, "classes_liba"), null, liba_src);
            var tool_classes = compile(new File(tmp, "classes_tool"), liba_classes.getAbsolutePath(), tool_src);
            var decoy_classes = compile(new File(tmp, "classes_decoy"), null, decoy_src);

            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "other", new VersionNumber(3, 0, 0)));

            // put the jars where a download would have written them
            var lib_provided = project.libProvidedDirectory();
            lib_provided.mkdirs();
            createJar(tool_classes, new File(lib_provided, "tool-1.0.0.jar"));
            createJar(liba_classes, new File(lib_provided, "liba-1.1.0.jar"));
            createJar(decoy_classes, new File(lib_provided, "other-3.0.0.jar"));

            var operation = new ToolOperation().fromProject(project);
            operation.execute();

            // the transitive dependency was on the classpath, the decoy wasn't
            assertTrue(operation.output_.contains("marker=liba-ok"), operation.output_.toString());
            assertTrue(operation.output_.contains("decoy=absent"), operation.output_.toString());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    private static File compile(File outDir, String classpath, File source) {
        outDir.mkdirs();
        var args = new ArrayList<>(List.of("-d", outDir.getAbsolutePath()));
        if (classpath != null) {
            args.add("-cp");
            args.add(classpath);
        }
        args.add(source.getAbsolutePath());
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        return outDir;
    }

    private static void createJar(File classesDir, File jarFile) throws IOException {
        try (var jar = new JarOutputStream(new FileOutputStream(jarFile))) {
            var base = classesDir.toPath();
            try (var walk = Files.walk(base)) {
                for (var path : walk.filter(Files::isRegularFile).toList()) {
                    jar.putNextEntry(new JarEntry(base.relativize(path).toString().replace(File.separatorChar, '/')));
                    jar.write(Files.readAllBytes(path));
                    jar.closeEntry();
                }
            }
        }
    }

    private static void writeWrapperProperties(Project project, HttpServer server, String extensions)
    throws IOException {
        var lib_bld = project.libBldDirectory();
        lib_bld.mkdirs();
        var properties = """
            bld.downloadExtensionJavadoc=false
            bld.downloadExtensionSources=false
            bld.extensions=%s
            bld.repositories=http://localhost:%d/
            bld.version=%s
            """.formatted(extensions, server.getAddress().getPort(), BldVersion.getVersion());
        FileUtils.writeString(properties, new File(lib_bld, "bld-wrapper.properties"));
    }


    @Test
    void testDependencyClasspathJarsCaching() throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", ""),
            "tool:2.0.0", pom("tool", "2.0.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));

            var jars = project.dependencyClasspathJars(provided, "com.example", "tool");
            var resolution_requests = settledRequests(requests);
            assertTrue(resolution_requests > 0);

            // a repeated call in the same build resolves nothing
            assertEquals(jars, project.dependencyClasspathJars(provided, "com.example", "tool"));
            assertEquals(resolution_requests, requests.get());

            // a fresh project instance reads the persisted cache instead
            // of resolving, like a new build invocation would
            var fresh = new JarsProject(tmp, serverRepository(server));
            fresh.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            assertEquals(jars, fresh.dependencyClasspathJars(provided, "com.example", "tool"));
            assertEquals(resolution_requests, requests.get());

            // changing the declared version invalidates the cache
            var changed = new JarsProject(tmp, serverRepository(server));
            changed.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(2, 0, 0)));
            assertEquals(List.of("tool-2.0.0.jar"),
                changed.dependencyClasspathJars(provided, "com.example", "tool").stream().map(File::getName).toList());
            assertTrue(requests.get() > resolution_requests);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExtensionClasspathJarsCaching() throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = createArtifactServer(Map.of(
            "ext:1.0.0", pom("ext", "1.0.0", dependency("tool", "1.0.0")),
            "tool:1.0.0", pom("tool", "1.0.0", dependency("liba", "1.1.0")),
            "liba:1.1.0", pom("liba", "1.1.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            writeWrapperProperties(project, server, "com.example:ext:1.0.0");

            var jars = project.extensionClasspathJars("com.example", "tool");
            var resolution_requests = settledRequests(requests);
            assertTrue(resolution_requests > 0);

            // repeated lookups in the same build resolve nothing, also
            // for another dependency out of the same extension universe
            assertEquals(jars, project.extensionClasspathJars("com.example", "tool"));
            assertEquals(resolution_requests, requests.get());
            var liba_jars = project.extensionClasspathJars("com.example", "liba");
            assertEquals(List.of("liba-1.1.0.jar"), liba_jars.stream().map(File::getName).toList());
            var universe_requests = settledRequests(requests);
            assertEquals(resolution_requests, universe_requests);

            // a fresh project instance reads the persisted cache
            var fresh = new JarsProject(tmp, serverRepository(server));
            assertEquals(jars, fresh.extensionClasspathJars("com.example", "tool"));
            assertEquals(universe_requests, requests.get());

            // changing the declared extensions invalidates the cache
            writeWrapperProperties(project, server, "com.example:tool:1.0.0");
            var changed = new JarsProject(tmp, serverRepository(server));
            assertEquals(List.of("tool-1.0.0.jar", "liba-1.1.0.jar"),
                changed.extensionClasspathJars("com.example", "tool").stream().map(File::getName).toList());
            assertTrue(requests.get() > universe_requests);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }


    @Test
    void testDependencyClasspathJarsBomChangeInvalidates() throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", dependency("libb", "1.5.0")),
            "bom1:2.0.0", bomPom("bom1", "2.0.0", dependency("libb", "2.0.0")),
            "tool:1.0.0", pom("tool", "1.0.0", dependency("libb", "3.0.0")),
            "libb:1.5.0", pom("libb", "1.5.0", ""),
            "libb:2.0.0", pom("libb", "2.0.0", ""),
            "libb:3.0.0", pom("libb", "3.0.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(test)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            assertEquals(List.of("tool-1.0.0.jar", "libb-1.5.0.jar"),
                project.dependencyClasspathJars(test, "com.example", "tool").stream().map(File::getName).toList());
            var warm_requests = requests.get();

            // a different BOM version changes the hash and the pinned jar
            var changed = new JarsProject(tmp, serverRepository(server));
            changed.dependencies().scope(test)
                .include(new Bom("com.example", "bom1", new VersionNumber(2, 0, 0)))
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            assertEquals(List.of("tool-1.0.0.jar", "libb-2.0.0.jar"),
                changed.dependencyClasspathJars(test, "com.example", "tool").stream().map(File::getName).toList());
            assertTrue(requests.get() > warm_requests);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyClasspathJarsOverrideChangeInvalidates() throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", dependency("libb", "2.0.0")),
            "libb:2.0.0", pom("libb", "2.0.0", ""),
            "libb:2.5.0", pom("libb", "2.5.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            assertEquals(List.of("tool-1.0.0.jar", "libb-2.0.0.jar"),
                project.dependencyClasspathJars(test, "com.example", "tool").stream().map(File::getName).toList());
            var warm_requests = requests.get();

            // a version override changes the hash and the resolved jar
            var overridden = new JarsProject(tmp, serverRepository(server));
            overridden.properties().put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:libb:2.5.0");
            overridden.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            assertEquals(List.of("tool-1.0.0.jar", "libb-2.5.0.jar"),
                overridden.dependencyClasspathJars(test, "com.example", "tool").stream().map(File::getName).toList());
            assertTrue(requests.get() > warm_requests);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyClasspathJarsRepositoryChangeInvalidates() throws Exception {
        var requests1 = new java.util.concurrent.atomic.AtomicInteger();
        var requests2 = new java.util.concurrent.atomic.AtomicInteger();
        var poms = Map.of("tool:1.0.0", pom("tool", "1.0.0", ""));
        var server1 = createArtifactServer(poms, requests1);
        var server2 = createArtifactServer(poms, requests2);
        server1.start();
        server2.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server1));
            project.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            project.dependencyClasspathJars(test, "com.example", "tool");
            assertTrue(requests1.get() > 0);

            // a different repository list changes the hash, the second
            // server sees the resolution instead of the cache answering
            var moved = new JarsProject(tmp, serverRepository(server2));
            moved.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            moved.dependencyClasspathJars(test, "com.example", "tool");
            assertTrue(requests2.get() > 0);
        } finally {
            server1.stop(0);
            server2.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testDependencyClasspathJarsScopeKeysIsolated() throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", ""),
            "tool:2.0.0", pom("tool", "2.0.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            project.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(2, 0, 0)));

            // the same coordinate is cached per scope, with its own version
            assertEquals(List.of("tool-1.0.0.jar"),
                project.dependencyClasspathJars(provided, "com.example", "tool").stream().map(File::getName).toList());
            assertEquals(List.of("tool-2.0.0.jar"),
                project.dependencyClasspathJars(test, "com.example", "tool").stream().map(File::getName).toList());
            var warm_requests = settledRequests(requests);

            var fresh = new JarsProject(tmp, serverRepository(server));
            fresh.dependencies().scope(provided)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            fresh.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(2, 0, 0)));
            assertEquals(List.of("tool-1.0.0.jar"),
                fresh.dependencyClasspathJars(provided, "com.example", "tool").stream().map(File::getName).toList());
            assertEquals(List.of("tool-2.0.0.jar"),
                fresh.dependencyClasspathJars(test, "com.example", "tool").stream().map(File::getName).toList());
            assertEquals(warm_requests, requests.get());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testClasspathCacheCoexistsWithDependencyTrees() throws Exception {
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = createArtifactServer(Map.of(
            "tool:1.0.0", pom("tool", "1.0.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            project.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            project.dependencyClasspathJars(test, "com.example", "tool");
            var warm_requests = settledRequests(requests);

            // another BldCache user writing to the same file with the same
            // hash preserves the classpath entries
            var cache = new BldCache(project.libBldDirectory(), new VersionResolution(project.properties()));
            cache.cacheDependenciesHash(project.repositories(), project.dependencies());
            assertTrue(cache.isDependenciesHashValid());
            cache.cacheDependenciesTestDependencyTree("tree placeholder");
            cache.writeCache();

            assertEquals("tree placeholder", cache.getCachedDependenciesTestDependencyTree());
            var fresh = new JarsProject(tmp, serverRepository(server));
            fresh.dependencies().scope(test)
                .include(new Dependency("com.example", "tool", new VersionNumber(1, 0, 0)));
            fresh.dependencyClasspathJars(test, "com.example", "tool");
            assertEquals(warm_requests, requests.get());

            // and a classpath write with the same hash preserves the tree
            fresh.dependencyClasspathJars(test, "com.example", "tool");
            assertEquals("tree placeholder", cache.getCachedDependenciesTestDependencyTree());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExtensionClasspathJarsUndeclaredAfterCached() throws Exception {
        var server = createArtifactServer(Map.of(
            "ext:1.0.0", pom("ext", "1.0.0", dependency("tool", "1.0.0")),
            "tool:1.0.0", pom("tool", "1.0.0", ""),
            "other:1.0.0", pom("other", "1.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            writeWrapperProperties(project, server, "com.example:ext:1.0.0");
            assertEquals(1, project.extensionClasspathJars("com.example", "tool").size());

            // the extension that brought the tool in is replaced, a stale
            // cache entry must not keep answering for it
            writeWrapperProperties(project, server, "com.example:other:1.0.0");
            var changed = new JarsProject(tmp, serverRepository(server));
            var exception = assertThrows(IllegalArgumentException.class,
                () -> changed.extensionClasspathJars("com.example", "tool"));
            assertTrue(exception.getMessage().contains("isn't part of the extensions"));
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }


    @Test
    void testExtensionClasspathJarsCoexistsWithWrapperResolver() throws Exception {
        // the scenario of an extension calling extensionClasspathJars in
        // every build: the wrapper resolves the extensions first, the
        // classpath lookup must not invalidate the wrapper's cache or the
        // next build downloads all the extension artifacts again
        var requests = new java.util.concurrent.atomic.AtomicInteger();
        var server = createArtifactServer(Map.of(
            "ext:1.0.0", pom("ext", "1.0.0", dependency("tool", "1.0.0")),
            "tool:1.0.0", pom("tool", "1.0.0", "")), requests);
        server.start();
        var tmp = Files.createTempDirectory("classpathjars").toFile();
        try {
            var project = new JarsProject(tmp, serverRepository(server));
            writeWrapperProperties(project, server, "com.example:ext:1.0.0");
            var repository = "http://localhost:" + server.getAddress().getPort() + "/";

            // first build: the wrapper resolves and downloads the extensions
            new rife.bld.wrapper.WrapperExtensionResolver(tmp, project.libBldDirectory(),
                new java.util.Properties(), new java.util.Properties(),
                List.of(repository), List.of("com.example:ext:1.0.0"),
                false, false).updateExtensions();
            assertTrue(new File(project.libBldDirectory(), "ext-1.0.0.jar").exists());
            assertTrue(new File(project.libBldDirectory(), "tool-1.0.0.jar").exists());

            // the extension asks for its tool classpath during the build
            assertEquals(List.of("tool-1.0.0.jar"),
                project.extensionClasspathJars("com.example", "tool").stream().map(File::getName).toList());
            var settled = settledRequests(requests);

            // second build: the wrapper has to consider its cache valid
            // and transfer nothing
            new rife.bld.wrapper.WrapperExtensionResolver(tmp, project.libBldDirectory(),
                new java.util.Properties(), new java.util.Properties(),
                List.of(repository), List.of("com.example:ext:1.0.0"),
                false, false).updateExtensions();
            assertEquals(settled, requests.get());

            // and the classpath lookup of the second build answers from
            // the cache without resolving
            var second_build = new JarsProject(tmp, serverRepository(server));
            assertEquals(List.of("tool-1.0.0.jar"),
                second_build.extensionClasspathJars("com.example", "tool").stream().map(File::getName).toList());
            assertEquals(settled, requests.get());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    // the POM prefetcher can still have a speculative request in flight
    // when resolution returns, count requests only after they settle
    private static int settledRequests(java.util.concurrent.atomic.AtomicInteger requests)
    throws InterruptedException {
        var last = requests.get();
        var stable_since = System.currentTimeMillis();
        while (System.currentTimeMillis() - stable_since < 300) {
            Thread.sleep(50);
            var current = requests.get();
            if (current != last) {
                last = current;
                stable_since = System.currentTimeMillis();
            }
        }
        return last;
    }

    private static Repository serverRepository(HttpServer server) {
        return new Repository("http://localhost:" + server.getAddress().getPort() + "/");
    }

    private static HttpServer createArtifactServer(Map<String, String> poms)
    throws IOException {
        return createArtifactServer(poms, new java.util.concurrent.atomic.AtomicInteger());
    }

    private static HttpServer createArtifactServer(Map<String, String> poms, java.util.concurrent.atomic.AtomicInteger requests)
    throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            byte[] body = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                var content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
                if (content != null) {
                    body = content.getBytes();
                }
            } else if (filename.endsWith(".jar") && segments.length >= 3) {
                if (poms.containsKey(segments[segments.length - 3] + ":" + segments[segments.length - 2])) {
                    body = "jar".getBytes();
                }
            }
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    private static String dependency(String artifact, String version) {
        return "<dependency><groupId>com.example</groupId><artifactId>" + artifact + "</artifactId><version>" + version + "</version></dependency>";
    }

    private static String pom(String artifact, String version, String dependencies) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <dependencies>%s</dependencies>
            </project>""".formatted(artifact, version, dependencies);
    }

    private static String bomPom(String artifact, String version, String managedDependencies) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                    <dependencies>%s</dependencies>
                </dependencyManagement>
            </project>""".formatted(artifact, version, managedDependencies);
    }
}
