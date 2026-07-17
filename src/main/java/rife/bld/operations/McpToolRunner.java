/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.BuildExecutor;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The entry point that runs exactly one build command for an MCP tool
 * call, in a separate build process.
 * <p>
 * It reads the command, its arguments, the flags and the excluded commands
 * from the control file, creates the build executor, and runs the single
 * command through its public API. All of this single command handling lives
 * here in the MCP feature, the build itself stays unaware of it. The runner
 * is launched with the build executor's class name as its last argument,
 * any earlier arguments, like the wrapper's {@code --offline} option, are
 * ignored.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4.0
 */
public final class McpToolRunner {
    /**
     * The environment variable with the path of the control file that
     * requests the execution of exactly one command.
     * @since 2.4.0
     */
    public static final String ENV_CONTROL_FILE = "BLD_MCP_CONTROL_FILE";

    /**
     * The environment variable with the path of the file that the runner
     * writes its result to.
     * @since 2.4.0
     */
    public static final String ENV_STATUS_FILE = "BLD_MCP_STATUS_FILE";

    /**
     * The outcome that is written when the command couldn't be resolved.
     * @since 2.4.0
     */
    public static final String STATUS_UNKNOWN_COMMAND = "unknown-command";

    /**
     * The outcome that is written when the command is excluded.
     * @since 2.4.0
     */
    public static final String STATUS_EXCLUDED_COMMAND = "excluded-command";

    /**
     * The outcome that is written when the runner itself couldn't be set
     * up, for instance when the build executor class couldn't be
     * instantiated, this is a server failure rather than a tool failure.
     * @since 2.4.0
     */
    public static final String STATUS_RUNNER_ERROR = "runner-error";

    private McpToolRunner() {
    }

    /**
     * Runs a single build command for an MCP tool call.
     *
     * @param arguments the build executor class name as the last argument
     * @since 2.4.0
     */
    public static void main(String[] arguments) {
        System.exit(run(arguments));
    }

    private static int run(String[] arguments) {
        var control_path = System.getenv(ENV_CONTROL_FILE);
        if (control_path == null) {
            writeStatus(STATUS_RUNNER_ERROR);
            System.err.println("ERROR: no MCP control file provided");
            return ExitStatusException.EXIT_FAILURE;
        }

        var control_file = new File(control_path);
        McpControl control;
        try {
            control = McpControl.read(control_file);
        } catch (IOException e) {
            writeStatus(STATUS_RUNNER_ERROR);
            System.err.println("ERROR: the MCP control file couldn't be read");
            return ExitStatusException.EXIT_FAILURE;
        } finally {
            // the control file is consumed immediately, so that nested
            // processes that inherit the environment don't re-enter the
            // single command execution and don't overwrite its status
            control_file.delete();
        }

        if (arguments.length == 0) {
            writeStatus(STATUS_RUNNER_ERROR);
            System.err.println("ERROR: no build executor class provided");
            return ExitStatusException.EXIT_FAILURE;
        }
        // the build executor class is instantiated directly through its
        // no-arg constructor and driven through its public API, its main
        // method is not invoked, so that a tool call runs exactly the
        // requested command and nothing else
        BuildExecutor executor;
        try {
            var executor_class = Class.forName(arguments[arguments.length - 1]);
            executor = (BuildExecutor) executor_class.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            writeStatus(STATUS_RUNNER_ERROR);
            System.err.println("ERROR: the build executor couldn't be instantiated: " + e);
            return ExitStatusException.EXIT_FAILURE;
        }

        executor.offline(control.offline);
        executor.verbose(control.verbose);
        executor.showStacktrace(control.stacktrace);

        var resolved = executor.resolveCommand(control.command);
        if (resolved == null) {
            // the result goes into the status file so that it can never be
            // confused with a regular exit status of a build command
            writeStatus(STATUS_UNKNOWN_COMMAND);
            System.err.println("ERROR: Unknown command '" + control.command + "'");
            return ExitStatusException.EXIT_FAILURE;
        }
        // the exclusions are enforced here, in the freshly compiled build,
        // so that aliases or commands added to the build sources can't
        // reach an excluded command either
        if (control.exclusions.contains(control.command) || control.exclusions.contains(resolved)) {
            writeStatus(STATUS_EXCLUDED_COMMAND);
            System.err.println("ERROR: Command '" + control.command + "' is not available");
            return ExitStatusException.EXIT_FAILURE;
        }

        // an automatic dependency download and purge that the project is
        // configured for runs before the command, exactly like it would on
        // the command line
        if (executor instanceof BaseProject project) {
            project.performAutoDownloadPurgeIfEnabled();
        }

        // the command's arguments come from the control file, they are
        // never interpreted as additional commands or as global flags
        executor.arguments().clear();
        executor.arguments().addAll(control.arguments);
        try {
            executor.executeCommand(resolved);
        } catch (Throwable e) {
            executor.exitStatus(ExitStatusException.EXIT_FAILURE);
            System.err.println();
            if (control.stacktrace) {
                System.err.println(ExceptionUtils.getExceptionStackTrace(e));
            } else if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            } else {
                System.err.println(e.getClass().getName());
            }
        }
        return executor.exitStatus();
    }

    private static void writeStatus(String status) {
        var status_file = System.getenv(ENV_STATUS_FILE);
        if (status_file != null) {
            try {
                Files.writeString(Path.of(status_file), status);
            } catch (IOException e) {
                // the caller falls back to the exit status
            }
        }
    }
}
