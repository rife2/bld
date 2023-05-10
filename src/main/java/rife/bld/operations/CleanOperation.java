/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.Project;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Cleans by deleting a list of directories and all their contents.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class CleanOperation extends AbstractOperation<CleanOperation> {
    private final List<File> directories_ = new ArrayList<>();

    /**
     * Performs the clean operation.
     *
     * @since 1.5
     */
    public void execute() {
        for (var directory : directories()) {
            executeCleanDirectory(directory);
        }
        if (!silent()) {
            System.out.println("Cleaning finished successfully.");
        }
    }

    /**
     * Part of the {@link #execute} operation, cleans an individual directory.
     *
     * @param directory the directory to clean.
     * @since 1.5
     */
    protected void executeCleanDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (FileUtilsErrorException e) {
            // no-op
        }
    }

    /**
     * Configures a clean operation from a {@link BaseProject}.
     *
     * @param project the project to configure the clean operation from
     * @since 1.5
     */
    public CleanOperation fromProject(BaseProject project) {
        return directories(project.buildDirectory()
            .listFiles(f -> !f.equals(project.buildBldDirectory())));
    }

    /**
     * Provides directories to clean.
     *
     * @param directories directories to clean
     * @return this operation instance
     * @since 1.5.18
     */
    public CleanOperation directories(File... directories) {
        directories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of directories to clean.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of directories to clean
     * @return this operation instance
     * @since 1.5
     */
    public CleanOperation directories(List<File> directories) {
        directories_.addAll(directories);
        return this;
    }

    /**
     * Retrieves the list of directories to clean.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of directories to clean.
     * @since 1.5
     */
    public List<File> directories() {
        return directories_;
    }
}