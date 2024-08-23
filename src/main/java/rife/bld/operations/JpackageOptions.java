/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.util.HashMap;

/**
 * Options for jpackage tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.0.2
 */
public class JpackageOptions extends HashMap<String, String> {
    /**
     * URL of the application's home page.
     *
     * @param url the URL
     * @return this map of options
     */
    public JpackageOptions aboutUrl(String url) {
        put("--about-url", url);
        return this;
    }

    /**
     * List of application launchers.
     * <p>
     * The main application launcher will be built from the command line options.
     * <p>
     * Additional alternative launchers can be built using this option, and this option can be used to build multiple
     * additional launchers.
     *
     * @param launcher one or more {@link Launcher}
     * @return this map of options
     */
    public JpackageOptions addLauncher(Launcher... launcher) {
        for (var l : launcher) {
            put("--add-launcher", l.name + '=' + l.path);
        }
        return this;
    }

    /**
     * List of modules to add.
     * <p>
     * This module list, along with the main module (if specified) will be passed to jlink as the
     * {@link JlinkOptions#addModules(String...) addModules} argument. If not specified, either just the main module
     * (if {@link #module(String, String) module} is specified), or the default set of modules (if
     * {@link #mainJar(String) mainJar} is specified) are used.
     *
     * @param modules one or more module
     * @return this map of options
     */
    public JpackageOptions addModules(String... modules) {
        put("--add-modules", String.join(",", modules));
        return this;
    }

    /**
     * List of paths to files and/or directories to add to the application payload.
     * <p>
     * <b>Requires Java 20 or higher</b>.
     *
     * @param additionalContent one or more path
     * @return this map of options
     */
    public JpackageOptions appContent(String... additionalContent) {
        put("--app-content", String.join(",", additionalContent));
        return this;
    }

    /**
     * Location of the predefined application image that is used to build an installable package.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions appImage(String path) {
        put("--app-image", path);
        return this;
    }

    /**
     * Version of the application and/or package.
     *
     * @param version the version
     * @return this map of options
     */
    public JpackageOptions appVersion(String version) {
        put("--app-version", version);
        return this;
    }

    /**
     * Command line arguments to pass to main class if no command line arguments are given to the launcher.
     *
     * @param argument one or more argument
     * @return this map of options
     */
    public JpackageOptions arguments(String... argument) {
        put("--arguments", String.join(" ", argument));
        return this;
    }

    /**
     * Copyright of the application.
     *
     * @param copyright the copyright
     * @return this map of options
     */
    public JpackageOptions copyright(String copyright) {
        put("--copyright", copyright);
        return this;
    }

    /**
     * Description of the application.
     *
     * @param description the description
     * @return this map of options
     */
    public JpackageOptions description(String description) {
        put("--description", description);
        return this;
    }

    /**
     * Path where generated output file is placed.
     * <p>
     * Defaults to the current working directory.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions dest(String path) {
        put("--dest", path);
        return this;
    }

    /**
     * Path to a Properties file that contains list of key, value pairs.
     * <p>
     * The keys {@code extension}, {@code mime-type}, {@code icon}, and {@code description} can be used to describe the
     * association.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions fileAssociations(String... path) {
        put("--file-associations", String.join(",", path));
        return this;
    }

    /**
     * Path of the icon of the application package.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions icon(String path) {
        put("--icon", path);
        return this;
    }

    /**
     * Path of the input directory that contains the files to be packaged.
     * <p>
     * All files in the input directory will be packaged into the application image.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions input(String path) {
        put("--input", path);
        return this;
    }

    /**
     * Absolute path of the installation directory of the application.
     *
     * @param path the absolute directory path
     * @return this map of options
     */
    public JpackageOptions installDir(String path) {
        put("--install-dir", path);
        return this;
    }

    /**
     * Options to pass to the Java runtime.
     *
     * @param options the options
     * @return this map of options
     */
    public JpackageOptions javaOptions(String... options) {
        put("--java-options", String.join(" ", options));
        return this;
    }

