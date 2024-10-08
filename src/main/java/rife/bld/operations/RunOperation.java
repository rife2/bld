/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.tools.FileUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs a Java application.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class RunOperation extends AbstractProcessOperation<RunOperation> {
    public static final String ARGS_OPTION = "--args=";
    protected final List<String> runOptions_ = new ArrayList<>();

    /**
     * Part of the {@link #execute} operation, constructs the command list
     * to use for building the process.
     *
     * @since 1.5
     */
    protected List<String> executeConstructProcessCommandList() {
        var args = new ArrayList<String>();
        args.add(javaTool());
        args.addAll(javaOptions());

        if (!classpath().isEmpty()) {
            args.add("-cp");
            args.add(FileUtils.joinPaths(classpath()));
        }

        if (!modulePath().isEmpty()) {
            args.add("-p");
            args.add(FileUtils.joinPaths(modulePath()));
        }

        if (module() != null && !module().isEmpty()) {
            args.add("-m");
            args.add(module());
        }
        else if (mainClass() != null && !mainClass().isEmpty()){
            args.add(mainClass());
        }

        args.addAll(runOptions());

        return args;
    }

    /**
     * Configures a run operation from a {@link BaseProject}.
     *
     * @param project the project to configure the run operation from
     * @since 1.5
     */
    public RunOperation fromProject(BaseProject project) {
        var operation = workDirectory(project.workDirectory())
                .javaTool(project.javaTool())
                .classpath(project.runClasspath())
                .modulePath(project.runModulePath())
                .mainClass(project.mainClass())
                .module(project.module());
        if (project.usesRife2Agent()) {
            operation.javaOptions().javaAgent(project.getRife2AgentFile());
        }

        // parse the run arguments if any
        var args = project.arguments();
        if (!args.isEmpty()) {
            var arg = args.get(0);
            if (arg.startsWith(ARGS_OPTION)) {
                args.remove(0);
                var runArgs = arg.substring(ARGS_OPTION.length());
                if (!runArgs.isBlank()) {
                    runOptions_.addAll(0, Arrays.asList(runArgs.split(" ")));
                }
            }
        }

        return operation;
    }

    /**
     * Provides options for the run operation
     *
     * @param options run options
     * @return this operation instance
     * @since 1.5.18
     */
    public RunOperation runOptions(String... options) {
        runOptions_.addAll(List.of(options));
        return this;
    }

    /**
     * Provides options for the run operation
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param options run options
     * @return this operation instance
     * @since 1.5.18
     */
    public RunOperation runOptions(List<String> options) {
        runOptions_.addAll(options);
        return this;
    }

    /**
     * Retrieves the run options
     *
     * @return the run options
     * @since 1.5.18
     */
    public List<String> runOptions() {
        return runOptions_;
    }
}
