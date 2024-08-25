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
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.operations.JpackageOperation.Launcher;
import static rife.bld.operations.JpackageOptions.PackageType;

public class TestJpackageOperation {
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream stdout = System.out;

    @AfterEach
    public void tearDown() {
        System.setOut(stdout);
    }

    @Test
    void testAddLauncher() {
        var op = new JpackageOperation();

        var foo = new Launcher("foo-name", "foo-path");
        var bar = new Launcher("bar-name", Path.of("bar-path"));
        assertEquals(bar.name(), "bar-name");
        assertEquals(bar.path(), "bar-path");

        var f = new File("foo/bar");
        var foobar = new Launcher("foobar", f);
        assertEquals(foobar.name(), "foobar");
        assertEquals(foobar.path(), f.getAbsolutePath());

        op = op.addLauncher(foo);
        assertTrue(op.launchers().contains(foo), "foo not found");
        op.addLauncher(bar);
        assertTrue(op.launchers().contains(bar), "bar not found");
    }

    @Test
    void testAppImage() {
        var options = new JpackageOptions().appImage("foo");
        assertEquals("foo", options.get("--app-image"));
        options.appImage(Path.of("bar"));
        assertEquals("bar", options.get("--app-image"));

        var foo = new File("foo");
        options = options.appImage(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--app-image"));
    }

    @Test
    void testArguments() {
        var args = new HashMap<String, String>();
        args.put("--about-url", "about-url");
        args.put("--add-modules", "modules-1,modules-2");
        args.put("--app-content", "content-1,content-2");
        args.put("--app-image", "app-image");
        args.put("--app-version", "app-version");
        args.put("--arguments", "argument1 argument2");
        args.put("--copyright", "copyright");
        args.put("--description", "description");
        args.put("--dest", "dest");
        args.put("--file-associations", "file-associations");
        args.put("--icon", "icon");
        args.put("--input", "input");
        args.put("--install-dir", "install-dir");
        args.put("--java-options", "java-options");
        args.put("--jlink-options", "--strip-debug --add-modules module-1,module-2");
        args.put("--launcher-as-service", null);
        args.put("--license-file", "license-file");
        args.put("--linux-app-category", "linux-app-category");
        args.put("--linux-app-release", "linux-app-release");
        args.put("--linux-deb-maintainer", "linux-deb-maintainer");
        args.put("--linux-menu-group", "linux-menu-group");
        args.put("--linux-package-deps", null);
        args.put("--linux-package-name", "linux-package-name");
        args.put("--linux-rpm-license-type", "linux-rpm-license-type");
        args.put("--linux-shortcut", null);
        args.put("--mac-app-category", "mac-app-category");
        args.put("--mac-app-image-sign-identity", "mac-app-image-sign-identity");
        args.put("--mac-app-store", null);
        args.put("--mac-dmg-content", "mac-dmg-content");
        args.put("--mac-entitlements", "mac-entitlements");
        args.put("--mac-installer-sign-identity", "mac-installer-sign-identity");
        args.put("--mac-package-identifier", "mac-package-identifier");
        args.put("--mac-package-name", "mac-package-name");
        args.put("--mac-package-signing-prefix", "mac-package-signing-prefix");
        args.put("--mac-sign", null);
        args.put("--mac-signing-key-user-name", "mac-signing-key-user-name");
        args.put("--mac-signing-keychain", "mac-signing-keychain");
        args.put("--main-class", "main-class");
        args.put("--main-jar", "main-jar");
        args.put("--module", "module");
        args.put("--module-path", "module-path-1:module-path-2");
        args.put("--name", "name");
        args.put("--resource-dir", "resource-dir");
        args.put("--runtime-image", "runtime-image");
        args.put("--strip-debug", null);
        args.put("--temp", "temp");
        args.put("--type", "exe");
        args.put("--vendor", "vendor");
        args.put("--verbose", null);
        args.put("--win-console", null);
        args.put("--win-dir-chooser", null);
        args.put("--win-help-url", "win-help-url");
        args.put("--win-menu", null);
        args.put("--win-menu-group", "win-menu-group");
        args.put("--win-per-user-install", null);
        args.put("--win-shortcut", null);
        args.put("--win-shortcut-prompt", null);
        args.put("--win-update-url", "win-update-url");
        args.put("--win-upgrade-uuid", "win-upgrade-uuid");

        var options = new JpackageOptions()
                .aboutUrl(args.get("--about-url"))
                .addModules(args.get("--add-modules").split(","))
                .appContent(args.get("--app-content").split(","))
                .appImage(args.get("--app-image"))
                .appVersion(args.get("--app-version"))
                .arguments(args.get("--arguments").split(" "))
                .copyright(args.get("--copyright"))
                .description(args.get("--description"))
                .dest(args.get("--dest"))
                .fileAssociations(args.get("--file-associations").split(","))
                .icon(args.get("--icon"))
                .input(args.get("--input"))
                .installDir(args.get("--install-dir"))
                .javaOptions(args.get("--java-options").split(","))
                .jlinkOptions(new JlinkOptions().stripDebug(true).addModules("module-1", "module-2"))
                .launcherAsService(true)
                .licenseFile(args.get("--license-file"))
                .linuxAppCategory(args.get("--linux-app-category"))
                .linuxAppRelease(args.get("--linux-app-release"))
                .linuxDebMaintainer(args.get("--linux-deb-maintainer"))
                .linuxMenuGroup(args.get("--linux-menu-group"))
                .linuxPackageDeps(true)
                .linuxPackageName(args.get("--linux-package-name"))
                .linuxRpmLicenseType(args.get("--linux-rpm-license-type"))
                .linuxShortcut(true)
                .macAppCategory(args.get("--mac-app-category"))
                .macAppImageSignIdentity(args.get("--mac-app-image-sign-identity"))
                .macAppStore(true)
                .macDmgContent(args.get("--mac-dmg-content"))
                .macEntitlements(args.get("--mac-entitlements"))
                .macInstallerSignIdentity(args.get("--mac-installer-sign-identity"))
                .macPackageIdentifier(args.get("--mac-package-identifier"))
                .macPackageName(args.get("--mac-package-name"))
                .macPackageSigningPrefix(args.get("--mac-package-signing-prefix"))
                .macSign(true)
                .macSigningKeyUserName(args.get("--mac-signing-key-user-name"))
                .macSigningKeychain(args.get("--mac-signing-keychain"))
                .mainClass(args.get("--main-class"))
                .mainJar(args.get("--main-jar"))
                .module(args.get("--module"))
                .modulePath(args.get("--module-path").split(","))
                .name(args.get("--name"))
                .resourceDir(args.get("--resource-dir"))
                .runtimeImage(args.get("--runtime-image"))
                .stripDebug(true)
                .temp(args.get("--temp"))
                .type(PackageType.EXE)
                .vendor(args.get("--vendor"))
                .verbose(true)
                .winConsole(true)
                .winDirChooser(true)
                .winHelpUrl(args.get("--win-help-url"))
                .winMenu(true)
                .winMenuGroup(args.get("--win-menu-group"))
                .winPerUserInstall(true)
                .winShortcut(true)
                .winShortcutPrompt(true)
                .winUpdateUrl(args.get("--win-update-url"))
                .winUpgradeUuid(args.get("--win-upgrade-uuid"));

        assertEquals(options.size(), args.size(), "Wrong number of arguments");

        for (var arg : args.entrySet()) {
            assertTrue(options.containsKey(arg.getKey()), arg.getValue() + " not found");
            assertEquals(arg.getValue(), options.get(arg.getKey()), arg.getKey());
        }

    }

    @Test
    void testCmdFiles() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jpackage = new JpackageOperation().cmdFiles(new File("src/test/resources/jlink/options_verbose.txt"),
                new File("src/test/resources/jlink/options_version.txt"));
        assertDoesNotThrow(jpackage::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }

    @Test
    void testCmdFilesPath() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jpackage = new JpackageOperation().cmdFiles(Path.of("src/test/resources/jlink/options_verbose.txt"),
                Path.of("src/test/resources/jlink/options_version.txt"));
        assertDoesNotThrow(jpackage::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }

    @Test
    void testCmdFilesVersion() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jpackage = new JpackageOperation().cmdFiles("src/test/resources/jlink/options_version.txt");
        assertDoesNotThrow(jpackage::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }

    @Test
    void testCreatePackage() throws Exception {
        var tmpdir = Files.createTempDirectory("bld-jpackage-test").toFile();
        try {
            var jlinkOptions = new JlinkOptions()
                    .compress(JlinkOptions.CompressionLevel.ZIP)
                    .stripNativeCommands(true);
            var options = new JpackageOptions()
                    .input("lib/bld")
                    .name("bld")
                    .mainJar("bld-wrapper.jar")
                    .javaOptions("--enable-preview")
                    .dest(tmpdir.getAbsolutePath())
                    .verbose(true)
                    .jlinkOptions(jlinkOptions);
            var os = System.getProperty("os.version");
            if (os.endsWith("MANJARO")) {
                options.type(PackageType.DEB);
            }

            var jpackage = new JpackageOperation().jpackageOptions(options);
            jpackage.execute();

            var files = tmpdir.listFiles();
            assertNotNull(files, "files should not be null");
            assertTrue(files.length > 0, "no files found");

            assertTrue(files[0].getName().matches("bld.*\\.[A-Za-z]{3}"), "Package not found");
        } finally {
            FileUtils.deleteDirectory(tmpdir);
        }
    }

    @Test
    void testDest() {
        var options = new JpackageOptions().dest("foo");
        assertEquals("foo", options.get("--dest"));
        options = options.dest(Path.of("bar"));
        assertEquals("bar", options.get("--dest"));

        var foo = new File("foo");
        options.dest(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--dest"));
    }

    @Test
    void testFileAssociations() {
        var options = new JpackageOptions().fileAssociations("foo", "bar");
        assertEquals("foo,bar", options.get("--file-associations"));

        options = options.fileAssociations(Path.of("bar"), Path.of("foo"));
        assertEquals("bar,foo", options.get("--file-associations"));

        var foo = new File("foo");
        var bar = new File("bar");
        options.fileAssociations(foo, bar);
        assertEquals(foo.getAbsolutePath() + ',' + bar.getAbsolutePath(), options.get("--file-associations"));
    }

    @Test
    void testHelp() {
        var jpackage = new JpackageOperation().toolArgs("--help");
        assertDoesNotThrow(jpackage::execute);
        assertTrue(jpackage.toolArgs().isEmpty(), "args not empty");
    }

    @Test
    void testIcon() {
        var options = new JpackageOptions().icon("foo");
        assertEquals("foo", options.get("--icon"));
        options = options.icon(Path.of("bar"));
        assertEquals("bar", options.get("--icon"));

        var foo = new File("foo");
        options.icon(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--icon"));
    }

    @Test
    void testInput() {
        var options = new JpackageOptions();

        options.input("foo");
        assertEquals("foo", options.get("--input"));
        options.input(Path.of("bar"));
        assertEquals("bar", options.get("--input"));

        var foo = new File("foo");
        options.input(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--input"));
    }

    @Test
    void testInstallDir() {
        var options = new JpackageOptions().installDir("foo");
        assertEquals("foo", options.get("--install-dir"));
        options = options.installDir(Path.of("bar"));
        assertEquals("bar", options.get("--install-dir"));

        var foo = new File("foo");
        options.installDir(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--install-dir"));
    }

    @Test
    void testLicenseFile() {
        var options = new JpackageOptions().licenseFile("foo");
        assertEquals("foo", options.get("--license-file"));
        options = options.licenseFile(Path.of("bar"));
        assertEquals("bar", options.get("--license-file"));

        var foo = new File("foo");
        options.licenseFile(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--license-file"));
    }

    @Test
    void testMacDmgContent() {
        var options = new JpackageOptions().macDmgContent("foo", "bar");
        assertEquals("foo,bar", options.get("--mac-dmg-content"));

        options = options.macDmgContent(Path.of("bar"), Path.of("foo"));
        assertEquals("bar,foo", options.get("--mac-dmg-content"));

        var foo = new File("foo");
        var bar = new File("bar");
        options.macDmgContent(foo, bar);
        assertEquals(foo.getAbsolutePath() + ',' + bar.getAbsolutePath(), options.get("--mac-dmg-content"));
    }

    @Test
    void testMacEntitlements() {
        var options = new JpackageOptions().macEntitlements("foo");
        assertEquals("foo", options.get("--mac-entitlements"));
        options = options.macEntitlements(Path.of("bar"));
        assertEquals("bar", options.get("--mac-entitlements"));

        var foo = new File("foo");
        options.macEntitlements(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--mac-entitlements"));
    }

    @Test
    void testModule() {
        var options = new JpackageOptions().module("name");
        assertEquals("name", options.get("--module"));

        options.module("name", "class");
        assertEquals("name:class", options.get("--module"));
    }

    @Test
    void testModulePath() {
        var options = new JpackageOptions().modulePath("foo");
        assertEquals("foo", options.get("--module-path"));
        options = options.modulePath(Path.of("bar"));
        assertEquals("bar", options.get("--module-path"));

        var foo = new File("foo");
        options.modulePath(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--module-path"));
    }

    @Test
    void testNoArguments() {
        var jpackage = new JpackageOperation();
        assertTrue(jpackage.cmdFiles().isEmpty(), "file options not empty");
        assertTrue(jpackage.jpackageOptions().isEmpty(), "jpackage options not empty");
        assertThrows(ExitStatusException.class, jpackage::execute);
    }

    @Test
    void testResourceDir() {
        var options = new JpackageOptions().resourceDir("foo");
        assertEquals("foo", options.get("--resource-dir"));
        options = options.resourceDir(Path.of("bar"));
        assertEquals("bar", options.get("--resource-dir"));

        var foo = new File("foo");
        options.resourceDir(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--resource-dir"));
    }

    @Test
    void testRuntimeImage() {
        var options = new JpackageOptions().runtimeImage("foo");
        assertEquals("foo", options.get("--runtime-image"));
        options = options.runtimeImage(Path.of("bar"));
        assertEquals("bar", options.get("--runtime-image"));

        var foo = new File("foo");
        options.runtimeImage(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--runtime-image"));
    }

    @Test
    void testTemp() {
        var options = new JpackageOptions().temp("foo");
        assertEquals("foo", options.get("--temp"));
        options = options.temp(Path.of("bar"));
        assertEquals("bar", options.get("--temp"));

        var foo = new File("foo");
        options.temp(foo);
        assertEquals(foo.getAbsolutePath(), options.get("--temp"));
    }

    @Test
    void testVersion() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jpackage = new JpackageOperation().toolArgs("--verbose", "--version");
        assertDoesNotThrow(jpackage::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }
}
