/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Package self-contained Java applications with the jpackage tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.1.0
 */
public class JpackageOperation extends AbstractToolProviderOperation<JpackageOperation> {
    private final List<String> cmdFiles_ = new ArrayList<>();
    private final JpackageOptions jpackageOptions_ = new JpackageOptions();
    private final List<Launcher> launchers_ = new ArrayList<>();

    public JpackageOperation() {
        super("jpackage");
    }

    /**
     * List of application launchers.
     * <p>
     * The main application launcher will be built from the command line options.
     * <p>
     * Additional alternative launchers can be built using this option, and this option can be used to build multiple
     * additional launchers.
     *
     * @param launcher one or more {@link JpackageOperation.Launcher}
     * @return this operation instance
     */
    public JpackageOperation addLauncher(Launcher... launcher) {
        launchers_.addAll(Arrays.asList(launcher));
        return this;
    }

    /**
     * Read options and/or mode from file(s).
     *
     * @param file one or more file
     * @return this operation instance
     */
    public JpackageOperation cmdFiles(File... file) {
        cmdFiles_.addAll(Arrays.stream(file).map(File::getAbsolutePath).toList());
        return this;
    }

    /**
     * Read options and/or mode from file(s).
     *
     * @param file one or more file
     * @return this operation instance
     */
    public JpackageOperation cmdFiles(Path... file) {
        cmdFiles_.addAll(Arrays.stream(file).map(Path::toString).toList());
        return this;
    }

    /**
     * Read options and/or mode from file(s).
     *
     * @param file one or more file
     * @return this operation instance
     */
    public JpackageOperation cmdFiles(String... file) {
        cmdFiles_.addAll(List.of(file));
        return this;
    }

    /**
     * Retrieves the list of files containing options or mode.
     *
     * @return the list of files
     */
    public List<String> cmdFiles() {
        return cmdFiles_;
    }

    @Override
    public void execute() throws Exception {
        toolArgs(cmdFiles_.stream().map(opt -> '@' + opt).toList());
        for (var l : launchers_) {
            toolArgs("--add-launcher", l.name + '=' + l.path);
        }
        toolArgs(jpackageOptions_);
        super.execute();
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

    /**
     * Retrieves the list of application launchers.
     *
     * @return the list of launchers
     */
    public List<Launcher> launchers() {
        return launchers_;
    }

    /**
     * Name of launcher, and a path to a Properties file that contains a list of key, value pairs.
     * <p>
     * The keys {@code module}, {@code main-jar}, {@code main-class}, {@code description},
     * {@code arguments}, {@code java-options}, {@code app-version}, {@code icon},
     * {@code launcher-as-service}, {@code win-console}, {@code win-shortcut}, {@code win-menu},
     * {@code linux-app-category}, and {@code linux-shortcut} can be used.
     * <p>
     * These options are added to, or used to overwrite, the original command line options to build an additional
     * alternative launcher.
     *
     * @param name the name
     * @param path absolute path or relative to the current directory
     */
    public record Launcher(String name, String path) {
        public Launcher(String name, File path) {
            this(name, path.getAbsolutePath());
        }

        public Launcher(String name, Path path) {
            this(name, path.toString());
        }
    }
}
