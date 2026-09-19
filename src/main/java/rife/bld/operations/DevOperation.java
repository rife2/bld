/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Runs a RIFE2 web application through its reloader and recompiles the main
 * sources whenever they change, so that the application restarts and the
 * browser reloads while developing.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 3.1.0
 */
public class DevOperation extends AbstractProcessOperation<DevOperation> {
    public static final String ARGS_OPTION = RunOperation.ARGS_OPTION;
    public static final String DEBUG_OPTION = "--debug";
    public static final String RELOADER_CLASS = "rife.engine.Reloader";
    public static final String RELOAD_DEBUG_PROPERTY = "rife.reload.debug";

    private static final String RELOADER_RESOURCE = RELOADER_CLASS.replace('.', '/') + ".class";
    private static final long POLL_INTERVAL_MS = 500;
    // longer than the reloader takes to end its application
    private static final long STOP_TIMEOUT_MS = 15000;

    record SourceStamp(long modified, long length) {
    }

    protected final List<String> runOptions_ = new ArrayList<>();
    protected final List<File> sourceDirectories_ = new ArrayList<>();
    protected Supplier<CompileOperation> compileOperation_ = null;
    protected String debugPort_ = null;

    /**
     * Part of the {@link #execute} operation, constructs the command list
     * to use for building the process.
     *
     * @since 3.1.0
     */
    protected List<String> executeConstructProcessCommandList() {
        var args = new ArrayList<String>();
        args.add(javaTool());
        args.addAll(javaOptions());

        if (debugPort_ != null) {
            args.add("-D" + RELOAD_DEBUG_PROPERTY + (debugPort_.isEmpty() ? "" : "=" + debugPort_));
        }

        if (!classpath().isEmpty()) {
            args.add("-cp");
            args.add(FileUtils.joinPaths(classpath()));
        }

        if (!modulePath().isEmpty()) {
            args.add("-p");
            args.add(FileUtils.joinPaths(modulePath()));
        }

        args.add(RELOADER_CLASS);
        args.add(mainClass());
        args.addAll(runOptions());

        return args;
    }

