/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

package rife.bld.operations;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Create run-time images using the jlink tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.0.2
 */
public class JlinkOperation extends AbstractToolProviderOperation<JlinkOperation> {
    private final List<String> disabledPlugins_ = new ArrayList<>();
    private final JlinkOptions jlinkOptions_ = new JlinkOptions();
    private final List<String> options_ = new ArrayList<>();

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
        addArgs(jlinkOptions_);
        addArgs(parseOptions());
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
     * List available plugins.
     *
     * @return this operation instance
     */
    public JlinkOperation listPlugins() {
        addArgs("--list-plugins");
        return this;
    }

    /**
     * Read options and/or mode from a file.
     *
     * @param filename one or more file
     * @return this operation instance
     */
    public JlinkOperation options(String... filename) {
        options_.addAll(List.of(filename));
        return this;
    }

    /**
     * Retrieves the list of files containing options or mode.
     *
     * @return the list of files
     */
    public List<String> options() {
        return options_;
    }

    // Shouldn't be needed, but for some reason jlink doesn't like @filename when called via ToolProvider
    private List<String> parseOptions() throws FileNotFoundException {
        var list = new ArrayList<String>();

        for (var option : options_) {
            try (var scanner = new Scanner(new File(option))) {
                while (scanner.hasNext()) {
                    var splitLine = scanner.nextLine().split("--");
                    for (String args : splitLine) {
                        if (!args.isEmpty()) {
                            var splitArgs = args.split(" ", 2);
                            list.add("--" + splitArgs[0]);
                            if (splitArgs.length > 1 && !splitArgs[1].isEmpty()) {
                                list.add(splitArgs[1]);
                            }
                        }
                    }
                }
            }
        }

        return list;
    }
}
