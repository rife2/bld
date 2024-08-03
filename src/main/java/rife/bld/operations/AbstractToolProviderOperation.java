/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
     * Adds arguments to pass to the tool.
     *
     * @param arg one or more argument
     * @return this operation
     */
    @SuppressWarnings("unchecked")
    public T toolArgs(String... arg) {
        toolArgs(List.of(arg));
        return (T) this;
    }

    /**
     * Adds arguments to pass to the tool.
     *
     * @param args the argument-value pairs to add
     * @return this operation
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    protected T toolArgs(Map<String, String> args) {
        args.forEach((k, v) -> {
            toolArgs_.add(k);
            if (v != null && !v.isEmpty()) {
                toolArgs_.add(v);
            }
        });
        return (T) this;
    }

    /**
     * Adds arguments to pass to the tool.
     *
     * @param args the argument to add
     * @return this operation
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public T toolArgs(List<String> args) {
        toolArgs_.addAll(args);
        return (T) this;
    }

    /**
     * Returns the tool's arguments.
     *
     * @return the arguments
     */
    public List<String> toolArgs() {
        return toolArgs_;
    }

    /**
     * Parses arguments to pass to the tool from the given files.
     *
     * @param files the list of files
     * @return this operation instance
     * @throws FileNotFoundException if a file cannot be found
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public T toolArgsFromFile(List<String> files) throws FileNotFoundException {
        var list = new ArrayList<String>();

        for (var option : files) {
            try (var scanner = new Scanner(new File(option))) {
                while (scanner.hasNext()) {
                    var splitLine = scanner.nextLine().split("--");
                    for (String args : splitLine) {
                        if (!args.isBlank()) {
                            var splitArgs = args.split(" ", 2);
                            list.add("--" + splitArgs[0]);
                            if (splitArgs.length > 1 && !splitArgs[1].isBlank()) {
                                list.add(splitArgs[1]);
                            }
                        }
                    }
                }
            }
        }

        toolArgs(list);

        return (T) this;
    }
}
