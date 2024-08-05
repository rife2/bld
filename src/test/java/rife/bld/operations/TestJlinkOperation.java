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
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.operations.JlinkOptions.CompressionLevel;

public class TestJlinkOperation {
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream stdout = System.out;

    @AfterEach
    public void tearDown() {
        System.setOut(stdout);
    }

    @Test
    void testArguments() {
        var args = new HashMap<String, String>();
        args.put("--add-modules", "module-1,module-2");
        args.put("--bind-services", null);
        args.put("--compress", "2");
        args.put("--endian", "big");
        args.put("--ignore-signing-information", null);
        args.put("--launcher", "name=module/mainclass");
        args.put("--limit-modules", "module-1,module-2");
        args.put("--module-path", "module-path");
        args.put("--no-header-files", null);
        args.put("--no-man-pages", null);
        args.put("--output", "output");
        args.put("--save-opts", "save-opts");
        args.put("--strip-debug", null);
        args.put("--suggest-providers", "provider-1,provider-2");
        args.put("--verbose", null);

        var options = new JlinkOptions()
                .addModules(args.get("--add-modules").split(","))
                .bindServices(true)
                .compress(CompressionLevel.ZIP)
                .endian(JlinkOptions.Endian.BIG)
                .ignoreSigningInformation(true)
                .launcher("name", "module", "mainclass")
                .limitModule(args.get("--limit-modules").split(","))
                .modulePath(args.get("--module-path"))
                .noHeaderFiles(true)
                .noManPages(true)
                .output(args.get("--output"))
                .saveOpts(args.get("--save-opts"))
                .stripDebug(true)
                .suggestProviders(args.get("--suggest-providers").split(","))
                .verbose(true);

        assertEquals(options.size(), args.size(), "Wrong number of arguments");

        for (var arg : args.entrySet()) {
            assertTrue(options.containsKey(arg.getKey()), arg.getValue() + " not found");
            assertEquals(arg.getValue(), options.get(arg.getKey()), arg.getKey());
        }

        options.launcher("name-2", "module-2");
        assertEquals("name-2=module-2", options.get("--launcher"), "incorrect launcher");
    }

    @Test
    void testCmdFiles() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jlink = new JlinkOperation().cmdFiles("src/test/resources/jlink/options_jlink.txt");
        assertDoesNotThrow(jlink::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.contains("List of available plugins:"), out);
    }

    @Test
    void testCmdFilesMulti() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jlink = new JlinkOperation().cmdFiles("src/test/resources/jlink/options_verbose.txt",
                "src/test/resources/jlink/options_version.txt");
        assertDoesNotThrow(jlink::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }

    @Test
    void testDisablePlugin() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jlink = new JlinkOperation()
                .disablePlugin("vm")
                .disablePlugin("system-modules")
                .toolArgs("--list-plugins");
        assertDoesNotThrow(jlink::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.contains("List of available plugins:"), out);
    }

    @Test
    void testExecute() throws IOException {
        var tmpdir = Files.createTempDirectory("bld-jlink-test").toFile();
        try {
            var output = new File(tmpdir, "jlink");

            var options = new JlinkOptions()
                    .modulePath("src/test/resources/jlink/build/jmod")
                    .addModules("dev.mccue.tree")
                    .launcher("tree", "dev.mccue.tree", "dev.mccue.tree.Tree")
                    .output(output.getAbsolutePath());
            if (Runtime.version().version().get(0) >= 21) {
                options.compress(ZipCompression.ZIP_6);
            } else {
                options.compress(CompressionLevel.ZIP);
            }

            var jlink = new JlinkOperation().jlinkOptions(options);

            assertDoesNotThrow(jlink::execute);
            assertTrue(output.exists(), "Output dir does not exist");
        } finally {
            FileUtils.deleteDirectory(tmpdir);
        }
    }

    @Test
    void testHelp() {
        var jlink = new JlinkOperation().toolArgs("--help");
        assertDoesNotThrow(jlink::execute);
        assertTrue(jlink.toolArgs().isEmpty(), "args not empty");
    }

    @Test
    void testNoArguments() {
        var jlink = new JlinkOperation();
        assertTrue(jlink.jlinkOptions().isEmpty(), "jlink options not empty");
        assertTrue(jlink.cmdFiles().isEmpty(), "file options not empty");
        assertThrows(ExitStatusException.class, jlink::execute);
    }

    @Test
    void testVersion() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jlink = new JlinkOperation().toolArgs("--verbose", "--version");
        assertDoesNotThrow(jlink::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }
}