    /**
     * Performs the dev operation.
     *
     * @throws InterruptedException    when the operation was interrupted
     * @throws IOException             when an exception occurred during the execution of the process
     * @throws FileUtilsErrorException when an exception occurred during the retrieval of the operation output
     * @throws ExitStatusException     when the sources don't compile at the start, the reloader can't
     *                                 be found or the application exited with a failure
     * @since 3.1.0
     */
    @Override
    public void execute()
    throws IOException, FileUtilsErrorException, InterruptedException, ExitStatusException {
        if (mainClass() == null || mainClass().isEmpty()) {
            if (!silent()) {
                System.err.println("No main class specified.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }

        if (!reloaderAvailable()) {
            if (!silent()) {
                System.err.println("The dev command needs the reloader of RIFE2 1.11.0 or later, which isn't on the classpath or module path.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }

        // sources that change during the first compilation are compiled again
        var sources = snapshot();
        compile(true);

        successful_ = true;
        outputProcessorThread_ = null;
        errorProcessorThread_ = null;
        process_ = executeStartProcess();

        var process = process_;
        var hook = new Thread(() -> stop(process));
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            var changed = false;
            while (process.isAlive()) {
                Thread.sleep(POLL_INTERVAL_MS);

                var current = snapshot();
                if (!current.equals(sources)) {
                    // an editor saves several files in a row, compile once they're done
                    sources = current;
                    changed = true;
                } else if (changed) {
                    changed = false;
                    compile(false);
                }
            }

            if (outputProcessorThread_ != null) {
                outputProcessorThread_.join();
            }
            if (errorProcessorThread_ != null) {
                errorProcessorThread_.join();
            }

            ExitStatusException.throwOnFailure(successful_ ? process.exitValue() : ExitStatusException.EXIT_FAILURE);
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException ignore) {
                // the JVM is already shutting down
            }
            stop(process);
        }
    }

    // the errors of a failed compilation are already printed
    private void compile(boolean required)
    throws IOException, ExitStatusException {
        if (null == compileOperation_) {
            return;
        }

        try {
            compileOperation_.get().execute();
        } catch (ExitStatusException e) {
            if (required) {
                throw e;
            }
            if (!silent()) {
                System.err.println("The compilation failed, the application keeps running with its previous classes until the sources compile again.");
            }
        }
    }

    // the reloader ends its application when it's asked to stop, except on
    // Windows where it's killed outright, so the application is found first
    private static void stop(Process process) {
        var descendants = process.descendants().collect(Collectors.toSet());
        process.destroy();
        try {
            if (!process.waitFor(STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        descendants.forEach(ProcessHandle::destroyForcibly);
    }

    private Map<String, SourceStamp> snapshot() {
        var result = new HashMap<String, SourceStamp>();
        for (var directory : sourceDirectories_) {
            if (!directory.isDirectory()) {
                continue;
            }
            for (var file : FileUtils.getJavaFileList(directory)) {
                result.put(file.getAbsolutePath(), new SourceStamp(file.lastModified(), file.length()));
            }
        }
        return result;
    }

    boolean reloaderAvailable() {
        var paths = new ArrayList<>(classpath());
        paths.addAll(modulePath());
        return reloaderAvailable(paths);
    }

    static boolean reloaderAvailable(List<String> paths) {
        for (var entry : paths) {
            var file = new File(entry);
            if (file.isDirectory()) {
                if (new File(file, RELOADER_RESOURCE).exists()) {
                    return true;
                }
            } else if (file.isFile()) {
                try (var jar = new JarFile(file)) {
                    if (jar.getEntry(RELOADER_RESOURCE) != null) {
                        return true;
                    }
                } catch (IOException ignore) {
                    // not an archive
                }
            }
        }
        return false;
    }

    /**
     * Configures a dev operation from a {@link BaseProject}.
     *
     * @param project the project to configure the dev operation from
     * @since 3.1.0
     */
    public DevOperation fromProject(BaseProject project) {
        var operation = verbose(project.verbose())
            .workDirectory(project.workDirectory())
            .javaTool(project.javaTool())
            .classpath(project.runClasspath())
            .modulePath(project.runModulePath())
            .mainClass(project.mainClass())
            .sourceDirectories(List.of(project.srcMainJavaDirectory()))
            .compileOperation(() -> new CompileOperation() {
                // the tests don't run while developing, only the main sources matter
                protected void executeBuildTestSources() {
                }
            }.compileOptions(project.compileOperation().compileOptions()).fromProject(project));
        if (project.usesRife2Agent()) {
            operation.javaOptions().javaAgent(project.getRife2AgentFile());
        }

        // parse the leading dev arguments if any
        var args = project.arguments();
        while (!args.isEmpty()) {
            var arg = args.get(0);
            if (arg.equals(DEBUG_OPTION)) {
                args.remove(0);
                debugPort("");
            } else if (arg.startsWith(DEBUG_OPTION + "=")) {
                args.remove(0);
                debugPort(arg.substring(DEBUG_OPTION.length() + 1));
            } else if (arg.startsWith(ARGS_OPTION)) {
                args.remove(0);
                var runArgs = arg.substring(ARGS_OPTION.length());
                if (!runArgs.isBlank()) {
                    runOptions_.addAll(0, Arrays.asList(runArgs.split(" ")));
                }
            } else {
                break;
            }
        }

        return operation;
    }

    /**
     * Provides options for the application that's being run.
     *
     * @param options run options
     * @return this operation instance
     * @since 3.1.0
     */
    public DevOperation runOptions(String... options) {
        runOptions_.addAll(List.of(options));
        return this;
    }

    /**
     * Provides options for the application that's being run.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param options run options
     * @return this operation instance
     * @since 3.1.0
     */
    public DevOperation runOptions(List<String> options) {
        runOptions_.addAll(options);
        return this;
    }

    /**
     * Retrieves the options for the application that's being run.
     *
     * @return the run options
     * @since 3.1.0
     */
    public List<String> runOptions() {
        return runOptions_;
    }

    /**
     * Provides the source directories that are watched for changes.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories the source directories to watch
     * @return this operation instance
     * @since 3.1.0
     */
    public DevOperation sourceDirectories(List<File> directories) {
        sourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Retrieves the source directories that are watched for changes.
     *
     * @return the watched source directories
     * @since 3.1.0
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Provides the compile operation that's created for each compilation of
     * the changed sources.
     *
     * @param operation the supplier of compile operations
     * @return this operation instance
     * @since 3.1.0
     */
    public DevOperation compileOperation(Supplier<CompileOperation> operation) {
        compileOperation_ = operation;
        return this;
    }

    /**
     * Retrieves the supplier of the compile operations.
     *
     * @return the supplier of compile operations; or
     * {@code null} if the sources aren't compiled
     * @since 3.1.0
     */
    public Supplier<CompileOperation> compileOperation() {
        return compileOperation_;
    }

    /**
     * Makes the application debuggable on a port, an empty string stands for
     * the default port of the reloader.
     *
     * @param port the debug port; or {@code null} to not debug
     * @return this operation instance
     * @since 3.1.0
     */
    public DevOperation debugPort(String port) {
        debugPort_ = port;
        return this;
    }

    /**
     * Retrieves the debug port.
     *
     * @return the debug port; an empty string for the default port; or
     * {@code null} when the application isn't debuggable
     * @since 3.1.0
     */
    public String debugPort() {
        return debugPort_;
    }
}
