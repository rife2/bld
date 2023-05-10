/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.Project;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compiles main and test sources in the relevant build directories.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class CompileOperation extends AbstractOperation<CompileOperation> {
    private File buildMainDirectory_;
    private File buildTestDirectory_;
    private final List<String> compileMainClasspath_ = new ArrayList<>();
    private final List<String> compileTestClasspath_ = new ArrayList<>();
    private final List<File> mainSourceFiles_ = new ArrayList<>();
    private final List<File> testSourceFiles_ = new ArrayList<>();
    private final List<File> mainSourceDirectories_ = new ArrayList<>();
    private final List<File> testSourceDirectories_ = new ArrayList<>();
    private final JavacOptions compileOptions_ = new JavacOptions();
    private final List<Diagnostic<? extends JavaFileObject>> diagnostics_ = new ArrayList<>();

    /**
     * Performs the compile operation.
     *
     * @since 1.5
     */
    public void execute()
    throws IOException, ExitStatusException {
        executeCreateBuildDirectories();
        executeBuildMainSources();
        executeBuildTestSources();
        if (!diagnostics().isEmpty()) {
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }
        if (!silent()) {
            System.out.println("Compilation finished successfully.");
        }
    }

    /**
     * Part of the {@link #execute} operation, creates the build directories.
     *
     * @since 1.5
     */
    protected void executeCreateBuildDirectories() {
        if (buildMainDirectory() != null) {
            buildMainDirectory().mkdirs();
        }
        if (buildTestDirectory() != null) {
            buildTestDirectory().mkdirs();
        }
    }

    /**
     * Part of the {@link #execute} operation, builds the main sources.
     *
     * @since 1.5
     */
    protected void executeBuildMainSources()
    throws IOException {
        var sources = new ArrayList<>(mainSourceFiles());
        for (var directory : mainSourceDirectories()) {
            sources.addAll(FileUtils.getJavaFileList(directory));
        }
        executeBuildSources(
            compileMainClasspath(),
            sources,
            buildMainDirectory());
    }

    /**
     * Part of the {@link #execute} operation, builds the test sources.
     *
     * @since 1.5
     */
    protected void executeBuildTestSources()
    throws IOException {
        var sources = new ArrayList<>(testSourceFiles());
        for (var directory : testSourceDirectories()) {
            sources.addAll(FileUtils.getJavaFileList(directory));
        }
        executeBuildSources(
            compileTestClasspath(),
            sources,
            buildTestDirectory());
    }

    /**
     * Part of the {@link #execute} operation, build sources to a destination.
     *
     * @param classpath   the classpath list used for the compilation
     * @param sources     the source files to compile
     * @param destination the destination directory
     * @since 1.5
     */
    protected void executeBuildSources(List<String> classpath, List<File> sources, File destination)
    throws IOException {
        if (sources.isEmpty() || destination == null) {
            return;
        }

        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var file_manager = compiler.getStandardFileManager(null, null, null)) {
            var compilation_units = file_manager.getJavaFileObjectsFromFiles(sources);
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            var options = new ArrayList<>(List.of("-d", destination.getAbsolutePath(), "-cp", FileUtils.joinPaths(classpath)));
            options.addAll(compileOptions());
            var compilation_task = compiler.getTask(null, file_manager, diagnostics, options, null, compilation_units);
            if (!compilation_task.call()) {
                diagnostics_.addAll(diagnostics.getDiagnostics());
                executeProcessDiagnostics(diagnostics);
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, processes the compilation diagnostics.
     *
     * @param diagnostics the diagnostics to process
     * @since 1.5
     */
    protected void executeProcessDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        for (var diagnostic : diagnostics.getDiagnostics()) {
            System.err.print(executeFormatDiagnostic(diagnostic));
        }
    }

    /**
     * Part of the {@link #execute} operation, format a single diagnostic.
     *
     * @param diagnostic the diagnostic to format
     * @return a string representation of the diagnostic
     * @since 1.5
     */
    protected String executeFormatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        return diagnostic.toString() + System.lineSeparator();
    }

    /**
     * Configures a compile operation from a {@link BaseProject}.
     *
     * @param project the project to configure the compile operation from
     * @since 1.5
     */
    public CompileOperation fromProject(BaseProject project) {
        var operation = buildMainDirectory(project.buildMainDirectory())
            .buildTestDirectory(project.buildTestDirectory())
            .compileMainClasspath(project.compileMainClasspath())
            .compileTestClasspath(project.compileTestClasspath())
            .mainSourceFiles(project.mainSourceFiles())
            .testSourceFiles(project.testSourceFiles());
        if (project.javaRelease() != null && !compileOptions().containsRelease()) {
            compileOptions().release(project.javaRelease());
        }
        return operation;
    }

    /**
     * Provides the main build destination directory.
     *
     * @param directory the directory to use for the main build destination
     * @return this operation instance
     * @since 1.5
     */
    public CompileOperation buildMainDirectory(File directory) {
        buildMainDirectory_ = directory;
        return this;
    }

    /**
     * Provides the test build destination directory.
     *
     * @param directory the directory to use for the test build destination
     * @return this operation instance
     * @since 1.5
     */
    public CompileOperation buildTestDirectory(File directory) {
        buildTestDirectory_ = directory;
        return this;
    }

    /**
     * Provides entries for the main compilation classpath.
     *
     * @param classpath classpath entries
     * @return this operation instance
     * @since 1.5.18
     */
    public CompileOperation compileMainClasspath(String... classpath) {
        compileMainClasspath_.addAll(Arrays.asList(classpath));
        return this;
    }

    /**
     * Provides a list of entries for the main compilation classpath.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param classpath a list of classpath entries
     * @return this operation instance
     * @since 1.5
     */
    public CompileOperation compileMainClasspath(List<String> classpath) {
        compileMainClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Provides entries for the test compilation classpath.
     *
     * @param classpath classpath entries
     * @return this operation instance
     * @since 1.5.18
     */
    public CompileOperation compileTestClasspath(String... classpath) {
        compileTestClasspath_.addAll(Arrays.asList(classpath));
        return this;
    }

    /**
     * Provides a list of entries for the test compilation classpath.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param classpath a list of classpath entries
     * @return this operation instance
     * @since 1.5
     */
    public CompileOperation compileTestClasspath(List<String> classpath) {
        compileTestClasspath_.addAll(classpath);
        return this;
    }

    /**
     * Provides main files that should be compiled.
     *
     * @param files main files
     * @return this operation instance
     * @since 1.5.18
     */
    public CompileOperation mainSourceFiles(File... files) {
        mainSourceFiles_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Provides a list of main files that should be compiled.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param files a list of main files
     * @return this operation instance
     * @since 1.5
     */
    public CompileOperation mainSourceFiles(List<File> files) {
        mainSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides test files that should be compiled.
     *
     * @param files test files
     * @return this operation instance
     * @since 1.5.18
     */
    public CompileOperation testSourceFiles(File... files) {
        testSourceFiles_.addAll(Arrays.asList(files));
        return this;
    }

    /**
     * Provides a list of test files that should be compiled.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param files a list of test files
     * @return this operation instance
     * @since 1.5
     */
    public CompileOperation testSourceFiles(List<File> files) {
        testSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides main source directories that should be compiled.
     *
     * @param directories main source directories
     * @return this operation instance
     * @since 1.5.10
     */
    public CompileOperation mainSourceDirectories(File... directories) {
        mainSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of main source directories that should be compiled.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of main source directories
     * @return this operation instance
     * @since 1.5.10
     */
    public CompileOperation mainSourceDirectories(List<File> directories) {
        mainSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides test source directories that should be compiled.
     *
     * @param directories test source directories
     * @return this operation instance
     * @since 1.5.10
     */
    public CompileOperation testSourceDirectories(File... directories) {
        testSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of test source directories that should be compiled.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of test source directories
     * @return this operation instance
     * @since 1.5.10
     */
    public CompileOperation testSourceDirectories(List<File> directories) {
        testSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides a list of compilation options to provide to the compiler.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param options the list of compilation options
     * @return this operation instance
     * @since 1.5
     */
    public CompileOperation compileOptions(List<String> options) {
        compileOptions_.addAll(options);
        return this;
    }

    /**
     * Retrieves the main build destination directory.
     *
     * @return the main build destination
     * @since 1.5
     */
    public File buildMainDirectory() {
        return buildMainDirectory_;
    }

    /**
     * Retrieves the test build destination directory.
     *
     * @return the test build destination
     * @since 1.5
     */
    public File buildTestDirectory() {
        return buildTestDirectory_;
    }

    /**
     * Retrieves the list of entries for the main compilation classpath.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the main compilation classpath list
     * @since 1.5
     */
    public List<String> compileMainClasspath() {
        return compileMainClasspath_;
    }

    /**
     * Retrieves the list of entries for the test compilation classpath.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the test compilation classpath list
     * @since 1.5
     */
    public List<String> compileTestClasspath() {
        return compileTestClasspath_;
    }

    /**
     * Retrieves the list of main files that should be compiled.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of main files to compile
     * @since 1.5
     */
    public List<File> mainSourceFiles() {
        return mainSourceFiles_;
    }

    /**
     * Retrieves the list of test files that should be compiled.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of test files to compile
     * @since 1.5
     */
    public List<File> testSourceFiles() {
        return testSourceFiles_;
    }

    /**
     * Retrieves the list of main source directories that should be compiled.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of main source directories to compile
     * @since 1.5.10
     */
    public List<File> mainSourceDirectories() {
        return mainSourceDirectories_;
    }

    /**
     * Retrieves the list of test source directories that should be compiled.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of test source directories to compile
     * @since 1.5.10
     */
    public List<File> testSourceDirectories() {
        return testSourceDirectories_;
    }

    /**
     * Retrieves the list of compilation options for the compiler.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of compiler options
     * @since 1.5
     */
    public JavacOptions compileOptions() {
        return compileOptions_;
    }

    /**
     * Retrieves the list of diagnostics resulting from the compilation.
     *
     * @return the list of compilation diagnostics
     * @since 1.5
     */
    public List<Diagnostic<? extends JavaFileObject>> diagnostics() {
        return diagnostics_;
    }
}
