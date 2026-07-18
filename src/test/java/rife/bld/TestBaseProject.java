/*
 * Copyright 2026 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import rife.bld.dependencies.Scope;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestBaseProject {
    private static Method ADD_LOCAL_JARS;
    @TempDir
    Path tempDir;
    private BaseProject project;

    @BeforeAll
    static void initReflection() throws Exception {
        ADD_LOCAL_JARS = BaseProject.class.getDeclaredMethod("addLocalJars", List.class, String.class);
        ADD_LOCAL_JARS.setAccessible(true);
    }

    @BeforeEach
    void beforeEach() {
        project = new BaseProject();
        project.workDirectory = tempDir.toFile();
        project.createProjectStructure();
    }

    private void invokeAddLocalJars(List<File> jars, String path) throws Exception {
        ADD_LOCAL_JARS.invoke(project, jars, path);
    }

    @Nested
    @DisplayName("local(Path) and local(File)")
    class LocalDependencyTest {

        @Test
        @DisplayName("local(Path) relative should resolve via workDirectory")
        void localPathRelative() throws Exception {
            var jar = Files.createFile(tempDir.resolve("a.jar"));
            var dep = project.local(Path.of("a.jar"));

            assertAll(
                    () -> assertNotNull(dep, "local(Path) should not return null"),
                    () -> assertEquals("a.jar", dep.path(), "relative Path should be kept as-is")
            );

            project.dependencies().scope(Scope.compile).include(dep);
            assertTrue(project.compileClasspathJars().stream()
                            .anyMatch(f -> f.getAbsolutePath().equals(jar.toFile().getAbsolutePath())),
                    "compileClasspathJars should resolve relative path against workDirectory");
        }

        @Test
        @DisplayName("local(Path) absolute should be kept absolute")
        void localPathAbsolute() throws Exception {
            var jar = Files.createFile(tempDir.resolve("abs.jar"));
            var dep = project.local(jar);

            assertAll(
                    () -> assertNotNull(dep, "local(Path) absolute should not return null"),
                    () -> assertEquals(jar.toString(), dep.path(), "absolute Path should be kept absolute")
            );

            project.dependencies().scope(Scope.compile).include(dep);
            assertTrue(project.compileClasspathJars().stream()
                            .anyMatch(f -> f.getName().equals("abs.jar")),
                    "compileClasspathJars should include absolute Path dependency");
        }

        @Test
        @DisplayName("local(File) relative file should be kept as-is")
        void localFileRelative() {
            var dep = project.local(new File("b.jar"));

            assertAll(
                    () -> assertNotNull(dep, "local(File) should not return null"),
                    () -> assertEquals("b.jar", dep.path(), "relative File should be kept as-is")
            );
        }

        @Test
        @DisplayName("local(Path) absolute outside the project should resolve")
        void localPathAbsoluteOutsideProject(@TempDir Path otherDir) throws Exception {
            var jar = Files.createFile(otherDir.resolve("outside.jar"));
            project.dependencies().scope(Scope.compile).include(project.local(jar));

            assertTrue(project.compileClasspathJars().stream()
                            .anyMatch(f -> f.getAbsolutePath().equals(jar.toFile().getAbsolutePath())),
                    "compileClasspathJars should include an absolute path outside the project");
        }

        @Test
        @DisplayName("local(File) absolute file")
        void localFileAbsolute() throws Exception {
            var jar = Files.createFile(tempDir.resolve("c.jar"));
            var dep = project.local(jar.toFile());

            assertAll(
                    () -> assertNotNull(dep, "local(File) absolute should not return null"),
                    () -> assertEquals(jar.toFile().getAbsolutePath(), dep.path(),
                            "absolute File should be kept absolute")
            );
        }

        @Test
        @DisplayName("local(File) directory should scan jars excluding sources/javadoc")
        void localFileDirectory() throws Exception {
            var dir = Files.createDirectory(tempDir.resolve("libs-dep-filter"));
            Files.createFile(dir.resolve("one.jar"));
            Files.createFile(dir.resolve("one-sources.jar"));
            Files.createFile(dir.resolve("one-JAVADOC.jar"));

            var dep = project.local(dir.toFile());
            project.dependencies().scope(Scope.compile).include(dep);

            var cp = project.compileClasspathJars();
            assertAll(
                    () -> assertTrue(cp.stream().anyMatch(
                            f -> f.getName().equals("one.jar")), "regular jar should be included"),
                    () -> assertFalse(cp.stream().anyMatch(f -> f.getName().equals("one-sources.jar")),
                            "sources jar should be excluded (case-insensitive)"),
                    () -> assertFalse(cp.stream().anyMatch(
                                    f -> f.getName().toLowerCase().contains("javadoc")),
                            "javadoc jar should be excluded")
            );
        }

        @Test
        @DisplayName("local(Path) and local(String) should behave same")
        void parityWithStringOverload() throws Exception {
            Files.createFile(tempDir.resolve("parity.jar"));

            var fromString = project.local("parity.jar");
            var fromPath = project.local(Path.of("parity.jar"));
            var fromFile = project.local(new File("parity.jar"));

            assertAll(
                    () -> assertEquals(fromString.path(), fromPath.path(),
                            "String and Path overloads should have same path()"),
                    () -> assertEquals(fromString.path(), fromFile.path(),
                            "String and File overloads should have same path()")
            );
        }
    }

    @Nested
    @DisplayName("addLocalJars - private implementation")
    class LocalJarsTest {
        @Test
        @DisplayName("should add relative file path")
        void relativeFile() throws Exception {
            Files.createFile(tempDir.resolve("my.jar"));
            var jars = new ArrayList<File>();
            invokeAddLocalJars(jars, "my.jar");
            assertEquals(1, jars.size(), "relative file that exists should be added");
        }

        @Test
        @DisplayName("should add absolute file path")
        void absoluteFile() throws Exception {
            var jar = Files.createFile(tempDir.resolve("abs2.jar"));
            var jars = new ArrayList<File>();
            invokeAddLocalJars(jars, jar.toAbsolutePath().toString());
            assertEquals(1, jars.size(), "absolute file that exists should be added");
        }

        @Test
        @DisplayName("should resolve relative to workDirectory")
        void relativeToWorkDirectory() throws Exception {
            var sub = Files.createDirectory(tempDir.resolve("sub"));
            var jar = Files.createFile(sub.resolve("x.jar"));
            var jars = new ArrayList<File>();
            invokeAddLocalJars(jars, "sub/x.jar");
            assertEquals(jar.toFile().getAbsoluteFile(), jars.get(0).getAbsoluteFile(),
                    "should resolve against workDirectory");
        }

        @Test
        @DisplayName("should scan directory for jars only")
        void directoryScansJars() throws Exception {
            var libDir = Files.createDirectory(tempDir.resolve("libs-jars-scan"));
            Files.createFile(libDir.resolve("a.jar"));
            Files.createFile(libDir.resolve("b.jar"));
            Files.createFile(libDir.resolve("not-a-jar.txt"));
            var jars = new ArrayList<File>();
            invokeAddLocalJars(jars, "libs-jars-scan");
            assertEquals(2, jars.size(), "should only include .jar files");
        }

        @Test
        @DisplayName("should exclude -sources and -javadoc jars in directory")
        void directoryExcludesSourcesAndJavadoc() throws Exception {
            var libDir = Files.createDirectory(tempDir.resolve("libs2-filter"));
            Files.createFile(libDir.resolve("foo.jar"));
            Files.createFile(libDir.resolve("foo-sources.jar"));
            Files.createFile(libDir.resolve("foo-javadoc.jar"));
            var jars = new ArrayList<File>();
            invokeAddLocalJars(jars, "libs2-filter");
            assertAll(
                    () -> assertEquals(1, jars.size(), "should filter out -sources and -javadoc"),
                    () -> assertEquals("foo.jar", jars.get(0).getName(), "only foo.jar should remain")
            );
        }

        @Nested
        @DisplayName("integration via public classpath methods")
        class Integration {
            @Test
            @DisplayName("compileClasspathJars should include local file dependency")
            void viaCompileClasspath() throws Exception {
                var myJar = Files.createFile(tempDir.resolve("custom.jar"));
                project.dependencies().scope(Scope.compile).include(project.local("custom.jar"));
                var classpath = project.compileClasspathJars();
                assertTrue(classpath.stream().anyMatch(
                                f -> f.getAbsolutePath().equals(myJar.toFile().getAbsolutePath())),
                        "compileClasspathJars should include local file dependency");
            }
        }

        @Nested
        @DisplayName("verbose branch")
        class VerboseBranch {
            @BeforeEach
            void enableVerbose() {
                project = new BaseProject() {
                    @Override
                    public boolean verbose() {
                        return true;
                    }
                };
                project.workDirectory = tempDir.toFile();
                project.createProjectStructure();
            }

            @Test
            @DisplayName("should skip non-existent path without throwing, even verbose")
            void nonExistentPathSkipped() {
                var jars = new ArrayList<File>();
                assertDoesNotThrow(() -> invokeAddLocalJars(jars, "does-not-exist-verbose"),
                        "should not throw for missing path");
                assertTrue(jars.isEmpty(), "non-existent path should be skipped");
            }
        }
    }

    @Nested
    @DisplayName("localModule(Path) and localModule(File)")
    class LocalModuleTest {
        @Test
        @DisplayName("localModule(Path) relative")
        void localModulePathRelative() {
            var mod = project.localModule(Path.of("mod.jar"));
            assertAll(
                    () -> assertNotNull(mod, "should not return null"),
                    () -> assertEquals("mod.jar", mod.path(), "relative path kept as-is")
            );
        }

        @Test
        @DisplayName("localModule(Path) absolute")
        void localModulePathAbsolute() throws Exception {
            var jar = Files.createFile(tempDir.resolve("modAbs.jar"));
            var mod = project.localModule(jar);
            assertAll(
                    () -> assertNotNull(mod, "should not return null"),
                    () -> assertEquals(jar.toString(), mod.path(), "absolute path kept absolute")
            );
        }

        @Test
        @DisplayName("localModule(File) absolute")
        void localModuleFileAbsolute() throws Exception {
            var jar = Files.createFile(tempDir.resolve("mod2.jar"));
            var mod = project.localModule(jar.toFile());
            assertAll(
                    () -> assertNotNull(mod, "should not return null"),
                    () -> assertEquals(jar.toFile().getAbsolutePath(), mod.path(),
                            "absolute File should be kept absolute")
            );
        }

        @Test
        @DisplayName("localModule(File) directory scan")
        void localModuleFileDirectory() throws Exception {
            var dir = Files.createDirectory(tempDir.resolve("mods-filter"));
            Files.createFile(dir.resolve("m1.jar"));
            Files.createFile(dir.resolve("m1-javadoc.jar"));

            var mod = project.localModule(dir.toFile());
            project.dependencies().scope(Scope.compile).include(mod);

            var mp = project.compileModulePathJars();
            assertAll(
                    () -> assertTrue(mp.stream().anyMatch(f -> f.getName().equals("m1.jar")),
                            "regular jar should be in module path"),
                    () -> assertFalse(mp.stream().anyMatch(f -> f.getName().contains("javadoc")),
                            "javadoc jar should be excluded")
            );
        }

        @Test
        @DisplayName("all three overloads are consistent")
        void overloadsConsistent() throws Exception {
            Files.createFile(tempDir.resolve("cons.jar"));

            var s = project.localModule("cons.jar");
            var p = project.localModule(Path.of("cons.jar"));
            var f = project.localModule(new File("cons.jar"));

            assertAll(
                    () -> assertEquals(s.path(), p.path(), "String and Path overloads equal"),
                    () -> assertEquals(s.path(), f.path(), "String and File overloads equal")
            );
        }
    }
}
