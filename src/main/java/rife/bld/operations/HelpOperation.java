/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BldVersion;
import rife.bld.BuildExecutor;
import rife.template.TemplateFactory;
import rife.tools.ExceptionUtils;

import java.util.List;

import static java.util.Comparator.comparingInt;

/**
 * Provides help about the build system commands.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class HelpOperation {
    private static final String JSON_ARGUMENT = "--json";

    private final BuildExecutor executor_;
    private final List<String> arguments_;
    private boolean outputJson_ = false;

    /**
     * Creates a new help operation.
     *
     * @param executor the build executor that commands are running into
     * @param arguments the arguments that were provided to the build executor
     * @since 1.5
     */
    public HelpOperation(BuildExecutor executor, List<String> arguments) {
        executor_ = executor;
        arguments_ = arguments;
    }

    /**
     * Performs the help operation.
     *
     * @since 1.5
     */
    public void execute() {
        var topic = "";
        if (!arguments_.isEmpty()) {
            if (arguments_.get(0).equals(JSON_ARGUMENT)) {
                arguments_.remove(0);
                outputJson_ = true;
            }
            else {
                topic = arguments_.remove(0);
            }
        }
        if (!arguments_.isEmpty() && arguments_.get(0).equals(JSON_ARGUMENT)) {
            arguments_.remove(0);
            outputJson_ = true;
        }

        if (!outputJson_) {
            executePrintWelcome();
        }

        var print_full_help = true;
        Exception exception = null;
        try {
            var commands = executor_.buildCommands();
            if (commands.containsKey(topic)) {
                var command = commands.get(topic);
                var help = command.getHelp().getDescription(topic);
                if (!help.isEmpty()) {
                    if (outputJson_) {
                        var t = TemplateFactory.JSON.get("bld.help_description");
                        t.setValueEncoded("command", topic);
                        t.setValueEncoded("description", help);
                        System.out.print(t.getContent());
                    }
                    else {
                        System.err.println(help);
                    }
                    print_full_help = false;
                }
            }
        } catch (Exception e) {
            exception = e;
        }

        if (print_full_help) {
            executePrintOverviewHelp(exception);
        }
    }

    private void executePrintOverviewHelp(Exception exception) {
        if (outputJson_) {
            var t = TemplateFactory.JSON.get("bld.help_commands");

            if (exception != null) {
                t.setValueEncoded("error-message", ExceptionUtils.getExceptionStackTrace(exception));
            }

            var commands = executor_.buildCommands();
            for (var command : commands.entrySet()) {
                if (t.isValueSet("commands")) {
                    t.setValue("separator", ", ");
                }
                else {
                    t.blankValue("separator");
                }
                t.setValueEncoded("command", command.getKey());
                var build_help = command.getValue().getHelp();
                t.setValueEncoded("summary", build_help.getSummary());
                t.appendBlock("commands", "command");
            }
            System.out.print(t.getContent());

        }
        else {
            if (exception != null) {
                exception.printStackTrace();
            }

            System.err.println("""
                The bld CLI provides its features through a series of commands that
                perform specific tasks. The help command provides more information about
                the other commands.
                
                Usage: help [command] [""" + JSON_ARGUMENT + "]");

            executePrintCommands();
            executePrintHelpArguments();
            executePrintBldArguments();
        }
    }

    /**
     * Part of the {@link #execute} operation, prints the welcome message.
     *
     * @since 2.0
     */
    public void executePrintWelcome() {
        System.err.println("Welcome to bld " + BldVersion.getVersion() + ".");
        System.err.println();
    }

    /**
     * Part of the {@link #execute} operation, prints the summaries of all supported build commands.
     *
     * @since 2.0
     */
    public void executePrintCommands() {
        System.err.println("""

                The following commands are supported:
                """);

        var commands = executor_.buildCommands();
        var command_length = commands.keySet().stream().max(comparingInt(String::length)).get().length() + 2;
        for (var command : commands.entrySet()) {
            System.err.print("  ");
            System.err.printf("%-" + command_length + "s", command.getKey());
            var build_help = command.getValue().getHelp();
            System.err.print(build_help.getSummary());
            System.err.println();
        }
    }

    /**
     * Part of the {@link #execute} operation, prints the supported help arguments.
     *
     * @since 2.0
     */
    public void executePrintHelpArguments() {
        System.err.println("""
                
                The following help arguments are supported:
                
                  --json            Output help in JSON format""");
    }

    /**
     * Part of the {@link #execute} operation, prints the supported bld arguments.
     *
     * @since 2.0
     */
    public void executePrintBldArguments() {
        System.err.println("""
                
                The following bld arguments are supported:
                
                  --offline         Work without internet (only as first argument)
                  -?, -h, --help    Shows the help
                  -D<name>=<value>  Set a JVM system property
                  -s, --stacktrace  Print out the stacktrace for exceptions
                """);
    }
}