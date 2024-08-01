/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import org.junit.Test;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class TestJpackageOperation {
    @Test
    public void TestCreatePackage() throws Exception {
        var tmpdir = Files.createTempDirectory("bld-jpackage-test").toFile();
        tmpdir.deleteOnExit();
        var options = new JpackageOptions()
                .input("lib/bld")
                .name("bld")
                .mainJar("bld-wrapper.jar")
                .javaOptions("--enable-preview")
                .dest(tmpdir.getAbsolutePath())
                .verbose(true);

        var os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            options.type(JpackageOptions.PackageType.EXE);
        } else if (os.startsWith("Linux")) {
            options.type(JpackageOptions.PackageType.DEB);
        } else if (os.startsWith("Mac")) {
            options.type(JpackageOptions.PackageType.DMG);
        }

        var jpackage = new JpackageOperation().jpackageOptions(options);
        jpackage.execute();

        var deb = new File(tmpdir, "bld_1.0-1_amd64.deb");
        assertTrue(deb.delete());
    }

    @Test
    public void TestNoArguments() {
        var jpackage = new JpackageOperation();
        assertThrows(ExitStatusException.class, jpackage::execute);
    }

    @Test
    public void TestVersion() {
        var jpackage = new JpackageOperation().toolArg("--version");
        assertDoesNotThrow(jpackage::execute);
    }
}