    /**
     * List of options to pass to jlink.
     * <p>
     * If not specified, defaults to {@link JlinkOptions#stripNativeCommands(boolean) stripNativeCommands}
     * {@link JlinkOptions#stripDebug(boolean) stripDebug} {@link JlinkOptions#noManPages(boolean) noManPages}
     * {@link JlinkOptions#noHeaderFiles(boolean) noHeaderFiles}.
     *
     * @param options the {@link JlinkOptions}
     * @return this map of options
     */
    public JpackageOptions jlinkOptions(JlinkOptions options) {
        put("--jlink-options", String.join(" ", options.toList()));
        return this;
    }

    /**
     * Request to create an installer that will register the main application launcher as a background service-type
     * application.
     * <p>
     * <b>Requires Java 20 or higher</b>.
     *
     * @param launcherAsService {@code true} to register the launcher as a service; {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions launcherAsService(boolean launcherAsService) {
        if (launcherAsService) {
            put("--launcher-as-service");
        } else {
            remove("--launcher-as-service");
        }
        return this;
    }

    /**
     * Path to the license file.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions licenseFile(String path) {
        put("--license-file", path);
        return this;
    }

    /**
     * Group value of the RPM {@code <name>.spec} file or Section value of DEB control file.
     *
     * @param appCategory the application category
     * @return this map of options
     */
    public JpackageOptions linuxAppCategory(String appCategory) {
        put("--linux-app-category", appCategory);
        return this;
    }

    /**
     * Release value of the RPM {@code <name>.spec} file or Debian revision value of the DEB control file.
     *
     * @param appRelease the release value
     * @return this map of options
     */
    public JpackageOptions linuxAppRelease(String appRelease) {
        put("--linux-app-release", appRelease);
        return this;
    }

    /**
     * Maintainer for {@code .deb} package.
     *
     * @param maintainer the maintainer
     * @return this map of options
     */
    public JpackageOptions linuxDebMaintainer(String maintainer) {
        put("--linux-deb-maintainer", maintainer);
        return this;
    }

    /**
     * Menu group this application is placed in.
     *
     * @param menuGroup the menu group
     * @return this map of options
     */
    public JpackageOptions linuxMenuGroup(String menuGroup) {
        put("--linux-menu-group", menuGroup);
        return this;
    }

    /**
     * Required packages or capabilities for the application.
     *
     * @param packageDeps {@code true} if required, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions linuxPackageDeps(boolean packageDeps) {
        if (packageDeps) {
            put("--linux-package-deps");
        } else {
            remove("--linux-package-deps");
        }
        return this;
    }

    /**
     * Name for Linux package, defaults to the application name.
     *
     * @param packageName the package name
     * @return this map of options
     */
    public JpackageOptions linuxPackageName(String packageName) {
        put("--linux-package-name", packageName);
        return this;
    }

    /**
     * Type of the license.
     * <p>
     * {@code License: <value>} of the RPM {@code .spec}
     *
     * @param licenseType the license type
     * @return this map of options
     */
    public JpackageOptions linuxRpmLicenseType(String licenseType) {
        put("--linux-rpm-license-type", licenseType);
        return this;
    }

    /**
     * Creates a shortcut for the application.
     *
     * @param shortcut {@code true| to create a shortcut, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions linuxShortcut(boolean shortcut) {
        if (shortcut) {
            put("--linux-shortcut");
        } else {
            remove("--linux-shortcut");
        }
        return this;
    }

    /**
     * String used to construct {@code LSApplicationCategoryType} in application plist.
     * <p>
     * The default value is {@code utilities}.
     *
     * @param appCategory the category
     * @return this map of options
     */
    public JpackageOptions macAppCategory(String appCategory) {
        put("--mac-app-category", appCategory);
        return this;
    }

    /**
     * Identity used to sign application image.
     * <p>
     * This value will be passed directly to {@code --sign} option of {@code codesign} tool.
     * <p>
     * This option cannot be combined with {@link #macSigningKeyUserName(String) macSignKeyUserName}.
     *
     * @param identity the identity
     * @return this map of options
     */
    public JpackageOptions macAppImageSignIdentity(String identity) {
        put("--mac-app-image-sign-identity", identity);
        return this;
    }

