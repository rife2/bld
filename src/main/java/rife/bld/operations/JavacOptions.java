/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.tools.Convert;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static rife.bld.operations.CompileOperation.COMPILE_OPTION_MODULE_PATH;

/**
 * Options for the standard javac tool.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.18
 */
@SuppressWarnings("UnusedReturnValue")
public class JavacOptions extends ArrayList<String> {
    public enum DebuggingInfo {
        ALL, NONE, LINES, VAR, SOURCE
    }

    public enum Implicit {
        NONE, CLASS
    }

    public enum Processing {
        FULL, NONE, ONLY
    }

    public enum XLintKey {
        ALL,
        AUXILIARYCLASS,
        CAST,
        CLASSFILE,
        DANGLING_DOC_COMMENTS,
        DEP_ANN,
        DEPRECATION,
        DIVZERO,
        EMPTY,
        EXPORTS,
        FALLTHROUGH,
        FINALLY,
        IDENTITY,
        INCUBATING,
        LOSSY_CONVERSIONS,
        MISSING_EXPLICIT_CTOR,
        MODULE,
        NONE,
        OPENS,
        OPTIONS,
        OUTPUT_FILE_CLASH,
        OVERLOADS,
        OVERRIDES,
        PATH,
        PREVIEW,
        PROCESSING,
        RAWTYPES,
        REMOVAL,
        REQUIRES_AUTOMATIC,
        REQUIRES_TRANSITIVE_AUTOMATIC,
        RESTRICTED,
        SERIAL,
        STATIC,
        STRICTFP,
        SYNCHRONIZATION,
        TEXT_BLOCKS,
        THIS_ESCAPE,
        TRY,
        UNCHECKED,
        VARARGS
    }

    // Helper method to check if an array is not empty
    private static <T> boolean isNotEmpty(T[] array) {
        return array != null && array.length > 0;
    }

    // Helper method to check if a collection is not empty
    private static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Helper method to add path-based options
     */
    private JavacOptions addPathOption(String option, Collection<String> paths) {
        if (isNotEmpty(paths)) {
            add(option);
            add(String.join(File.pathSeparator, paths));
        }
        return this;
    }

