/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Create run-time images using the jlink tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.1.0
 */
public class JlinkOperation extends AbstractToolProviderOperation<JlinkOperation> {
    private final List<String> disabledPlugins_ = new ArrayList<>();
    private final JlinkOptions jlinkOptions_ = new JlinkOptions();

    public JlinkOperation() {
        super("jlink");
    }

    /**
     * Disable the plugin(s) mentioned.
     *
     * @param plugins the plugin name(s)
     * @return this map of options
     */
    public JlinkOperation disablePlugin(List<String> plugins) {
        disabledPlugins_.addAll(plugins);
        return this;
    }

    /**
     * Disable the plugin(s) mentioned.
     *
     * @param plugins the plugin name(s)
     * @return this map of options
     */
    public JlinkOperation disablePlugin(String... plugins) {
        return disablePlugin(List.of(plugins));
    }

    @Override
    public void execute() throws Exception {
        toolArgsFromFiles();
        disabledPlugins_.forEach(plugin -> toolArgs("--disable-plugin", plugin));
        toolArgs(jlinkOptions_);
        super.execute();
    }

    /**
     * Provides a list of options to provide to the jlink tool.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param options the argument-value pairs
     * @return this operation instance
     */
    public JlinkOperation jlinkOptions(Map<String, String> options) {
        jlinkOptions_.putAll(options);
        return this;
    }

    /**
     * Retrieves the list of options for the jlink tool.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the map of jlink options
     */
    public JlinkOptions jlinkOptions() {
        return jlinkOptions_;
    }
}
