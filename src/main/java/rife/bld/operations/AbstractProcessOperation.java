/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.bld.operations.exceptions.OperationOptionException;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Abstract operation that starts a Java application as a separate process.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.18
 */
public abstract class AbstractProcessOperation<T extends AbstractProcessOperation<T>> extends AbstractOperation<T> {
    public static final String DEFAULT_JAVA_TOOL = "java";

    protected File workDirectory_ = new File(System.getProperty("user.dir"));
    protected String javaTool_ = DEFAULT_JAVA_TOOL;
    protected final JavaOptions javaOptions_ = new JavaOptions();
    protected final List<String> classpath_ = new ArrayList<>();
    protected String mainClass_;
    protected Function<String, Boolean> outputProcessor_;
    protected Function<String, Boolean> errorProcessor_;
    protected Process process_;
    protected boolean successful_;
    protected Thread outputProcessorThread_;
    protected Thread errorProcessorThread_;

    /**
     * Performs the operation.
     *
     * @throws InterruptedException    when the operation was interrupted
     * @throws IOException             when an exception occurred during the execution of the process
     * @throws FileUtilsErrorException when an exception occurred during the retrieval of the operation output
     * @throws ExitStatusException     when the exit status was changed during the operation
     * @since 1.5
     */
    public void execute()
    throws IOException, FileUtilsErrorException, InterruptedException, ExitStatusException {
        successful_ = true;
        outputProcessorThread_ = null;
        errorProcessorThread_ = null;

        process_ = executeStartProcess();

        int status = process_.waitFor();

        if (outputProcessorThread_ != null) {
            outputProcessorThread_.join();
        }
        if (errorProcessorThread_ != null) {
            errorProcessorThread_.join();
        }
        if (!successful_) {
            status = ExitStatusException.EXIT_FAILURE;
        }

        ExitStatusException.throwOnFailure(status);
    }

    /**
     * Part of the {@link #execute} operation, constructs the command list
     * to use for building the process.
     *
     * @since 1.5
     */
    abstract protected List<String> executeConstructProcessCommandList();

    /**
     * Part of the {@link #execute} operation, starts the process.
     *
     * @since 1.5
     */
    protected Process executeStartProcess()
    throws IOException {
        var builder = new ProcessBuilder(executeConstructProcessCommandList());
        builder.directory(workDirectory());

        final var output_processor = outputProcessor();
        if (output_processor == null) {
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        } else {
            builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        }

        final var error_processor = errorProcessor();
        if (error_processor == null) {
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        } else {
            builder.redirectError(ProcessBuilder.Redirect.PIPE);
        }

        final var process = builder.start();

        if (output_processor != null) {
            outputProcessorThread_ = startProcessStreamProcessor(process.getInputStream(), output_processor);
        }
        if (error_processor != null) {
            errorProcessorThread_ = startProcessStreamProcessor(process.getErrorStream(), error_processor);
        }

        return process;
    }

    private Thread startProcessStreamProcessor(InputStream stream, Function<String, Boolean> processor) {
        var processor_thread = new Thread(() -> {
            try {
                String line;
                var in = new BufferedReader(new InputStreamReader(stream));
                while ((line = in.readLine()) != null) {
                    successful_ &= processor.apply(line);
                }
            } catch (Exception e) {
                // ignore
            }
        });
        processor_thread.start();
        return processor_thread;
    }

    /**
     * Configures the operation from a {@link BaseProject}.
     *
     * @param project the project to configure the operation from
     * @since 1.5
     */
    abstract public T fromProject(BaseProject project);

    /**
     * Provides the work directory in which the operation will be performed.
     * <p>
     * If no work directory is provided, the JVM working directory will be used.
     *
     * @param directory the directory to use as a work directory
     * @return this operation instance
     * @since 1.5
     */
    public T workDirectory(File directory) {
        if (!directory.exists()) {
            throw new OperationOptionException("ERROR: The work directory '" + directory + "' doesn't exist.");
        }
        if (!directory.isDirectory()) {
            throw new OperationOptionException("ERROR: '" + directory + "' is not a directory.");
        }
        if (!directory.canWrite()) {
            throw new OperationOptionException("ERROR: The work directory '" + directory + "' is not writable.");
        }

        workDirectory_ = directory;
        return (T) this;
    }

