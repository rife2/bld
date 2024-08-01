/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.operations.exceptions.ExitStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final Map<String, String> toolArgs_ = new HashMap<>();
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
     * Converts arguments to a list.
     *
     * @param args the argument-value pairs
     * @return the arguments list
     */
    static List<String> argsToList(Map<String, String> args) {
        var list = new ArrayList<String>();
        for (String arg : args.keySet()) {
            var value = args.get(arg);
            list.add(arg);
            if (value != null && !value.isEmpty()) {
                list.add(value);
            }
        }
        return list;
    }

    /**
     * Adds tool command line argument.
     *
     * @param arg the argument to add
     * @return this operation
     */
    public T toolArg(String arg) {
        toolArgs_.put(arg, null);
        return (T) this;
    }

    /**
     * Add tool command line argument.
     *
     * @param arg   the argument
     * @param value the value
     * @return this operation
     */
    public T toolArg(String arg, String value) {
        toolArgs_.put(arg, value);
        return (T) this;
    }

    /**
     * Adds tool command line arguments.
     *
     * @param args the argument-value pairs to add
     * @return this operation
     */
    protected T toolArgs(Map<String, String> args) {
        toolArgs_.putAll(args);
        return (T) this;
    }

    /**
     * Clears the tool command line arguments.
     *
     * @return this operation
     */
    protected T clearToolArguments() {
        toolArgs_.clear();
        return (T) this;
    }

    @Override
    public void execute() throws Exception {
        if (toolArgs_.isEmpty()) {
            System.err.println("No " + toolName_ + " arguments specified.");
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }
        var tool = ToolProvider.findFirst(toolName_).orElseThrow(() ->
                new IllegalStateException("No " + toolName_ + " tool found."));

        var argsList = argsToList(toolArgs_);

        var stderr = new StringWriter();
        var stdout = new StringWriter();
        try (var err = new PrintWriter(stderr); var out = new PrintWriter(stdout)) {
            var status = tool.run(out, err, argsList.toArray(new String[0]));
            out.flush();
            err.flush();

            if (status != 0) {
                System.out.println(tool.name() + " " + String.join(" ", argsList));
            }

            var output = stdout.toString();
            if (!output.isBlank()) {
                System.out.println(output);
            }
            var error = stderr.toString();
            if (!error.isBlank()) {
                System.err.println(error);
            }

            ExitStatusException.throwOnFailure(status);
        }
    }

    /**
     * Returns the tool command line arguments.
     *
     * @return the arguments
     */
    public Map<String, String> toolArgs() {
        return toolArgs_;
    }
}
