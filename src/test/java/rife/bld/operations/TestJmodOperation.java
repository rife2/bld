/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.operations.JmodOperation.OperationMode;

public class TestJmodOperation {
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream stdout = System.out;

    @AfterEach
    public void tearDown() {
        System.setOut(stdout);
    }

    @Test
    void testArguments() {
        var args = new HashMap<String, String>();
        args.put("--class-path", "classpath");
        args.put("--cmds", "cmds");
        args.put("--config", "config");
        args.put("--date", "1997-08-29T09:14:00Z");
        args.put("--dir", "dir");
        args.put("--do-not-resolve-by-default", null);
        args.put("--dry-run", null);
        args.put("--exclude", "glob:glob,regex:regex");
        args.put("--hash-modules", "regex");
        args.put("--header-files", "header-files");
        args.put("--legal-notices", "legal-notices");
        args.put("--libs", "libs");
        args.put("--main-class", "main-class");
        args.put("--man-pages", "man-pages");
        args.put("--module-path", "module-path");
        args.put("--module-version", "module-version");
        args.put("--target-platform", "target-platform");
        args.put("--warn-if-resolved", "deprecated");

        var options = new JmodOptions()
                .classpath(args.get("--class-path"))
                .cmds(args.get("--cmds"))
                .config(args.get("--config"))
                .date(ZonedDateTime.of(1997, 8, 29, 2, 14, 0, 1, ZoneId.of("America/Los_Angeles")))
                .dir(args.get("--dir"))
                .doNotResolveByDefault(true)
                .dryRun(true)
                .exclude(new JmodOptions.FilePattern(JmodOptions.FilePatternType.GLOB, "glob"),
                        new JmodOptions.FilePattern(JmodOptions.FilePatternType.REGEX, "regex"))
                .hashModules(args.get("--hash-modules"))
                .headerFiles(args.get("--header-files"))
                .legalNotices(args.get("--legal-notices"))
                .libs(args.get("--libs"))
                .mainClass(args.get("--main-class"))
                .manPages(args.get("--man-pages"))
                .modulePath(args.get("--module-path"))
                .moduleVersion(args.get("--module-version"))
                .targetPlatform(args.get("--target-platform"))
                .warnIfResolved(JmodOptions.ResolvedReason.DEPRECATED);

        assertEquals(options.size(), args.size(), "Wrong number of arguments");

        for (var arg : args.entrySet()) {
            assertTrue(options.containsKey(arg.getKey()), arg.getValue() + " not found");
            assertEquals(arg.getValue(), options.get(arg.getKey()), arg.getKey());
        }
    }

    @Test
    void testCmdFiles() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jmod = new JmodOperation().cmdFiles(new File("src/test/resources/jlink/options_version.txt"));
        assertDoesNotThrow(jmod::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }

    @Test
    void testCmdFilesCreate() throws IOException {
        var tmpdir = Files.createTempDirectory("bld-jmod-test").toFile();
        try {
            var mod = new File(tmpdir, "dev.mccue.tree.jmod");

            var jmod = new JmodOperation()
                    .cmdFiles("src/test/resources/jlink/options_jmod.txt")
                    .jmodFile(mod);

            assertDoesNotThrow(jmod::execute);
            assertTrue(mod.exists(), "mod does not exist");
        } finally {
            FileUtils.deleteDirectory(tmpdir);
        }
    }

    @Test
    void testCmdFilesPath() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jmod = new JmodOperation().cmdFiles(Path.of("src/test/resources/jlink/options_version.txt"));
        assertDoesNotThrow(jmod::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }

