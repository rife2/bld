/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.bld.dependencies.DependencyResolver;
import rife.bld.dependencies.Repository;
import rife.bld.help.HelpHelp;
import rife.bld.operations.HelpOperation;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.ioc.HierarchicalProperties;
import rife.tools.ExceptionUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Base class that executes build commands from a list of arguments.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @see BuildCommand
 * @see CommandDefinition
 * @since 1.5
 */
public class BuildExecutor {
    public static final File RIFE2_USER_DIR = new File(System.getProperty("user.home"), ".rife2");
    public static final String BLD_PROPERTIES = "bld.properties";
    public static final String LOCAL_PROPERTIES = "local.properties";

    private static final String ARG_HELP1 = "--help";
    private static final String ARG_HELP2 = "-h";
    private static final String ARG_HELP3 = "-?";
    private static final String ARG_STACKTRACE1 = "--stacktrace";
    private static final String ARG_STACKTRACE2 = "-s";

    private final HierarchicalProperties properties_;
    private List<String> arguments_ = Collections.emptyList();
    private Map<String, CommandDefinition> buildCommands_ = null;
    private final AtomicReference<String> currentCommandName_ = new AtomicReference<>();
    private final AtomicReference<CommandDefinition> currentCommandDefinition_ = new AtomicReference<>();
    private int exitStatus_ = 0;

    /**
     * Show the full Java stacktrace when exceptions occur, as opposed
     * to the chain of messages.
     * <p>
     * Defaults to {@code false}, can be set to {@code true} by setting
     * through code or by adding {@code --stacktrace} as a CLI argument.
     *
     * @since 1.5.19
     */
    protected boolean showStacktrace = false;

    /**
     * Creates a new build executor instance.
     *
     * @since 1.5
     */
    public BuildExecutor() {
        properties_ = setupProperties(workDirectory());
        Repository.resolveMavenLocal(properties());
    }

    /**
     * Creates a properties hierarchy for bld execution.
     *
     * @param workDirectory the directory where the project build files are location
     * @return the properties hierarchy
     * @since 1.5.12
     */
    public static HierarchicalProperties setupProperties(File workDirectory) {
        var system_properties = HierarchicalProperties.createSystemInstance();

        var java_properties = system_properties;
        system_properties = java_properties.getParent();

        HierarchicalProperties bld_properties = null;
        HierarchicalProperties local_properties = null;

        var bld_properties_file = new File(RIFE2_USER_DIR, BLD_PROPERTIES);
        if (bld_properties_file.exists() && bld_properties_file.isFile() && bld_properties_file.canRead()) {
            try {
                var bld = new Properties();
                bld.load(new FileReader(bld_properties_file));
                bld_properties = new HierarchicalProperties();
                bld_properties.putAll(bld);

                bld_properties.parent(system_properties);
            } catch (IOException e) {
                Logger.getLogger("rife.bld").warning("Unable to parse " + bld_properties_file + " as a properties file:\n" + e.getMessage());
            }
        }

        var local_properties_file = new File(workDirectory, LOCAL_PROPERTIES);
        if (local_properties_file.exists() && local_properties_file.isFile() && local_properties_file.canRead()) {
            try {
                var local = new Properties();
                local.load(new FileReader(local_properties_file));
                local_properties = new HierarchicalProperties();
                local_properties.putAll(local);

                local_properties.parent(Objects.requireNonNullElse(bld_properties, system_properties));
            } catch (IOException e) {
                Logger.getLogger("rife.bld").warning("Unable to parse " + local_properties_file + " as a properties file:\n" + e.getMessage());
            }
        }

        java_properties.parent(
            Objects.requireNonNullElse(local_properties,
                Objects.requireNonNullElse(bld_properties, system_properties)));

        final HierarchicalProperties properties = new HierarchicalProperties();
        properties.parent(java_properties);
        return properties;
    }

    /**
     * Returns the properties uses by this conversation.
     *
     * @return the instance of {@code HierarchicalProperties} that is used
     * by this build executor
     * @since 1.5
     */
    public HierarchicalProperties properties() {
        return properties_;
    }

    /**
     * Retrieve a property from the {@link #properties()}.
     *
     * @param name the name of the property
     * @return the requested property; or {@code null} if it doesn't exist
     * @since 1.5.15
     */
    public String property(String name) {
        return properties().getValueString(name);
    }

