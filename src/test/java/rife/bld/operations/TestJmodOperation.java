/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.operations.exceptions.ExitStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class TestJmodOperation {
    @Test
    void testNoArguments() {
        var jmod = new JmodOperation();
        assertThrows(ExitStatusException.class, jmod::execute);
    }

    @Test
    void testOptions() {
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
        args.put("@filename", null);

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
                .filename("filename")
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
    void testVersion() {
        var jmod = new JmodOperation()
                .operationMode(JmodOperation.OperationMode.DESCRIBE)
                .jmodFile("foo")
                .addArgs("--version");
        assertDoesNotThrow(jmod::execute);
    }
}