    /**
     * Indicates that the jpackage output is intended for the Mac App Store.
     *
     * @param appStore {@code true} if intended for the Mac App Store, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions macAppStore(boolean appStore) {
        if (appStore) {
            put("--mac-app-store");
        } else {
            remove("--mac-app-store");
        }
        return this;
    }

    /**
     * Include all the referenced content in the dmg.
     *
     * @param additionalContent one or more path
     * @return this map of options
     */
    public JpackageOptions macDmgContent(String... additionalContent) {
        put("--mac-dmg-content", String.join(",", additionalContent));
        return this;
    }

    /**
     * Path to file containing entitlements to use when signing executables and libraries in the bundle.
     *
     * @param path the fie path
     * @return this map of options
     */
    public JpackageOptions macEntitlements(String path) {
        put("--mac-entitlements", path);
        return this;
    }

    /**
     * Identity used to sign "pkg" installer.
     * <p>
     * This value will be passed directly to {@code --sign} option of {@code productbuild} tool.
     * <p>
     * This option cannot be combined with {@link #macSigningKeyUserName(String) macSignKeyUserName}.
     *
     * @param identity the identity
     * @return this map of options
     */
    public JpackageOptions macInstallerSignIdentity(String identity) {
        put("--mac-installer-sign-identity", identity);
        return this;
    }

    /**
     * An identifier that uniquely identifies the application for macOS.
     * <p>
     * Defaults to the main class name.
     * <p>
     * May only use alphanumeric ({@code A-Z,a-z,0-9}), hyphen ({@code -}), and period ({@code .}) characters.
     *
     * @param packageIdentifier the package identifier
     * @return this map of options
     */
    public JpackageOptions macPackageIdentifier(String packageIdentifier) {
        put("--mac-package-identifier", packageIdentifier);
        return this;
    }

    /**
     * Name of the application as it appears in the Menu Bar.
     * <p>
     * This can be different from the application name.
     * <p>
     * This name must be less than 16 characters long and be suitable for displaying in the menu bar and the application
     * Info window.
     * <p>
     * Defaults to the application name.
     *
     * @param name the package name
     * @return this map of options
     */
    public JpackageOptions macPackageName(String name) {
        put("--mac-package-name", name);
        return this;
    }

    /**
     * When signing the application package, this value is prefixed to all components that need to be signed that don't
     * have an existing package identifier.
     *
     * @param prefix the signing prefix
     * @return this map of options
     */
    public JpackageOptions macPackageSigningPrefix(String prefix) {
        put("--mac-package-signing-prefix", prefix);
        return this;
    }

    /**
     * Request that the package or the predefined application image be signed.
     *
     * @param sign {@code true} to sign, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions macSign(boolean sign) {
        if (sign) {
            put("--mac-sign");
        } else {
            remove("--mac-sign");
        }
        return this;
    }

    /**
     * Team or user name portion in Apple signing identities.
     * <p>
     * For direct control of the signing identity used to sign application images or installers use
     * {@link #macAppImageSignIdentity(String) macAppImageSignIdentity} and/or
     * {@link #macInstallerSignIdentity(String) macInstallerSignIdentity}.
     * <p>
     * This option cannot be combined with {@link #macAppImageSignIdentity(String) macAppImageSignIdentity} or
     * {@link #macInstallerSignIdentity(String) macInstallerSignIdentity}.
     *
     * @param username the username
     * @return this map of options
     */
    public JpackageOptions macSigningKeyUserName(String username) {
        put("--mac-signing-key-user-name", username);
        return this;
    }

    /**
     * Name of the keychain to search for the signing identity.
     * <p>
     * If not specified, the standard keychains are used.
     *
     * @param keychain the keychain name
     * @return this map of options
     */
    public JpackageOptions macSigningKeychain(String keychain) {
        put("--mac-signing-keychain", keychain);
        return this;
    }

    /**
     * Qualified name of the application main class to execute.
     * <p>
     * This option can only be used if {@link #mainJar(String) mainJar} is specified.
     *
     * @param mainClass the main class
     * @return this map of options
     */
    public JpackageOptions mainClass(String mainClass) {
        put("--main-class", mainClass);
        return this;
    }

