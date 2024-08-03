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
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.operations.JpackageOptions.Launcher;
import static rife.bld.operations.JpackageOptions.PackageType;

public class TestJpackageOperation {
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream stdout = System.out;

    @AfterEach
    public void tearDown() {
        System.setOut(stdout);
    }

    @Test
    void testArguments() {
        var args = new HashMap<String, String>();
        args.put("--about-url", "about-url");
        args.put("--add-launcher", "name=path");
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
                .addLauncher(new Launcher("name", "path"))
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
    void testCreatePackage() throws Exception {
        var tmpdir = Files.createTempDirectory("bld-jpackage-test").toFile();
        try {
            var options = new JpackageOptions()
                    .input("lib/bld")
                    .name("bld")
                    .mainJar("bld-wrapper.jar")
                    .javaOptions("--enable-preview")
                    .dest(tmpdir.getAbsolutePath())
                    .verbose(true);

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
    void testFileOptions() {
        System.setOut(new PrintStream(outputStreamCaptor));
        var jpackage = new JpackageOperation().fileOptions("src/test/resources/jlink/options_verbose.txt",
                "src/test/resources/jlink/options_version.txt");
        assertDoesNotThrow(jpackage::execute);
        var out = outputStreamCaptor.toString();
        assertTrue(out.matches("[\\d.]+[\\r\\n]+"), out);
    }

    @Test
    void testHelp() {
        var jpackage = new JpackageOperation().toolArgs("--help");
        assertDoesNotThrow(jpackage::execute);
        assertTrue(jpackage.toolArgs().isEmpty(), "args not empty");
    }

    @Test
    void testNoArguments() {
        var jpackage = new JpackageOperation();
        assertTrue(jpackage.fileOptions().isEmpty(), "file options not empty");
        assertTrue(jpackage.jpackageOptions().isEmpty(), "jpackage options not empty");
        assertThrows(ExitStatusException.class, jpackage::execute);
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