    /**
     * Retrieve a property from the {@link #properties()} with a default value.
     *
     * @param name         the name of the property
     * @param defaultValue the value that should be used as a fallback
     * @return the requested property; or the default value if it doesn't exist
     * @since 1.5.15
     */
    public String property(String name, String defaultValue) {
        return properties().getValueString(name, defaultValue);
    }

    /**
     * Checks for the existence of a property in {@link #properties()}.
     *
     * @param name the name of the property
     * @return {@code true} if the property exists; or {@code false} otherwise
     * @since 1.5.15
     */
    public boolean hasProperty(String name) {
        return properties().contains(name);
    }

    /**
     * Returns the work directory of the project.
     * Defaults to this process's user working directory, which when running
     * through the bld wrapper corresponds to the top-level project directory.
     *
     * @since 1.5.12
     */
    public File workDirectory() {
        return new File(System.getProperty("user.dir"));
    }

    /**
     * Set the exist status to use at the end of the execution.
     *
     * @param status sets the exit status
     * @since 1.5.1
     */
    public void exitStatus(int status) {
        exitStatus_ = status;
    }

    /**
     * Retrieves the exit status.
     *
     * @return the exit status
     * @since 1.5.1
     */
    public int exitStatus() {
        return exitStatus_;
    }

    /**
     * Execute the build commands from the provided arguments.
     * <p>
     * While the build is executing, the arguments can be retrieved
     * using {@link #arguments()}.
     *
     * @param arguments the arguments to execute the build with
     * @return the exist status
     * @since 1.5.1
     */
    public int execute(String[] arguments) {
        arguments_ = new ArrayList<>(Arrays.asList(arguments));

        var show_help = false;
        show_help |= arguments_.removeAll(List.of(ARG_HELP1, ARG_HELP2, ARG_HELP3));
        showStacktrace |= arguments_.removeAll(List.of(ARG_STACKTRACE1, ARG_STACKTRACE2));
        show_help |= arguments_.isEmpty();

        if (show_help) {
            new HelpOperation(this, Collections.emptyList()).execute();
            return exitStatus_;
        }

        while (!arguments_.isEmpty()) {
            var command = arguments_.remove(0);

            try {
                if (!executeCommand(command)) {
                    break;
                }
            } catch (Throwable e) {
                exitStatus(1);

                System.err.println();

                if (showStacktrace) {
                    System.err.println(ExceptionUtils.getExceptionStackTrace(e));
                } else {
                    boolean first = true;
                    var e2 = e;
                    while (e2 != null) {
                        if (e2.getMessage() != null) {
                            if (!first) {
                                System.err.print("> ");
                            }
                            System.err.println(e2.getMessage());
                            first = false;
                        }
                        e2 = e2.getCause();
                    }

                    if (first) {
                        System.err.println(ExceptionUtils.getExceptionStackTrace(e));
                    }
                }
            }
        }


        return exitStatus_;
    }

    /**
     * Starts the execution of the build. This method will call
     * System.exit() when done with the appropriate exit status.
     *
     * @param arguments the arguments to execute the build with
     * @see #execute
     * @since 1.5.1
     */
    public void start(String[] arguments) {
        System.exit(execute(arguments));
    }

    /**
     * Retrieves the list of arguments that are being processed.
     *
     * @return the list of arguments
     * @since 1.5
     */
    public List<String> arguments() {
        return arguments_;
    }

