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
 * @since 2.0.2
 */
public class JlinkOperation extends AbstractToolProviderOperation<JlinkOperation> {
    private final JlinkOptions jlinkOptions_ = new JlinkOptions();
    private final List<String> disabledPlugins_ = new ArrayList<>();

    public JlinkOperation() {
        super("jlink");
    }

    /**
     * Disable the plugin mentioned.
     *
     * @param plugin the plugin name
     * @return this map of options
     */
    public JlinkOperation disablePlugin(String... plugin) {
        disabledPlugins_.addAll(List.of(plugin));
        return this;
    }

    @Override
    public void execute() throws Exception {
        disabledPlugins_.forEach(plugin -> addArg("--disable-plugin", plugin));
        super.execute();
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

    /**
     * List available plugins.
     *
     * @return this operation instance
     */
    public JlinkOperation listPlugins() {
        addArgs("--list-plugins");
        return this;
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
}
