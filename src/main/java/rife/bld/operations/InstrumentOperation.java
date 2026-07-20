/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.tools.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Instruments compiled classes ahead of time with RIFE2's bytecode
 * transformations, as an alternative to the java agent.
 * <p>The instrumentation is performed by
 * {@code rife.instrument.InstrumentationDeployer} from the RIFE2
 * dependency of the project that is being built, so the same
 * transformations are applied as the ones the agent of that RIFE2 version
 * performs at class loading time: web engine continuations, workflow
 * continuations, meta-data merging and lazy-loading. Classes that don't
 * use any of these capabilities are left untouched, and instrumenting
 * already instrumented classes makes no changes.
 * <p>Ahead-of-time instrumented classes make the agent unnecessary at run
 * time and are the way to use these capabilities inside a GraalVM native
 * image.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4
 */
public class InstrumentOperation extends AbstractProcessOperation<InstrumentOperation> {
    public static final String DEPLOYER_CLASS = "rife.instrument.InstrumentationDeployer";

    private final List<File> sourceDirectories_ = new ArrayList<>();
    private File destinationDirectory_;

    /**
     * Part of the {@link #execute} operation, constructs the command list
     * to use for building the process.
     *
     * @since 2.4
     */
    protected List<String> executeConstructProcessCommandList() {
        var args = new ArrayList<String>();
        args.add(javaTool());
        args.addAll(javaOptions());

        if (!classpath().isEmpty()) {
            args.add("-cp");
            args.add(FileUtils.joinPaths(classpath()));
        }

        args.add(DEPLOYER_CLASS);
        args.add("-verbose");
        args.add("-d");
        args.add(destinationDirectory().getAbsolutePath());
        for (var directory : sourceDirectories()) {
            args.add(directory.getAbsolutePath());
        }

        return args;
    }

    /**
     * Configures an instrument operation from a {@link BaseProject}.
     * <p>The project's main build directory is instrumented in place, with
     * the runtime classpath providing the RIFE2 dependency that performs
     * the instrumentation.
     *
     * @param project the project to configure the instrument operation from
     * @since 2.4
     */
    public InstrumentOperation fromProject(BaseProject project) {
        return classpath(project.runClasspath())
            .sourceDirectories(project.buildMainDirectory())
            .destinationDirectory(project.buildMainDirectory());
    }

    /**
     * Provides source directories that will be instrumented.
     *
     * @param directories source directories
     * @return this operation instance
     * @since 2.4
     */
    public InstrumentOperation sourceDirectories(File... directories) {
        sourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of source directories that will be instrumented.
     * <p>A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of source directories
     * @return this operation instance
     * @since 2.4
     */
    public InstrumentOperation sourceDirectories(Collection<File> directories) {
        sourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides the destination directory in which the instrumented classes
     * will be stored.
     *
     * @param directory the instrumentation destination directory
     * @return this operation instance
     * @since 2.4
     */
    public InstrumentOperation destinationDirectory(File directory) {
        destinationDirectory_ = directory;
        return this;
    }

    /**
     * Retrieves the source directories that will be instrumented.
     * <p>This is a modifiable list that can be retrieved and changed.
     *
     * @return the instrumentation source directories
     * @since 2.4
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Retrieves the destination directory in which the instrumented classes
     * will be stored.
     *
     * @return the instrumentation destination directory
     * @since 2.4
     */
    public File destinationDirectory() {
        return destinationDirectory_;
    }
}