    /**
     * Retrieves the commands that can be executed by this {@code BuildExecutor}.
     *
     * @return a map containing the name of the build command and the method that
     * corresponds to execution
     * @see BuildCommand
     * @since 1.5
     */
    public Map<String, CommandDefinition> buildCommands() {
        if (buildCommands_ == null) {
            var build_commands = new TreeMap<String, CommandDefinition>();

            Class<?> klass = getClass();

            try {
                while (klass != null) {
                    for (var method : klass.getDeclaredMethods()) {
                        if (method.getParameters().length == 0 && method.isAnnotationPresent(BuildCommand.class)) {
                            method.setAccessible(true);

                            var name = method.getName();
                            var annotation = method.getAnnotation(BuildCommand.class);

                            var annotation_name = annotation.value();
                            if (annotation_name != null && !annotation_name.isEmpty()) {
                                name = annotation_name;
                            }

                            if (!build_commands.containsKey(name)) {
                                var build_help = annotation.help();
                                CommandHelp command_help = null;
                                if (build_help != null && build_help != CommandHelp.class) {
                                    command_help = build_help.getDeclaredConstructor().newInstance();
                                }

                                var summary = annotation.summary();
                                var description = annotation.description();
                                if ((summary != null && !summary.isBlank()) ||
                                    (description != null && !description.isBlank())) {
                                    if (summary == null) summary = "";
                                    if (description == null) description = "";
                                    if (command_help != null) {
                                        if (summary.isBlank()) summary = command_help.getSummary();
                                        if (description.isBlank()) description = command_help.getDescription(name);
                                    }
                                    command_help = new AnnotatedCommandHelp(summary, description);
                                }

                                build_commands.put(name, new CommandAnnotated(this, method, command_help));
                            }
                        }
                    }

                    klass = klass.getSuperclass();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            buildCommands_ = build_commands;
        }

        return buildCommands_;
    }

    private static class AnnotatedCommandHelp implements CommandHelp {
        private final String summary_;
        private final String description_;

        AnnotatedCommandHelp(String summary, String description) {
            summary_ = summary;
            description_ = description;
        }

        @Override
        public String getSummary() {
            return summary_;
        }

        @Override
        public String getDescription(String topic) {
            return description_;
        }
    }

    /**
     * Performs the execution of a single command.
     *
     * @param command the name of the command to execute
     * @return {@code true} when the command was found and executed; or
     * {@code false} if the command couldn't be found
     * @throws Throwable when an exception occurred during the command execution
     * @see BuildCommand
     * @since 1.5
     */
    public boolean executeCommand(String command)
    throws Throwable {
        var matched_command = command;
        var definition = buildCommands().get(command);

        // try to find a match for the provided command amongst
        // the ones that are known
        if (definition == null) {
            // try to find starting matching options
            var matches = new ArrayList<>(buildCommands().keySet().stream()
                .filter(c -> c.toLowerCase().startsWith(command.toLowerCase()))
                .toList());

            if (matches.isEmpty()) {
                // try to find fuzzy matching options
                var fuzzy_regexp = new StringBuilder("^.*");
                for (var ch : command.toCharArray()) {
                    fuzzy_regexp.append("\\Q");
                    fuzzy_regexp.append(ch);
                    fuzzy_regexp.append("\\E.*");
                }
                fuzzy_regexp.append("$");
                var fuzzy_pattern = Pattern.compile(fuzzy_regexp.toString());
                matches.addAll(buildCommands().keySet().stream()
                    .filter(c -> fuzzy_pattern.matcher(c.toLowerCase()).matches())
                    .toList());
            }

            // only proceed if exactly one match was found
            if (matches.size() == 1) {
                matched_command = matches.get(0);
                System.out.println("Executing matched command: " + matched_command);
                definition = buildCommands().get(matched_command);
            }
        }

        // execute the command if we found one
        if (definition != null) {
            try {
                currentCommandName_.set(matched_command);
                currentCommandDefinition_.set(definition);
                definition.execute();
            } catch (ExitStatusException e) {
                exitStatus(e.getExitStatus());
                return e.getExitStatus() == ExitStatusException.EXIT_SUCCESS;
            } finally {
                currentCommandDefinition_.set(null);
                currentCommandName_.set(null);
            }
        } else {
            new HelpOperation(this, arguments()).executePrintOverviewHelp();
            System.err.println();
            System.err.println("ERROR: unknown command '" + command + "'");
            return false;
        }
        return true;
    }

    /**
     * Retrieves the name of the currently executing command.
     *
     * @return the name of the current command; or
     * {@code null} if no command is currently executing
     * @since 1.5.12
     */
    public String getCurrentCommandName() {
        return currentCommandName_.get();
    }

    /**
     * Retrieves the definition of the currently executing command.
     *
     * @return the definition of the current command; or
     * {@code null} if no command is currently executing
     * @since 1.5.12
     */
    public CommandDefinition getCurrentCommandDefinition() {
        return currentCommandDefinition_.get();
    }

    /**
     * The standard {@code help} command.
     *
     * @since 1.5
     */
    @BuildCommand(help = HelpHelp.class)
    public void help() {
        new HelpOperation(this, arguments()).execute();
    }
}
