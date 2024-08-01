/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.operations.exceptions.ExitStatusException;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class TestJpackageOperation {
    @Test
    void testCreatePackage() throws Exception {
        var tmpdir = Files.createTempDirectory("bld-jpackage-test").toFile();
        tmpdir.deleteOnExit();
        var options = new JpackageOptions()
                .input("lib/bld")
                .name("bld")
                .mainJar("bld-wrapper.jar")
                .javaOptions("--enable-preview")
                .dest(tmpdir.getAbsolutePath())
                .verbose(true);

        var os = System.getProperty("os.version");
        if (os.endsWith("MANJARO")) {
            options.type(JpackageOptions.PackageType.DEB);
        }

        var jpackage = new JpackageOperation().jpackageOptions(options);
        jpackage.execute();

        var files = tmpdir.listFiles();
        assertNotNull(files, "files should not be null");
        assertTrue(files.length > 0, "No files found");

        for (var file : files) {
            System.out.println(file.getName());
            file.deleteOnExit();
        }
    }

    @Test
    void testNoArguments() {
        var jpackage = new JpackageOperation();
        assertThrows(ExitStatusException.class, jpackage::execute);
    }

    @Test
    void testVersion() {
        var jpackage = new JpackageOperation().toolArg("--version");
        assertDoesNotThrow(jpackage::execute);
    }
}
