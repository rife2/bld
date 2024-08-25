/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.tools.FileUtils;
import rife.tools.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Options for the standard java tool.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.18
 */
public class JavaOptions extends ArrayList<String> {
    public enum Verbose {
        CLASS, MODULE, GC, JNI
    }

    /**
     * Select the "truffle" VM.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions truffle() {
        add("-truffle");
        return this;
    }

    /**
     * A list of directories, each directory is a directory of modules.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions modulePath(File... paths) {
        return modulePath(List.of(paths));
    }

    /**
     * A list of directories, each directory is a directory of modules.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions modulePath(List<File> paths) {
        return modulePathStrings(paths.stream().map(File::getAbsolutePath).toList());
    }

    /**
     * A list of directories, each directory is a directory of modules.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions modulePath(Path... paths) {
        return modulePathPaths(List.of(paths));
    }

    /**
     * A list of directories, each directory is a directory of modules.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions modulePathPaths(List<Path> paths) {
        return modulePath(paths.stream().map(Path::toFile).toList());
    }

    /**
     * A list of directories, each directory is a directory of modules.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions modulePath(String... paths) {
        return modulePathStrings(List.of(paths));
    }

    /**
     * A list of directories, each directory is a directory of modules.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions modulePathStrings(List<String> paths) {
        add("--module-path");
        add(FileUtils.joinPaths(paths));
        return this;
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions upgradeModulePath(File... paths) {
        return upgradeModulePath(List.of(paths));
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions upgradeModulePath(List<File> paths) {
        return upgradeModulePathStrings(paths.stream().map(File::getAbsolutePath).toList());
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions upgradeModulePath(Path... paths) {
        return upgradeModulePathPaths(List.of(paths));
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions upgradeModulePathPaths(List<Path> paths) {
        return upgradeModulePath(paths.stream().map(Path::toFile).toList());
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions upgradeModulePath(String... paths) {
        return upgradeModulePathStrings(List.of(paths));
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions upgradeModulePathStrings(List<String> paths) {
        add("--upgrade-module-path");
        add(FileUtils.joinPaths(paths));
        return this;
    }

    /**
     * Root modules to resolve in addition to the initial module.
     * The module name can also be ALL-DEFAULT, ALL-SYSTEM,
     * ALL-MODULE-PATH.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions addModules(String... modules) {
        return addModules(List.of(modules));
    }

    /**
     * Root modules to resolve in addition to the initial module.
     * The module name can also be ALL-DEFAULT, ALL-SYSTEM,
     * ALL-MODULE-PATH.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions addModules(List<String> modules) {
        add("--add-modules");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Modules that are permitted to perform restricted native operations.
     * The module name can also be ALL-UNNAMED.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions enableNativeAccess(String... modules) {
        return enableNativeAccess(List.of(modules));
    }

    /**
     * Modules that are permitted to perform restricted native operations.
     * The module name can also be ALL-UNNAMED.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions enableNativeAccess(List<String> modules) {
        add("--enable-native-access");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Set a system property.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions property(String key, String value) {
        add("-D" + key + "=" + value);
        return this;
    }

    /**
     * Enable verbose output for the given subsystem
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions verbose(Verbose verbose) {
        add("-verbose:" + verbose.name().toLowerCase());
        return this;
    }

    /**
     * Show module resolution output during startup.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions showModuleResolution() {
        add("--show-module-resolution");
        return this;
    }

    /**
     * Enable assertions with specified granularity, either
     * package name or class name.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions enableAssertions(String name) {
        add("-enableassertions:"+name);
        return this;
    }

    /**
     * Disable assertions with specified granularity, either
     * package name or class name.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions disableAssertions(String name) {
        add("-disableassertions:"+name);
        return this;
    }

    /**
     * Enable system assertions.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions enableSystemAssertions() {
        add("-enablesystemassertions");
        return this;
    }

    /**
     * Disable system assertions.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions disableSystemAssertions() {
        add("-disablesystemassertions");
        return this;
    }

    /**
     * Load native agent library.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions agentLib(String libName) {
        return agentLib(libName, (String)null);
    }

    /**
     * Load native agent library.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions agentLib(String libName, String options) {
        add("-agentlib:" + libName + (options == null ? "" : "=" + options));
        return this;
    }

    /**
     * Load native agent library.
     *
     * @return this list of options
     * @since 1.7.1
     */
    public JavaOptions agentLib(String libName, String... options) {
        return agentLib(libName, List.of(options));
    }

