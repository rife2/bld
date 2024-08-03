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
        args.put("--compress", "zip-5");
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
                .compress(ZipCompression.ZIP_5)
                .config(args.get("--config"))
                .date(ZonedDateTime.of(1997, 8, 29, 2, 14, 0, 0, ZoneId.of("America/Los_Angeles")))
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
    void testCreate() throws IOException {
        var tmpdir = Files.createTempDirectory("bld-jmod-test").toFile();
        try {
            var mod = new File(tmpdir, "dev.mccue.tree.jmod");

            var options = new JmodOptions()
                    .legalNotices("src/test/resources/jlink/dev.mccue.apple/legal")
                    .classpath("src/test/resources/jlink/build/jar/dev.mccue.apple.jar");
            var jmod = new JmodOperation()
                    .operationMode(OperationMode.CREATE)
                    .jmodFile(mod.getAbsolutePath())
                    .jmodOptions(options);

            assertDoesNotThrow(jmod::execute);
            assertTrue(mod.exists(), "mod does not exist");
        } finally {
            FileUtils.deleteDirectory(tmpdir);
        }
    }

    @Test
    void testExecute() throws IOException {
        var tmpdir = Files.createTempDirectory("bld-jmod-test").toFile();
        try {
            var mod = new File(tmpdir, "dev.mccue.tree.jmod");

            var options = new JmodOptions().classpath("src/test/resources/jlink/build/jar/dev.mccue.tree.jar");
            var jmod = new JmodOperation()
                    .operationMode(OperationMode.CREATE)
                    .jmodFile(mod.getAbsolutePath())
                    .jmodOptions(options);

            assertDoesNotThrow(jmod::execute);
            assertTrue(mod.exists(), "mod does not exist");

            jmod.jmodOptions().clear();

            jmod.operationMode(OperationMode.DESCRIBE);
            assertDoesNotThrow(jmod::execute, "describe mod failed");

            jmod.operationMode(OperationMode.LIST);
            assertDoesNotThrow(jmod::execute, "list mod failed");
        } finally {
            FileUtils.deleteDirectory(tmpdir);
        }
    }

    @Test
    void testHelp() {
        var jmod = new JmodOperation().toolArgs("--help-extra");
        assertDoesNotThrow(jmod::execute);
        assertTrue(jmod.toolArgs().isEmpty(), "args not empty");
    }

    @Test
    void testNoArguments() {
        var jmod = new JmodOperation();
        assertTrue(jmod.fileOptions().isEmpty(), "file options not empty");
        assertTrue(jmod.jmodOptions().isEmpty(), "jmod options not empty");
        assertThrows(ExitStatusException.class, jmod::execute);
    }

    @Test
    void testFileOptions() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jmod = new JmodOperation().fileOptions("src/test/resources/jlink/options_version.txt");
        assertDoesNotThrow(jmod::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("\\d+.\\d+.\\d+[\\r\\n]+"), out);
    }

    @Test
    void testVersion() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jmod = new JmodOperation().toolArgs("--version");
        assertDoesNotThrow(jmod::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("\\d+.\\d+.\\d+[\\r\\n]+"), out);
    }
}