    /**
     * Helper method to add comma-separated options
     */
    private JavacOptions addCommaSeparatedOption(String option, Collection<String> values) {
        if (isNotEmpty(values)) {
            add(option);
            add(String.join(",", values));
        }
        return this;
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
     * Specifies a package to be considered as exported from its defining
     * module to additional modules or to all unnamed modules when the value
     * of other-module is ALL-UNNAMED
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions addExports(String... modules) {
        if (isNotEmpty(modules)) {
            addExports(Arrays.asList(modules));
        }
        return this;
    }

    /**
     * Specifies a package to be considered as exported from its defining
     * module to additional modules or to all unnamed modules when the value
     * of other-module is ALL-UNNAMED
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions addExports(Collection<String> modules) {
        if (isNotEmpty(modules)) {
            return addCommaSeparatedOption("--add-exports", modules);
        }
        return this;
    }

    /**
     * Specifies additional modules to be considered as required by a given module
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions addReads(String... modules) {
        if (isNotEmpty(modules)) {
            addReads(Arrays.asList(modules));
        }
        return this;
    }

    /**
     * Specifies additional modules to be considered as required by a given module
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions addReads(Collection<String> modules) {
        if (isNotEmpty(modules)) {
            return addCommaSeparatedOption("--add-reads", modules);
        }
        return this;
    }

    /**
     * Root modules to resolve in addition to the initial modules,
     * or all modules on the module path if a module is ALL-MODULE-PATH
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions addModules(String... modules) {
        if (isNotEmpty(modules)) {
            addModules(Arrays.asList(modules));
        }
        return this;
    }

    /**
     * Root modules to resolve in addition to the initial modules,
     * or all modules on the module path if a module is ALL-MODULE-PATH
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions addModules(Collection<String> modules) {
        if (isNotEmpty(modules)) {
            return addCommaSeparatedOption("--add-modules", modules);
        }
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
        add(name);
        return this;
    }

    /**
     * Fallback target module for files created by annotation processors,
     * if none specified or inferred
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions defaultModuleForCreatedFiles(String module) {
        add("--default-module-for-created-files");
        add(module);
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
     * Enable preview language features. To be used in conjunction with {@link #release}
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
        if (isNotEmpty(dirs)) {
            endorsedDirs(Arrays.asList(dirs));
        }
        return this;
    }

    /**
     * Override location of endorsed standards path
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions endorsedDirs(Collection<File> dirs) {
        if (isNotEmpty(dirs)) {
            return endorsedDirsStrings(dirs.stream().map(File::getAbsolutePath).toList());
        }
        return this;
    }

    /**
     * Override location of endorsed standards path
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions endorsedDirs(Path... dirs) {
        if (isNotEmpty(dirs)) {
            endorsedDirsPaths(Arrays.asList(dirs));
        }
        return this;
    }

    /**
     * Override location of endorsed standards path
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions endorsedDirsPaths(Collection<Path> dirs) {
        if (isNotEmpty(dirs)) {
            return endorsedDirs(dirs.stream().map(Path::toFile).toList());
        }
        return this;
    }

    /**
     * Override location of endorsed standards path
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions endorsedDirs(String... dirs) {
        if (isNotEmpty(dirs)) {
            endorsedDirsStrings(Arrays.asList(dirs));
        }
        return this;
    }

    /**
     * Override location of endorsed standards path
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions endorsedDirsStrings(Collection<String> dirs) {
        if (isNotEmpty(dirs)) {
            add("-endorseddirs");
            add(String.join(",", dirs));
        }
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions extDirs(File... dirs) {
        if (isNotEmpty(dirs)) {
            extDirs(Arrays.asList(dirs));
        }
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions extDirs(Collection<File> dirs) {
        if (isNotEmpty(dirs)) {
            return extDirsStrings(dirs.stream().map(File::getAbsolutePath).toList());
        }
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions extDirs(Path... dirs) {
        if (isNotEmpty(dirs)) {
            extDirsPaths(Arrays.asList(dirs));
        }
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions extDirsPaths(Collection<Path> dirs) {
        if (isNotEmpty(dirs)) {
            return extDirs(dirs.stream().map(Path::toFile).toList());
        }
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions extDirs(String... dirs) {
        if (isNotEmpty(dirs)) {
            extDirsStrings(Arrays.asList(dirs));
        }
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions extDirsStrings(Collection<String> dirs) {
        if (isNotEmpty(dirs)) {
            add("-extdirs");
            add(String.join(",", dirs));
        }
        return this;
    }

    /**
     * Indicates whether the Java SE release was set
     *
     * @return {@code true} if the release was set; or
     * {@code false} otherwise
     * @since 1.5.18
     */
    public boolean containsRelease() {
        return contains("--release");
    }

    /**
     * Overrides or augments a module with classes and resources in JAR files or directories
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions patchModule(String module) {
        add("--patch-module");
        add(module);
        return this;
    }

    /**
     * Compile for the specified Java SE release
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
     * Provide source compatibility with the specified Java SE release
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions source(int version) {
        add("--source");
        add(Convert.toString(version));
        return this;
    }

    /**
     * Generate class files suitable for the specified Java SE release
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions target(int version) {
        add("--target");
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
        return nativeHeaders(path.getAbsolutePath());
    }

    /**
     * Specify where to place generated native header files
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions nativeHeaders(Path path) {
        return nativeHeaders(path.toFile());
    }

    /**
     * Specify where to place generated native header files
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions nativeHeaders(String path) {
        add("-h");
        add(path);
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
        if (isNotEmpty(modules)) {
            limitModules(Arrays.asList(modules));
        }
        return this;
    }

    /**
     * Limit the universe of observable modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions limitModules(Collection<String> modules) {
        if (isNotEmpty(modules)) {
            return addCommaSeparatedOption("--limit-modules", modules);
        }
        return this;
    }

    /**
     * Compile only the specified module(s), check timestamps
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions module(String... modules) {
        if (isNotEmpty(modules)) {
            module(Arrays.asList(modules));
        }
        return this;
    }

    /**
     * Compile only the specified module(s), check timestamps
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions module(Collection<String> modules) {
        if (isNotEmpty(modules)) {
            return addCommaSeparatedOption("--module", modules);
        }
        return this;
    }

    /**
     * /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions modulePath(File... paths) {
        if (isNotEmpty(paths)) {
            modulePath(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 1.6.2
     */
    public JavacOptions modulePath(Collection<File> paths) {
        if (isNotEmpty(paths)) {
            return modulePathStrings(paths.stream().map(File::getAbsolutePath).toList());
        }
        return this;
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions modulePath(Path... paths) {
        if (isNotEmpty(paths)) {
            modulePathPaths(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions modulePathPaths(Collection<Path> paths) {
        if (isNotEmpty(paths)) {
            return modulePathStrings(paths.stream().map(Path::toString).toList());
        }
        return this;
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions modulePath(String... paths) {
        if (isNotEmpty(paths)) {
            modulePathStrings(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions modulePathStrings(Collection<String> paths) {
        if (isNotEmpty(paths)) {
            return addPathOption(COMPILE_OPTION_MODULE_PATH, paths);
        }
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions moduleSourcePathStrings(Collection<String> paths) {
        if (isNotEmpty(paths)) {
            return addPathOption("--module-source-path", paths);
        }
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions moduleSourcePathPaths(Collection<Path> paths) {
        if (isNotEmpty(paths)) {
            return moduleSourcePathStrings(paths.stream().map(Path::toString).toList());
        }
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions moduleSourcePath(Collection<File> paths) {
        if (isNotEmpty(paths)) {
            return moduleSourcePathStrings(paths.stream().map(File::getPath).toList());
        }
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions moduleSourcePath(File... paths) {
        if (isNotEmpty(paths)) {
            moduleSourcePath(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions moduleSourcePath(String... paths) {
        if (isNotEmpty(paths)) {
            moduleSourcePathStrings(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions moduleSourcePath(Path... paths) {
        if (isNotEmpty(paths)) {
            moduleSourcePathPaths(Arrays.asList(paths));
        }
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
     * Control whether annotation processing and/or compilation is done
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
        if (isNotEmpty(classnames)) {
            processors(Arrays.asList(classnames));
        }
        return this;
    }

    /**
     * Names of the annotation processors to run; bypasses default discovery process
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions processors(Collection<String> classnames) {
        if (isNotEmpty(classnames)) {
            return addCommaSeparatedOption("-processor", classnames);
        }
        return this;
    }

    /**
     * Specify a module path where to find annotation processors
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions processorModulePath(File... paths) {
        if (isNotEmpty(paths)) {
            processorModulePath(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify a module path where to find annotation processors
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions processorModulePath(Path... paths) {
        if (isNotEmpty(paths)) {
            processorModulePathPaths(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify a module path where to find annotation processors
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions processorModulePath(String... paths) {
        if (isNotEmpty(paths)) {
            processorModulePathStrings(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify a module path where to find annotation processors
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions processorModulePathPaths(Collection<Path> paths) {
        if (isNotEmpty(paths)) {
            return processorModulePathStrings(paths.stream().map(Path::toString).toList());
        }
        return this;
    }

    /**
     * Specify a module path where to find annotation processors
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions processorModulePathStrings(Collection<String> paths) {
        if (isNotEmpty(paths)) {
            return addPathOption("--processor-module-path", paths);
        }
        return this;
    }

    /**
     * Specify a module path where to find annotation processors
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions processorModulePath(Collection<File> paths) {
        if (isNotEmpty(paths)) {
            return processorModulePathStrings(paths.stream().map(File::getAbsolutePath).toList());
        }
        return this;
    }

    /**
     * Specify where to find annotation processors
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavacOptions processorPath(String... paths) {
        if (isNotEmpty(paths)) {
            processorPathStrings(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find annotation processors
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions processorPath(File... paths) {
        if (isNotEmpty(paths)) {
            processorPath(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find annotation processors
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions processorPath(Path... paths) {
        if (isNotEmpty(paths)) {
            processorPathPaths(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Specify where to find annotation processors
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions processorPathStrings(Collection<String> paths) {
        if (isNotEmpty(paths)) {
            return addPathOption("--processor-path", paths);
        }
        return this;
    }

    /**
     * Specify where to find annotation processors
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions processorPath(Collection<File> paths) {
        if (isNotEmpty(paths)) {
            return processorPathStrings(paths.stream().map(File::getAbsolutePath).toList());
        }
        return this;
    }

    /**
     * Specify where to find annotation processors
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions processorPathPaths(Collection<Path> paths) {
        if (isNotEmpty(paths)) {
            return processorPathStrings(paths.stream().map(Path::toString).toList());
        }
        return this;
    }

    /**
     * Check that the API used is available in the specified profile
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
     * Specify the directory used to place the generated source files.
     *
     * @param path the source output directory path
     * @return the list of options
     * @since 2.1.1
     */
    public JavacOptions sourceOutput(String path) {
        add("-s");
        add(path);
        return this;
    }

    /**
     * Specify the directory used to place the generated source files.
     *
     * @param path the source output directory path
     * @return the list of options
     * @since 2.1.1
     */
    public JavacOptions sourceOutput(File path) {
        return sourceOutput(path.getAbsolutePath());
    }

    /**
     * Specify the directory used to place the generated source files.
     *
     * @param path the source output directory path
     * @return the list of options
     * @since 2.1.1
     */
    public JavacOptions sourceOutput(Path path) {
        return sourceOutput(path.toFile());
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
    public JavacOptions upgradeModulePath(File... paths) {
        if (isNotEmpty(paths)) {
            upgradeModulePath(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Override location of upgradeable modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions upgradeModulePath(Path... paths) {
        if (isNotEmpty(paths)) {
            upgradeModulePathPaths(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Override location of upgradeable modules
     *
     * @return this list of options
     * @since 2.1
     */
    public JavacOptions upgradeModulePath(String... paths) {
        if (isNotEmpty(paths)) {
            upgradeModulePathStrings(Arrays.asList(paths));
        }
        return this;
    }

    /**
     * Override location of upgradeable modules
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions upgradeModulePathStrings(Collection<String> paths) {
        if (isNotEmpty(paths)) {
            return addPathOption("--upgrade-module-path", paths);
        }
        return this;
    }

    /**
     * Override location of upgradeable modules
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions upgradeModulePath(Collection<File> paths) {
        if (isNotEmpty(paths)) {
            return upgradeModulePathStrings(paths.stream().map(File::getAbsolutePath).toList());
        }
        return this;
    }

    /**
     * Override location of upgradeable modules
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions upgradeModulePathPaths(Collection<Path> paths) {
        if (isNotEmpty(paths)) {
            return upgradeModulePathStrings(paths.stream().map(Path::toString).toList());
        }
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

    /**
     * Enable recommended warning categories
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions xLint() {
        add("-Xlint");
        return this;
    }

    /**
     * Warning categories to enable
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions xLint(XLintKey... keys) {
        if (isNotEmpty(keys)) {
            xLint(Arrays.asList(keys));
        }
        return this;
    }

    /**
     * Warning categories to enable
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions xLint(Collection<XLintKey> keys) {
        if (isNotEmpty(keys)) {
            return addXLintOption(keys, "");
        }
        return this;
    }

    /**
     * Warning categories to disable
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions xLintDisable(XLintKey... keys) {
        if (isNotEmpty(keys)) {
            xLintDisable(Arrays.asList(keys));
        }
        return this;
    }

    /**
     * Warning categories to disable
     *
     * @return this list of options
     * @since 2.3.1
     */
    public JavacOptions xLintDisable(Collection<XLintKey> keys) {
        if (isNotEmpty(keys)) {
            return addXLintOption(keys, "-");
        }
        return this;
    }

    private JavacOptions addXLintOption(Collection<XLintKey> keys, String prefix) {
        if (isNotEmpty(keys)) {
            var keyString = keys.stream()
                    .map(key -> key.name().replace('_', '-').toLowerCase())
                    .collect(Collectors.joining("," + prefix, prefix, ""));

            add("-Xlint:" + keyString);
        }
        return this;
    }
}