    /**
     * Load native agent library.
     *
     * @return this list of options
     * @since 1.7.1
     */
    public JavaOptions agentLib(String libName, List<String> options) {
        add("-agentlib:" + libName + (options == null || options.isEmpty() ? "" : "=" + StringUtils.join(options, ",")));
        return this;
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions agentPath(File pathName) {
        return agentPath(pathName.getAbsolutePath(), (String)null);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions agentPath(File pathName, String options) {
        return agentPath(pathName.getAbsolutePath(), options);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 1.7.1
     */
    public JavaOptions agentPath(File pathName, String... options) {
        return agentPath(pathName.getAbsolutePath(), List.of(options));
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 1.7.1
     */
    public JavaOptions agentPath(File pathName, List<String> options) {
        return agentPath(pathName.getAbsolutePath(), options);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(Path pathName) {
        return agentPath(pathName.toFile(), (String)null);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(Path pathName, String options) {
        return agentPath(pathName.toFile(), options);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(Path pathName, String... options) {
        return agentPath(pathName.toFile(), List.of(options));
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(Path pathName, List<String> options) {
        return agentPath(pathName.toFile(), options);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(String pathName) {
        return agentPath(pathName, (String)null);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(String pathName, String options) {
        add("-agentpath:" + pathName + (options == null ? "" : "=" + options));
        return this;
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(String pathName, String... options) {
        return agentPath(pathName, List.of(options));
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions agentPath(String pathName, List<String> options) {
        add("-agentpath:" + pathName + (options == null || options.isEmpty() ? "" : "=" + StringUtils.join(options, ",")));
        return this;
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions javaAgent(File jarPath) {
        return javaAgent(jarPath.getAbsolutePath(), (String)null);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions javaAgent(File jarPath, String options) {
        return javaAgent(jarPath.getAbsolutePath(), options);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 1.7.1
     */
    public JavaOptions javaAgent(File jarPath, String... options) {
        return javaAgent(jarPath.getAbsolutePath(), List.of(options));
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 1.7.1
     */
    public JavaOptions javaAgent(File jarPath, List<String> options) {
        return javaAgent(jarPath.getAbsolutePath(), options);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(Path jarPath) {
        return javaAgent(jarPath.toFile(), (String)null);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(Path jarPath, String options) {
        return javaAgent(jarPath.toFile(), options);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(Path jarPath, String... options) {
        return javaAgent(jarPath.toFile(), List.of(options));
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(Path jarPath, List<String> options) {
        return javaAgent(jarPath.toFile(), options);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(String jarPath) {
        return javaAgent(jarPath, (String)null);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(String jarPath, String options) {
        add("-javaagent:" + jarPath + (options == null ? "" : "=" + options));
        return this;
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(String jarPath, String... options) {
        return javaAgent(jarPath, List.of(options));
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 2.1
     */
    public JavaOptions javaAgent(String jarPath, List<String> options) {
        add("-javaagent:" + jarPath + (options == null || options.isEmpty() ? "" : "=" + StringUtils.join(options, ",")));
        return this;
    }

    /**
     * Allow classes to depend on preview features of this release
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions enablePreview() {
        add("--enable-preview");
        return this;
    }

    /**
     * Set the initial Java heap size in megabytes.
     *
     * @param megabytes the size
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions initialHeapSize(int megabytes) {
        add("-Xms" + megabytes + "m");
        return this;
    }

    /**
     * Set the maximum Java heap size in megabytes.
     *
     * @param megabytes the size
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions maximumHeapSize(int megabytes) {
        add("-Xmx" + megabytes + "m");
        return this;
    }
}