    /**
     * The main JAR of the application; containing the main class.
     * <p>
     * Either {@link #module(String, String) module} or {@link #mainJar(String) mainJar} option can be specified but
     * not both.
     *
     * @param jar the path relative to the input path
     * @return this map of options
     */
    @SuppressWarnings("JavadocDeclaration")
    public JpackageOptions mainJar(String jar) {
        put("--main-jar", jar);
        return this;
    }

    /**
     * The main module and main class of the application.
     * <p>
     * This module must be located on the {@link #modulePath(String...) module path}.
     * <p>
     * When this option is specified, the main module will be linked in the Java runtime image.
     * <p>
     * Either {@link #module(String, String) module} or {@link #mainJar(String) mainJar} option can be specified but
     * not both.
     *
     * @param name the module name
     * @return this map of options
     */
    public JpackageOptions module(String name) {
        put("--module", name);
        return this;
    }

    /**
     * The main module and main class of the application.
     * <p>
     * This module must be located on the {@link #modulePath(String...) module path}.
     * <p>
     * When this option is specified, the main module will be linked in the Java runtime image.
     * <p>
     * Either {@link #module(String, String) module} or {@link #mainJar(String) mainJar} option can be specified but
     * not both.
     *
     * @param name      the module name
     * @param mainClass the main class
     * @return this map of options
     */
    @SuppressWarnings("JavadocDeclaration")
    public JpackageOptions module(String name, String mainClass) {
        put("--module-name", name + "/" + mainClass);
        return this;
    }

    /**
     * List of module paths.
     * <p>
     * Each path is either a directory of modules or the path to a modular jar.
     * <p>
     * Each path is absolute or relative to the current directory.
     *
     * @param path one or more path
     * @return this map of options
     */
    public JpackageOptions modulePath(String... path) {
        put("--module-path", String.join(":", path));
        return this;
    }

    /**
     * Name of the application and/or package.
     *
     * @param name the name
     * @return this map of options
     */
    public JpackageOptions name(String name) {
        put("--name", name);
        return this;
    }

    /**
     * Associates {@code null} with the specified key in this map. If the map previously contained a mapping for the
     * key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated
     */
    public void put(String key) {
        put(key, null);
    }

    /**
     * Path to override jpackage resources.
     * <p>
     * Icons, template files, and other resources of jpackage can be over-ridden by adding replacement resources to
     * this directory.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions resourceDir(String path) {
        put("--resource-dir", path);
        return this;
    }

    /**
     * Path of the predefined runtime image that will be copied into the application image.
     * <p>
     * If not specified, jpackage will run jlink to create the runtime image using options:
     * {@link JlinkOptions#stripNativeCommands(boolean) stripNativeCommands}
     * {@link JlinkOptions#stripDebug(boolean) stripDebug} {@link JlinkOptions#noManPages(boolean) noManPages}
     * {@link JlinkOptions#noHeaderFiles(boolean) noHeaderFiles}
     * <p>
     * Option is required when creating a runtime package.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions runtimeImage(String path) {
        put("--runtime-image", path);
        return this;
    }

    /**
     * Strip debug information.
     *
     * @param stripDebug {@code true} to strip debug info, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions stripDebug(boolean stripDebug) {
        if (stripDebug) {
            put("--strip-debug");
        } else {
            remove("--strip-debug");
        }
        return this;
    }

    /**
     * Path of a new or empty directory used to create temporary files.
     * <p>
     * If specified, the temp dir will not be removed upon the task completion and must be removed manually.
     * <p>
     * If not specified, a temporary directory will be created and removed upon the task completion.
     *
     * @param path absolute path or relative to the current directory
     * @return this map of options
     */
    public JpackageOptions temp(String path) {
        put("--temp", path);
        return this;
    }

    /**
     * The type of package to create.
     * <p>
     * If this option is not specified a platform dependent default type will be created.
     *
     * @param type the package type
     * @return this map of options
     */
    public JpackageOptions type(PackageType type) {
        put("--type", type.type);
        return this;
    }

    /**
     * Vendor of the application.
     *
     * @param vendor the vendor
     * @return this map of options
     */
    public JpackageOptions vendor(String vendor) {
        put("--vendor", vendor);
        return this;
    }

    /**
     * Enables verbose output.
     *
     * @param verbose {@code true} to enable verbose tracing, {@code false} otherwise.
     * @return this map of options
     */
    public JpackageOptions verbose(boolean verbose) {
        if (verbose) {
            put("--verbose");
        } else {
            remove("--verbose");
        }
        return this;
    }