    /**
     * Provides the name of the tool to use for {@code java} execution.
     * <p>
     * If no java tool is provided {@code java} will be used.
     *
     * @param tool the name of the java tool
     * @return this operation instance
     * @since 1.5
     */
    public T javaTool(String tool) {
        javaTool_ = tool;
        return (T) this;
    }

    /**
     * Provides the options to provide to the java tool.
     *
     * @param options the java tool's options
     * @return this operation instance
     * @since 1.5
     */
    public T javaOptions(List<String> options) {
        javaOptions_.addAll(options);
        return (T) this;
    }

    /**
     * Provides classpath entries to use for the operation.
     *
     * @param classpath classpath entries for the operation
     * @return this operation instance
     * @since 1.5.18
     */
    public T classpath(String... classpath) {
        classpath_.addAll(List.of(classpath));
        return (T) this;
    }

    /**
     * Provides a list of classpath entries to use for the  operation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param classpath a list of classpath entries for the operation
     * @return this operation instance
     * @since 1.5
     */
    public T classpath(List<String> classpath) {
        classpath_.addAll(classpath);
        return (T) this;
    }

    /**
     * Provides the main class to launch with the java tool.
     *
     * @param name the main class to launch
     * @return this operation instance
     * @since 1.5
     */
    public T mainClass(String name) {
        mainClass_ = name;
        return (T) this;
    }

    /**
     * Provides the processor that will be used to handle the process output.
     * <p>
     * It will be called for each line in the output.
     *
     * @param processor the output processor
     * @return this operation instance
     * @since 1.5.1
     */
    public T outputProcessor(Function<String, Boolean> processor) {
        outputProcessor_ = processor;
        return (T) this;
    }

    /**
     * Provides the processor that will be used to handle the process errors.
     * <p>
     * It will be called for each line in the error output.
     *
     * @param processor the error processor
     * @return this operation instance
     * @since 1.5.1
     */
    public T errorProcessor(Function<String, Boolean> processor) {
        errorProcessor_ = processor;
        return (T) this;
    }

    /**
     * Retrieves the work directory in which the operation will be performed.
     *
     * @return the directory to use as a work directory
     * @since 1.5
     */
    public File workDirectory() {
        return workDirectory_;
    }

    /**
     * retrieves the name of the tool to use for {@code java} execution.
     *
     * @return the name of the java tool
     * @since 1.5
     */
    public String javaTool() {
        return javaTool_;
    }

    /**
     * Retrieves the options to provide to the java tool.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the java tool's options
     * @since 1.5
     */
    public JavaOptions javaOptions() {
        return javaOptions_;
    }

    /**
     * Retrieves the classpath to use for the operation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the operation's classpath
     * @since 1.5
     */
    public List<String> classpath() {
        return classpath_;
    }

    /**
     * Retrieves the main class to launch with the java tool.
     *
     * @return the main class to launch
     * @since 1.5
     */
    public String mainClass() {
        return mainClass_;
    }

    /**
     * Retrieves the processor that is used to handle the process output.
     *
     * @return the output processor
     * @since 1.5.1
     */
    public Function<String, Boolean> outputProcessor() {
        return outputProcessor_;
    }

    /**
     * Retrieves the processor that is used to handle the process errors.
     *
     * @return the error processor
     * @since 1.5.1
     */
    public Function<String, Boolean> errorProcessor() {
        return errorProcessor_;
    }

    /**
     * Retrieves the process that was used for the execution.
     *
     * @return the process that was executed
     * @since 1.5
     */
    public Process process() {
        return process_;
    }
}
