/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Package self-contained Java applications with the jpackage tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.0.2
 */
public class JpackageOperation extends AbstractToolProviderOperation<JpackageOperation> {
    private final JpackageOptions jpackageOptions_ = new JpackageOptions();
    private final List<String> options_ = new ArrayList<>();

    public JpackageOperation() {
        super("jpackage");
    }

    @Override
    public void execute() throws Exception {
        addArgs(jpackageOptions_);
        addArgs(options_.stream().map(opt -> '@' + opt).toList());
        super.execute();
    }

    /**
     * Retrieves the list of files containing options or mode.
     *
     * @return the list of files
     */
    public List<String> options(){
        return options_;
    }
    /**
     * Read options and/or mode from a file.
     *
     * @param filename one or more file
     * @return this operation instance
     */
    public JpackageOperation options(String... filename) {
        options_.addAll(List.of(filename));
        return this;
    }

    /**
     * Retrieves the list of options for the jpackage tool.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the map of jpackage options
     */
    public JpackageOptions jpackageOptions() {
        return jpackageOptions_;
    }

    /**
     * Provides a list of options to provide to the jpackage tool.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param options the map of jpackage options
     * @return this operation instance
     */
    public JpackageOperation jpackageOptions(Map<String, String> options) {
        jpackageOptions_.putAll(options);
        return this;
    }
}
