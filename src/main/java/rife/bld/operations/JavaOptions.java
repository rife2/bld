/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.tools.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    public JavaOptions modulePath(File... modules) {
        return modulePath(Arrays.asList(modules));
    }

    /**
     * A list of directories, each directory is a directory of modules.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions modulePath(List<File> modules) {
        add("--module-path");
        add(StringUtils.join(modules, ":"));
        return this;
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions upgradeModulePath(File... modulePath) {
        return upgradeModulePath(Arrays.asList(modulePath));
    }

    /**
     * List of directories, each directory is a directory of modules
     * that replace upgradeable modules in the runtime image
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions upgradeModulePath(List<File> modulePath) {
        add("--upgrade-module-path");
        add(StringUtils.join(modulePath, ":"));
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
        return addModules(Arrays.asList(modules));
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
    public JavaOptions enableNativeAccess(List... modules) {
        return enableNativeAccess(Arrays.asList(modules));
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
        return agentLib(libName, null);
    }

    /**
     * Load native agent library.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions agentLib(String libName, String options) {
        add("-agentlib:" + libName + (options == null ? "" : ":" + options));
        return this;
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions agentPath(File pathName) {
        return agentPath(pathName, null);
    }

    /**
     * Load native agent library by full pathname.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions agentPath(File pathName, String options) {
        add("-agentpath:" + pathName + (options == null ? "" : ":" + options));
        return this;
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions javaAgent(File jarPath) {
        return javaAgent(jarPath, null);
    }

    /**
     * Load Java programming language agent.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavaOptions javaAgent(File jarPath, String options) {
        add("-javaagent:" + jarPath + (options == null ? "" : ":" + options));
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