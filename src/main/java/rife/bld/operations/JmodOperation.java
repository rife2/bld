/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import rife.bld.operations.exceptions.ExitStatusException;

import java.util.Map;

/**
 * Create JMOD files with the jmod tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.0.2
 */
public class JmodOperation extends AbstractToolProviderOperation<JmodOperation> {
    private final JmodOptions jmodOptions_ = new JmodOptions();
    private OperationMode operationMode_;

    public JmodOperation() {
        super("jmod");
    }

    @Override
    public void execute() throws Exception {
        if (operationMode_ == null) {
            System.err.println("Operation mode not set.");
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }
        toolArg(operationMode_.mode);
        toolArgs(jmodOptions_);
        super.execute();
    }

    /**
     * Retrieves the list of options for the jmod tool.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of jmod options
     */
    public JmodOptions jmodOptions() {
        return jmodOptions_;
    }

    /**
     * Provides a list of options to provide to the jmod tool.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param options the list of jmod options
     * @return this operation instance
     */
    public JmodOperation jmodOptions(Map<String, String> options) {
        jmodOptions_.putAll(options);
        return this;
    }

    /**
     * Provides the required {@link OperationMode operation mode}.
     *
     * @param mode the mode
     * @return this operation instance
     */
    public JmodOperation operationMode(OperationMode mode) {
        operationMode_ = mode;
        return this;
    }

    /**
     * The operation modes.
     */
    public enum OperationMode {
        CREATE("create"),
        DESCRIBE("describe"),
        EXTRACT("extract"),
        HASH("hash"),
        LIST("list");

        final String mode;

        OperationMode(String mode) {
            this.mode = mode;
        }
    }
}
