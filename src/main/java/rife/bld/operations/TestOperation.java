/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.Project;
import rife.tools.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests a Java application.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class TestOperation<T extends TestOperation<T, O>, O extends List<String>> extends AbstractProcessOperation<T> {
    protected final O testToolOptions_;

    /**
     * Instantiates a new test operation.
     * @since 1.5.20
     */
    public TestOperation() {
        testToolOptions_ = createTestToolOptions();
    }

    /**
     * Creates a new collection of test tool options.
     *
     * @return the test tool options to use
     * @since 1.5.20
     */
    protected O createTestToolOptions() {
        return (O)new ArrayList<String>();
    }

    /**
     * Part of the {@link #execute} operation, constructs the command list
     * to use for building the process.
     *
     * @since 1.5
     */
    protected List<String> executeConstructProcessCommandList() {
        if (mainClass() == null) {
            throw new IllegalArgumentException("ERROR: Missing main class for test execution.");
        }

        var args = new ArrayList<String>();
        args.add(javaTool());
        args.addAll(javaOptions());
        args.add("-cp");
        args.add(FileUtils.joinPaths(classpath()));
        args.add(mainClass());
        args.addAll(testToolOptions());

        return args;
    }

    /**
     * Configures a test operation from a {@link BaseProject}.
     *
     * @param project the project to configure the test operation from
     * @since 1.5
     */
    public T fromProject(BaseProject project) {
        var operation = workDirectory(project.workDirectory())
            .javaTool(project.javaTool())
            .classpath(project.testClasspath());
        if (project.usesRife2Agent()) {
            operation.javaOptions().javaAgent(project.getRife2AgentFile());
        }
        return operation;
    }

    /**
     * Provides options to provide to the test tool.
     *
     * @param options test tool options
     * @return this operation instance
     * @since 1.5.18
     */
    public T testToolOptions(String... options) {
        testToolOptions_.addAll(List.of(options));
        return (T)this;
    }

    /**
     * Provides options to provide to the test tool.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param options test tool options
     * @return this operation instance
     * @since 1.5
     */
    public T testToolOptions(List<String> options) {
        testToolOptions_.addAll(options);
        return (T)this;
    }

    /**
     * Retrieves the options for the test tool.
     *
     * @return the test tool's options
     * @since 1.5
     */
    public O testToolOptions() {
        return (O)testToolOptions_;
    }
}
