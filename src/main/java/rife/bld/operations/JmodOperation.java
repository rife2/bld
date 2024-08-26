/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * Create JMOD files with the jmod tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.1.0
 */
public class JmodOperation extends AbstractToolProviderOperation<JmodOperation> {
    private final JmodOptions jmodOptions_ = new JmodOptions();
    private String jmodFile_;
    private OperationMode operationMode_;

    public JmodOperation() {
        super("jmod");
    }

    @Override
    public void execute() throws Exception {
        if (operationMode_ != null) {
            toolArgs(operationMode_.mode);
        }

        toolArgsFromFiles();
        toolArgs(jmodOptions_);

        if (jmodFile_ != null) {
            toolArgs(jmodFile_);
        }

        super.execute();
    }

    /**
     * Retrieves the name of the JMOD file to create or from which to retrieve information.
     *
     * @return the JMOD file
     */
    public String jmodFile() {
        return jmodFile_;
    }

    /**
     * Specifies name of the JMOD file to create or from which to retrieve information.
     * <p>
     * The JMOD file is <b>required</b>.
     *
     * @param file the JMOD file
     * @return this operation instance
     */
    public JmodOperation jmodFile(String file) {
        jmodFile_ = file;
        return this;
    }

    /**
     * Specifies name of the JMOD file to create or from which to retrieve information.
     * <p>
     * The JMOD file is <b>required</b>.
     *
     * @param file the JMOD file
     * @return this operation instance
     */
    public JmodOperation jmodFile(File file) {
        jmodFile_ = file.getAbsolutePath();
        return this;
    }

    /**
     * Specifies name of the JMOD file to create or from which to retrieve information.
     * <p>
     * The JMOD file is <b>required</b>.
     *
     * @param file the JMOD file
     * @return this operation instance
     */
    public JmodOperation jmodFile(Path file) {
        return jmodFile(file.toFile());
    }

    /**
     * Retrieves the list of options for the jmod tool.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the map of jmod options
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
     * Provides the {@link OperationMode operation mode}.
     * <p>
     * The operation mode is <b>required</b>.
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
        /**
         * Creates a new JMOD archive file.
         */
        CREATE("create"),
        /**
         * Prints the module details.
         */
        DESCRIBE("describe"),
        /**
         * Extracts all the files from the JMOD archive file.
         */
        EXTRACT("extract"),
        /**
         * Determines leaf modules and records the hashes of the dependencies that directly and indirectly require them.
         */
        HASH("hash"),
        /**
         * Prints the names of all the entries.
         */
        LIST("list");

        final String mode;

        OperationMode(String mode) {
            this.mode = mode;
        }
    }
}
