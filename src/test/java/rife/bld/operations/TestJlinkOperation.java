/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class TestJlinkOperation {
    @Test
    void testArguments() {
        var args = new HashMap<String, String>();
        args.put("--add-modules", "module-1,module-2");
        args.put("--bind-services", null);
        args.put("--compress", "zip-6");
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
                .compress(ZipCompression.ZIP_6)
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
    void testDisablePlugin() {
        var jlink = new JlinkOperation()
                .disablePlugin("vm")
                .disablePlugin("system-modules")
                .listPlugins();
        assertDoesNotThrow(jlink::execute);
    }

    @Test
    void testExecute() throws IOException {
        var tmpdir = Files.createTempDirectory("bld-jlink-test").toFile();
        tmpdir.deleteOnExit();

        var output = new File(tmpdir, "jlink");
        output.deleteOnExit();

        var options = new JlinkOptions()
                .modulePath("src/test/resources/jlink/build/jmod")
                .addModules("dev.mccue.tree")
                .launcher("tree", "dev.mccue.tree", "dev.mccue.tree.Tree")
                .output(output.getAbsolutePath());
        var jlink = new JlinkOperation().jlinkOptions(options);

        assertDoesNotThrow(jlink::execute);
        assertTrue(output.exists(), "Output dir does not exist");

        FileUtils.deleteDirectory(tmpdir);
    }

    @Test
    void testHelp() {
        var jlink = new JlinkOperation().addArgs("--help");
        assertDoesNotThrow(jlink::execute);
    }

    @Test
    void testNoArguments() {
        var jlink = new JlinkOperation();
        assertTrue(jlink.jlinkOptions().isEmpty(), "jlink options not empty");
        assertTrue(jlink.options().isEmpty(), "options not empty");
        assertThrows(ExitStatusException.class, jlink::execute);
    }

    @Test
    void testOptions() {
        var jlink = new JlinkOperation().options("src/test/resources/jlink/options_verbose.txt");
        assertDoesNotThrow(jlink::execute);
    }

    @Test
    void testVersion() {
        var jlink = new JlinkOperation().addArgs("--verbose", "--version");
        assertDoesNotThrow(jlink::execute);
    }
}