    /**
     * Creates a console launcher for the application, should be specified for application which requires console
     * interactions.
     *
     * @param winConsole {@code true} to create a console launcher, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions winConsole(boolean winConsole) {
        if (winConsole) {
            put("--win-console");
        } else {
            remove("--win-console");
        }
        return this;
    }

    /**
     * Adds a dialog to enable the user to choose a directory in which the product is installed.
     *
     * @param winDirChooser {@code true} to let the user choose a directory, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions winDirChooser(boolean winDirChooser) {
        if (winDirChooser) {
            put("--win-dir-chooser");
        } else {
            remove("--win-dir-chooser");
        }
        return this;
    }

    /**
     * URL where user can obtain further information or technical support.
     *
     * @param helpUrl the help URL
     * @return this map of options
     */
    public JpackageOptions winHelpUrl(String helpUrl) {
        put("--win-help-url", helpUrl);
        return this;
    }

    /**
     * Request to add a Start Menu shortcut for this application.
     *
     * @param winMenu {@code true} to add a start menu shortcut, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions winMenu(boolean winMenu) {
        if (winMenu) {
            put("--win-menu");
        } else {
            remove("--win-menu");
        }
        return this;
    }

    /**
     * Start Menu group this application is placed in.
     *
     * @param menuGroup the menu group
     * @return this map of options
     */
    public JpackageOptions winMenuGroup(String menuGroup) {
        put("--win-menu-group", menuGroup);
        return this;
    }

    /**
     * Request to perform an install on a per-user basis.
     *
     * @param winPerUserInstall {@code true} for per-user install, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions winPerUserInstall(boolean winPerUserInstall) {
        if (winPerUserInstall) {
            put("--win-per-user-install");
        } else {
            remove("--win-per-user-install");
        }
        return this;
    }

    /**
     * Request to create a desktop shortcut for this application.
     *
     * @param winShortcut {@code true} to create a shortcut, {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions winShortcut(boolean winShortcut) {
        if (winShortcut) {
            put("--win-shortcut");
        } else {
            remove("--win-shortcut");
        }
        return this;
    }

    /**
     * Adds a dialog to enable the user to choose if shortcuts will be created by installer.
     *
     * @param shortcutPrompt {@code true} to add a prompt; {@code false} otherwise
     * @return this map of options
     */
    public JpackageOptions winShortcutPrompt(boolean shortcutPrompt) {
        if (shortcutPrompt) {
            put("--win-shortcut-prompt");
        } else {
            remove("--win-shortcut-prompt");
        }
        return this;
    }

    /**
     * URL of available application update information.
     *
     * @param url the URL
     * @return this map of options
     */
    public JpackageOptions winUpdateUrl(String url) {
        put("--win-update-url", url);
        return this;
    }

    /**
     * UUID associated with upgrades for this package.
     *
     * @param uuid the uuid
     * @return this map of options
     */
    public JpackageOptions winUpgradeUuid(String uuid) {
        put("--win-upgrade-uuid", uuid);
        return this;
    }

    /**
     * The package types.
     */
    public enum PackageType {
        APP_IMAGE("app_image"),
        DEB("deb"),
        DMG("dmg"),
        EXE("exe"),
        MSI("msi"),
        PKG("pkg"),
        RPM("rpm");

        final String type;

        PackageType(String type) {
            this.type = type;
        }
    }

    /**
     * Name of launcher, and a path to a Properties file that contains a list of key, value pairs.
     * <p>
     * The keys {@code module}, {@code main-jar}, {@code main-class}, {@code description},
     * {@code arguments}, {@code java-options}, {@code app-version}, {@code icon},
     * {@code launcher-as-service}, {@code win-console}, {@code win-shortcut}, {@code win-menu},
     * {@code linux-app-category}, and {@code linux-shortcut} can be used.
     * <p>
     * These options are added to, or used to overwrite, the original command line options to build an additional
     * alternative launcher.
     *
     * @param name the name
     * @param path absolute path or relative to the current directory
     */
    public record Launcher(String name, String path) {
    }
}
