/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.operations.exceptions.ExitStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.spi.ToolProvider;

/**
 * Provides common features for tool providers.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.0.2
 */
public abstract class AbstractToolProviderOperation<T extends AbstractToolProviderOperation<T>>
        extends AbstractOperation<AbstractToolProviderOperation<T>> {
    private final List<String> toolArgs_ = new ArrayList<>();
    private final String toolName_;

    /**
     * Provides the name of the tool.
     *
     * @param toolName the tool name
     */
    public AbstractToolProviderOperation(String toolName) {
        toolName_ = toolName;
    }

    /**
     * Adds tool command line arguments.
     *
     * @param args the argument-value pairs to add
     * @return this operation
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    protected T addArgs(Map<String, String> args) {
        args.forEach((k, v) -> {
            toolArgs_.add(k);
            if (v != null && !v.isEmpty()) {
                toolArgs_.add(v);
            }
        });
        return (T) this;
    }

    /**
     * Adds tool command line arguments.
     *
     * @param args the argument to add
     * @return this operation
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public T addArgs(List<String> args) {
        toolArgs_.addAll(args);
        return (T) this;
    }

    /**
     * Adds tool command line arguments.
     *
     * @param arg one or more argument
     * @return this operation
     */
    @SuppressWarnings("unchecked")
    public T addArgs(String... arg) {
        addArgs(List.of(arg));
        return (T) this;
    }

    /**
     * Runs an instance of the tool.
     * <p>
     * On success, command line arguments are automatically cleared.
     *
     * @throws Exception if an error occurred
     */
    @Override
    public void execute() throws Exception {
        if (toolArgs_.isEmpty()) {
            System.err.println("No " + toolName_ + " command line arguments specified.");
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }
        var tool = ToolProvider.findFirst(toolName_).orElseThrow(() ->
                new IllegalStateException("No " + toolName_ + " tool found."));

        var status = tool.run(System.out, System.err, toolArgs_.toArray(new String[0]));
        if (status != 0) {
            System.out.println(tool.name() + ' ' + String.join(" ", toolArgs_));
        }
        ExitStatusException.throwOnFailure(status);

        toolArgs_.clear();
    }

    /**
     * Returns the tool command line arguments.
     *
     * @return the arguments
     */
    public List<String> toolArgs() {
        return toolArgs_;
    }
}