    @Test
    void testCmds() {
        var options = new JmodOptions().cmds("foo");
        assertEquals("foo", options.get("--cmds"));
        options = options.cmds(Path.of("bar"));
        assertEquals("bar", options.get("--cmds"));

        var foo = new File("foo");
        options.cmds(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--cmds"));
    }

    @Test
    void testConfig() {
        var options = new JmodOptions().config("foo");
        assertEquals("foo", options.get("--config"));
        options = options.config(Path.of("bar"));
        assertEquals("bar", options.get("--config"));

        var foo = new File("foo");
        options.config(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--config"));
    }

    @Test
    void testCreate() throws IOException {
        var tmpdir = Files.createTempDirectory("bld-jmod-test").toFile();
        try {
            var mod = new File(tmpdir, "dev.mccue.tree.jmod");

            var options = new JmodOptions()
                    .date(ZonedDateTime.now())
                    .legalNotices("src/test/resources/jlink/dev.mccue.apple/legal")
                    .classpath("src/test/resources/jlink/build/jar/dev.mccue.apple.jar");
            var jmod = new JmodOperation()
                    .operationMode(OperationMode.CREATE)
                    .jmodFile(mod)
                    .jmodOptions(options);

            assertDoesNotThrow(jmod::execute);
            assertTrue(mod.exists(), "mod does not exist");
        } finally {
            FileUtils.deleteDirectory(tmpdir);
        }
    }

    @Test
    void testDir() {
        var options = new JmodOptions().dir("foo");
        assertEquals("foo", options.get("--dir"));
        options = options.dir(Path.of("bar"));
        assertEquals("bar", options.get("--dir"));

        var foo = new File("foo");
        options.dir(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--dir"));
    }

    @Test
    void testExecute() throws IOException {
        var tmpdir = Files.createTempDirectory("bld-jmod-test").toFile();
        try {
            var mod = new File(tmpdir, "dev.mccue.tree.jmod");

            var options = new JmodOptions().classpath("src/test/resources/jlink/build/jar/dev.mccue.tree.jar");
            if (Runtime.version().version().get(0) >= 20) {
                options.compress(ZipCompression.ZIP_9);
            }

            var jmod = new JmodOperation()
                    .operationMode(OperationMode.CREATE)
                    .jmodFile(mod)
                    .jmodOptions(options);

            assertDoesNotThrow(jmod::execute);
            assertTrue(mod.exists(), "mod does not exist");

            jmod.jmodOptions().clear();
            System.setOut(new PrintStream(outputStreamCaptor));

            jmod.operationMode(OperationMode.DESCRIBE);
            assertDoesNotThrow(jmod::execute, "describe mod failed");
            assertTrue(outputStreamCaptor.toString().contains("dev.mccue.tree"),
                    "missing dev.mccue.tee in:\n" + outputStreamCaptor);

            jmod.operationMode(OperationMode.LIST);
            assertDoesNotThrow(jmod::execute, "list mod failed");
            assertTrue(outputStreamCaptor.toString().contains("module-info.class"),
                    "missing module-info.class in:\n" + outputStreamCaptor);
        } finally {
            FileUtils.deleteDirectory(tmpdir);
        }
    }

    @Test
    void testHeaderFiles() {
        var options = new JmodOptions().headerFiles("foo");
        assertEquals("foo", options.get("--header-files"));
        options = options.headerFiles(Path.of("bar"));
        assertEquals("bar", options.get("--header-files"));

        var foo = new File("foo");
        options.headerFiles(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--header-files"));
    }

    @Test
    void testHelp() {
        var jmod = new JmodOperation().toolArgs("--help-extra");
        assertDoesNotThrow(jmod::execute);
        assertTrue(jmod.toolArgs().isEmpty(), "args not empty");
    }

    @Test
    void testJmodFile() {
        var op = new JmodOperation().jmodFile("foo");
        assertEquals("foo", op.jmodFile());
        op = op.jmodFile(Path.of("bar"));
        assertEquals("bar", op.jmodFile());

        var foo = new File("foo");
        op.jmodFile(foo);
        assertEquals(foo.getAbsolutePath(), op.jmodFile());
    }

    @Test
    void testLegalNotices() {
        var options = new JmodOptions().legalNotices("foo");
        assertEquals("foo", options.get("--legal-notices"));
        options = options.legalNotices(Path.of("bar"));
        assertEquals("bar", options.get("--legal-notices"));

        var foo = new File("foo");
        options.legalNotices(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--legal-notices"));
    }

    @Test
    void testLibs() {
        var options = new JmodOptions().libs("foo");
        assertEquals("foo", options.get("--libs"));
        options = options.libs(Path.of("bar"));
        assertEquals("bar", options.get("--libs"));

        var foo = new File("foo");
        options.libs(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--libs"));
    }

    @Test
    void testManPages() {
        var options = new JmodOptions().manPages("foo");
        assertEquals("foo", options.get("--man-pages"));
        options = options.manPages(Path.of("bar"));
        assertEquals("bar", options.get("--man-pages"));

        var foo = new File("foo");
        options.manPages(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--man-pages"));
    }

    @Test
    void testModulePath() {
        var options = new JmodOptions().modulePath("foo");
        assertEquals("foo", options.get("--module-path"));
        options = options.modulePath(Path.of("bar"));
        assertEquals("bar", options.get("--module-path"));

        var foo = new File("foo");
        options.modulePath(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--module-path"));
    }

    @Test
    void testNoArguments() {
        var jmod = new JmodOperation();
        assertTrue(jmod.cmdFiles().isEmpty(), "file options not empty");
        assertTrue(jmod.jmodOptions().isEmpty(), "jmod options not empty");
        assertThrows(ExitStatusException.class, jmod::execute);
    }

    @Test
    void testVersion() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jmod = new JmodOperation().toolArgs("--version");
        assertDoesNotThrow(jmod::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }
}
