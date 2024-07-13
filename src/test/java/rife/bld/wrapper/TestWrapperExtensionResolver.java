/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.wrapper;

import org.junit.jupiter.api.Test;
import rife.bld.BldVersion;
import rife.bld.dependencies.VersionResolution;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.wrapper.Wrapper.MAVEN_CENTRAL;

public class TestWrapperExtensionResolver {
    @Test
    void testNoExtensions()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), Collections.emptySet(), Collections.emptySet(), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(2, files2.size());
            Collections.sort(files2);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateExtensions()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2,
                new Properties(), new Properties(),
                List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(9, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateExtensionsOverride1()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var properties = new Properties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "org.antlr:antlr4:4.11.0");
            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2,
                properties, new Properties(),
                List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(9, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.0.jar
                antlr4-runtime-4.11.0.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateExtensionsOverride2()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var properties = new Properties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "org.glassfish:javax.json:1.1.3");
            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2,
                properties, new Properties(),
                List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(10, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.3.jar
                javax.json-api-1.1.3.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateExtensionsSources()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), true, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(16, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4-sources.jar
                ST4-4.3.4.jar
                antlr-runtime-3.5.3-sources.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1-sources.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1-sources.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1-sources.jar
                icu4j-71.1.jar
                javax.json-1.1.4-sources.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3-sources.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateExtensionsJavadoc()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, true);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(16, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4-javadoc.jar
                ST4-4.3.4.jar
                antlr-runtime-3.5.3-javadoc.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1-javadoc.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1-javadoc.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1-javadoc.jar
                icu4j-71.1.jar
                javax.json-1.1.4-javadoc.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3-javadoc.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateExtensionsBoth()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), true, true);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(23, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4-javadoc.jar
                ST4-4.3.4-sources.jar
                ST4-4.3.4.jar
                antlr-runtime-3.5.3-javadoc.jar
                antlr-runtime-3.5.3-sources.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1-javadoc.jar
                antlr4-4.11.1-sources.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1-javadoc.jar
                antlr4-runtime-4.11.1-sources.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1-javadoc.jar
                icu4j-71.1-sources.jar
                icu4j-71.1.jar
                javax.json-1.1.4-javadoc.jar
                javax.json-1.1.4-sources.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3-javadoc.jar
                org.abego.treelayout.core-1.0.3-sources.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateExtensionsBothOverride()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var properties = new Properties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "org.antlr:antlr4:4.11.0");
            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2,
                properties, new Properties(),
                List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), true, true);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(23, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4-javadoc.jar
                ST4-4.3.4-sources.jar
                ST4-4.3.4.jar
                antlr-runtime-3.5.3-javadoc.jar
                antlr-runtime-3.5.3-sources.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.0-javadoc.jar
                antlr4-4.11.0-sources.jar
                antlr4-4.11.0.jar
                antlr4-runtime-4.11.0-javadoc.jar
                antlr4-runtime-4.11.0-sources.jar
                antlr4-runtime-4.11.0.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1-javadoc.jar
                icu4j-71.1-sources.jar
                icu4j-71.1.jar
                javax.json-1.1.4-javadoc.jar
                javax.json-1.1.4-sources.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3-javadoc.jar
                org.abego.treelayout.core-1.0.3-sources.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testResolvedRepository()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var properties = new File(tmp1, "local.properties");
            FileUtils.writeString("bld.repo.testrepo=" + MAVEN_CENTRAL, properties);

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of("testrepo"), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(9, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testCheckHash()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files = tmp2.listFiles();
            assertEquals(9, files.length);
            Arrays.stream(files).forEach(file -> {
                if (!file.getName().startsWith(Wrapper.WRAPPER_PREFIX)) {
                    file.delete();
                }
            });
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(2, files2.size());
            Collections.sort(files2);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files2));

            resolver.updateExtensions();
            var files3 = FileUtils.getFileList(tmp2);
            assertEquals(2, files3.size());
            Collections.sort(files3);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files3));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testDeleteHash()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files = tmp2.listFiles();
            assertEquals(9, files.length);
            Arrays.stream(files).forEach(file -> {
                if (!file.getName().startsWith(Wrapper.WRAPPER_PREFIX)) {
                    file.delete();
                }
            });
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(2, files2.size());
            Collections.sort(files2);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files2));

            resolver.updateExtensions();
            var files3 = FileUtils.getFileList(tmp2);
            assertEquals(2, files3.size());
            Collections.sort(files3);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files3));
            hash_file.delete();

            resolver.updateExtensions();
            var files4 = FileUtils.getFileList(tmp2);
            assertEquals(9, files4.size());
            Collections.sort(files4);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files4));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testUpdateHash()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver.updateExtensions();

            assertTrue(hash_file.exists());
            var files = tmp2.listFiles();
            assertEquals(9, files.length);
            Arrays.stream(files).forEach(file -> {
                if (!file.getName().startsWith(Wrapper.WRAPPER_PREFIX)) {
                    file.delete();
                }
            });
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(2, files2.size());
            Collections.sort(files2);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files2));

            resolver.updateExtensions();
            var files3 = FileUtils.getFileList(tmp2);
            assertEquals(2, files3.size());
            Collections.sort(files3);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files3));
            FileUtils.writeString("updated", hash_file);

            resolver.updateExtensions();
            var files4 = FileUtils.getFileList(tmp2);
            assertEquals(9, files4.size());
            Collections.sort(files4);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files4));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testAddExtension()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver1 = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver1.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(9, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));

            var resolver2 = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1", "org.jsoup:jsoup:1.15.4"), false, false);
            resolver2.updateExtensions();
            var files3 = FileUtils.getFileList(tmp2);
            assertEquals(10, files3.size());
            Collections.sort(files3);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                jsoup-1.15.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files3));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testRemoveExtension()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver1 = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1", "org.jsoup:jsoup:1.15.4"), false, false);
            resolver1.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(10, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                jsoup-1.15.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));

            var resolver2 = new WrapperExtensionResolver(tmp1, hash_file, tmp2, new Properties(), new Properties(), List.of(MAVEN_CENTRAL), List.of("org.jsoup:jsoup:1.15.4"), false, false);
            resolver2.updateExtensions();
            var files3 = FileUtils.getFileList(tmp2);
            assertEquals(3, files3.size());
            Collections.sort(files3);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties
                jsoup-1.15.4.jar""", String.join("\n", files3));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }

    @Test
    void testOverrideExtension()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        try {
            new Wrapper().createWrapperFiles(tmp2, BldVersion.getVersion());

            var hash_file = new File(tmp1, "wrapper.hash");
            assertFalse(hash_file.exists());
            var files1 = FileUtils.getFileList(tmp2);
            assertEquals(2, files1.size());
            Collections.sort(files1);
            assertEquals("""
                bld-wrapper.jar
                bld-wrapper.properties""", String.join("\n", files1));

            var resolver1 = new WrapperExtensionResolver(tmp1, hash_file, tmp2,
                new Properties(), new Properties(),
                List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver1.updateExtensions();

            assertTrue(hash_file.exists());
            var files2 = FileUtils.getFileList(tmp2);
            assertEquals(9, files2.size());
            Collections.sort(files2);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.1.jar
                antlr4-runtime-4.11.1.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files2));

            var properties = new Properties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "org.antlr:antlr4:4.11.0");
            var resolver2 = new WrapperExtensionResolver(tmp1, hash_file, tmp2,
                properties, new Properties(),
                List.of(MAVEN_CENTRAL), List.of("org.antlr:antlr4:4.11.1"), false, false);
            resolver2.updateExtensions();
            var files3 = FileUtils.getFileList(tmp2);
            assertEquals(9, files3.size());
            Collections.sort(files3);
            assertEquals("""
                ST4-4.3.4.jar
                antlr-runtime-3.5.3.jar
                antlr4-4.11.0.jar
                antlr4-runtime-4.11.0.jar
                bld-wrapper.jar
                bld-wrapper.properties
                icu4j-71.1.jar
                javax.json-1.1.4.jar
                org.abego.treelayout.core-1.0.3.jar""", String.join("\n", files3));
        } finally {
            tmp2.delete();
            tmp1.delete();
        }
    }
}
