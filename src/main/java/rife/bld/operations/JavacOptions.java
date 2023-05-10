/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.tools.Convert;
import rife.tools.FileUtils;
import rife.tools.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Options for the standard javac tool.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.18
 */
public class JavacOptions extends ArrayList<String> {
    public enum DebuggingInfo {
        ALL, NONE, LINES, VAR, SOURCE
    }

    public enum Implicit {
        NONE, CLASS
    }

    public enum Processing {
        NONE, ONLY
    }

    /**
     * Option to pass to annotation processors
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions annotationOption(String key, String value) {
        add("-A" + key + "=" + value);
        return this;
    }

    /**
     * Root modules to resolve in addition to the initial modules,
     * or all modules on the module path if a module is
     * ALL-MODULE-PATH.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions addModules(String... modules) {
        return addModules(Arrays.asList(modules));
    }

    /**
     * Root modules to resolve in addition to the initial modules,
     * or all modules on the module path if a module is
     * ALL-MODULE-PATH.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions addModules(List<String> modules) {
        add("--add-modules");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Specify character encoding used by source files
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions encoding(String name) {
        add("-encoding");
        add("name");
        return this;
    }

    /**
     * Output source locations where deprecated APIs are used
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions deprecation() {
        add("-deprecation");
        return this;
    }

    /**
     * Enable preview language features. To be used in conjunction with {@link #release}.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions enablePreview() {
        add("--enable-preview");
        return this;
    }

    /**
     * Override location of endorsed standards path
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions endorsedDirs(File... dirs) {
        return endorsedDirs(Arrays.asList(dirs));
    }

    /**
     * Override location of endorsed standards path
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions endorsedDirs(List<File> dirs) {
        add("-endorseddirs");
        add(dirs.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions extDirs(File... dirs) {
        return extDirs(Arrays.asList(dirs));
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions extDirs(List<File> dirs) {
        add("-extdirs");
        add(dirs.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Indicates whether the Java SE release was set.
     *
     * @return {@code true} if the release was set; or
     * {@code false} otherwise
     * @since 1.5.18
     */
    public boolean containsRelease() {
        return contains("-release");
    }

    /**
     * Compile for the specified Java SE release.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions release(int version) {
        add("--release");
        add(Convert.toString(version));
        return this;
    }

    /**
     * Generate debugging info
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions debuggingInfo(DebuggingInfo option) {
        if (option.equals(DebuggingInfo.ALL)) {
            add("-g");
        } else {
            add("-g:" + option.name().toLowerCase());
        }
        return this;
    }

    /**
     * Specify where to place generated native header files
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions nativeHeaders(File path) {
        add("-h");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Specify whether or not to generate class files for implicitly referenced files
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions implicit(Implicit option) {
        add("-implicit:" + option.name().toLowerCase());
        return this;
    }

    /**
     * Limit the universe of observable modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions limitModules(String... modules) {
        return limitModules(Arrays.asList(modules));
    }

    /**
     * Limit the universe of observable modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions limitModules(List<String> modules) {
        add("--limit-modules");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Compile only the specified module(s), check timestamps
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions module(String... modules) {
        return module(Arrays.asList(modules));
    }

    /**
     * Compile only the specified module(s), check timestamps
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions module(List<String> modules) {
        add("--module");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions modulePath(File... paths) {
        return modulePath(Arrays.asList(paths));
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 1.6.2
     */
    public JavacOptions modulePath(List<File> paths) {
        add("--module-path");
        add(FileUtils.joinPaths(paths.stream().map(File::getAbsolutePath).toList()));
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions moduleSourcePath(File path) {
        add("--module-source-path");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Specify version of modules that are being compiled
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions moduleVersion(String version) {
        add("--module-version");
        add(version);
        return this;
    }

    /**
     * Generate no warnings
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions noWarn() {
        add("-nowarn");
        return this;
    }

    /**
     * Generate metadata for reflection on method parameters
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions parameters() {
        add("-parameters");
        return this;
    }

    /**
     * Control whether annotation processing and/or compilation is done.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions process(Processing option) {
        add("-proc:" + option.name().toLowerCase());
        return this;
    }

    /**
     * Names of the annotation processors to run; bypasses default discovery process
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions processors(String... classnames) {
        return processors(Arrays.asList(classnames));
    }

    /**
     * Names of the annotation processors to run; bypasses default discovery process
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions processors(List<String> classnames) {
        add("-processor");
        add(StringUtils.join(classnames, ","));
        return this;
    }

    /**
     * Specify a module path where to find annotation processors
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions processorModulePath(File path) {
        add("--processor-module-path");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Specify where to find annotation processors
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions processorPath(File path) {
        add("--processor-path");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Check that API used is available in the specified profile
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions profile(String profile) {
        add("-profile");
        add(profile);
        return this;
    }

    /**
     * Override location of system modules. Option is &lt;jdk&gt; or none.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions system(String option) {
        add("--system");
        add(option);
        return this;
    }

    /**
     * Override location of upgradeable modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions upgradeModulePath(File path) {
        add("--upgrade-module-path");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Terminate compilation if warnings occur
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions warningError() {
        add("-Werror");
        return this;
    }
}