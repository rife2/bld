/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.NamedFile;
import rife.bld.Project;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.regex.Pattern;

/**
 * Creates an uberjar archive of the provided jars and resources.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class UberJarOperation extends AbstractOperation<UberJarOperation> {
    private final List<File> jarSourceFiles_ = new ArrayList<>();
    private final List<NamedFile> sourceDirectories_ = new ArrayList<>();
    private File destinationDirectory_;
    private String destinationFileName_;
    private String mainClass_;

    /**
     * Performs the uberjar operation.
     *
     * @throws IOException when an exception occurred during the uberjar creation process
     * @throws FileUtilsErrorException when an exception occurred during the uberjar creation process
     * @since 1.5
     */
    public void execute()
    throws IOException, FileUtilsErrorException {
        var tmp_dir = Files.createTempDirectory("uberjar").toFile();
        try {
            executeCollectSourceJarContents(tmp_dir);
            executeCollectSourceResources(tmp_dir);
            executeCreateUberJarArchive(tmp_dir);

            if (!silent()) {
                System.out.println("The uberjar archive was created at '" + new File(destinationDirectory(), destinationFileName()) + "'");
            }
        } finally {
            FileUtils.deleteDirectory(tmp_dir);
        }
    }

    /**
     * Part of the {@link #execute} operation, collect the contents of all the source jars.
     *
     * @since 1.5
     */
    protected void executeCollectSourceJarContents(File stagingDirectory)
    throws FileUtilsErrorException {
        for (var jar : jarSourceFiles()) {
            FileUtils.unzipFile(jar, stagingDirectory);
        }
    }

    /**
     * Part of the {@link #execute} operation, collect the source resources.
     *
     * @since 1.5
     */
    protected void executeCollectSourceResources(File stagingDirectory)
    throws FileUtilsErrorException {
        for (var named_file : sourceDirectories()) {
            if (named_file.file().exists()) {
                var destination_file = new File(stagingDirectory, named_file.name());
                destination_file.mkdirs();
                FileUtils.copyDirectory(named_file.file(), destination_file);
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, create the uberjar archive.
     *
     * @since 1.5
     */
    protected void executeCreateUberJarArchive(File stagingDirectory)
    throws IOException {
        var existing_manifest = new File(new File(stagingDirectory, "META-INF"), "MANIFEST.MF");
        existing_manifest.delete();

        new JarOperation()
            .manifestAttributes(Map.of(
                Attributes.Name.MANIFEST_VERSION, "1.0",
                Attributes.Name.MAIN_CLASS, mainClass()))
            .sourceDirectories(stagingDirectory)
            .destinationDirectory(destinationDirectory())
            .destinationFileName(destinationFileName())
            .excluded(List.of(Pattern.compile("(?:(?:^.*[/\\\\])|^)\\.DS_Store$")))
            .silent(true)
            .execute();
    }

    /**
     * Configures an uberjar operation from a {@link BaseProject}.
     *
     * @param project the project to configure the uberjar operation from
     * @since 1.5
     */
    public UberJarOperation fromProject(BaseProject project) {
        var jars = new ArrayList<>(project.compileClasspathJars());
        jars.addAll(project.runtimeClasspathJars());
        jars.add(new File(project.buildDistDirectory(), project.jarFileName()));

        return jarSourceFiles(jars)
            .destinationDirectory(project.buildDistDirectory())
            .destinationFileName(project.uberJarFileName())
            .mainClass(project.uberJarMainClass());
    }

    /**
     * Provides source jar files that will be used for the uberjar archive creation.
     *
     * @param files source files
     * @return this operation instance
     * @since 1.5.18
     */
    public UberJarOperation jarSourceFiles(File... files) {
        jarSourceFiles_.addAll(List.of(files));
        return this;
    }

    /**
     * Provides a list of source jar files that will be used for the uberjar archive creation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param files a list of jar source files
     * @return this operation instance
     * @since 1.5
     */
    public UberJarOperation jarSourceFiles(List<File> files) {
        jarSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides source directories that will be used for the uberjar archive creation.
     *
     * @param directories source directories
     * @return this operation instance
     * @since 1.5.18
     */
    public UberJarOperation sourceDirectories(NamedFile... directories) {
        sourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of source directories that will be used for the uberjar archive creation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of source directories
     * @return this operation instance
     * @since 1.5
     */
    public UberJarOperation sourceDirectories(List<NamedFile> directories) {
        sourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides the destination directory in which the uberjar archive will be created.
     *
     * @param directory the uberjar destination directory
     * @return this operation instance
     * @since 1.5
     */
    public UberJarOperation destinationDirectory(File directory) {
        destinationDirectory_ = directory;
        return this;
    }

    /**
     * Provides the destination file name that will be used for the uberjar archive creation.
     *
     * @param name the uberjar archive destination file name
     * @return this operation instance
     * @since 1.5
     */
    public UberJarOperation destinationFileName(String name) {
        destinationFileName_ = name;
        return this;
    }

    /**
     * Provides the main class to run from the uberjar archive.
     *
     * @param name the main class to run
     * @return this operation instance
     * @since 1.5
     */
    public UberJarOperation mainClass(String name) {
        mainClass_ = name;
        return this;
    }

    /**
     * Retrieves the list of jar source files that will be used for the
     * uberjar archive creation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the uberjar archive's jar source files
     * @since 1.5
     */
    public List<File> jarSourceFiles() {
        return jarSourceFiles_;
    }

    /**
     * Retrieves the list of source directories that will be used for the
     * uberjar archive creation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the uberjar archive's source directories
     * @since 1.5
     */
    public List<NamedFile> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Retrieves the destination directory in which the uberjar archive will
     * be created.
     *
     * @return the uberjar archive's destination directory
     * @since 1.5
     */
    public File destinationDirectory() {
        return destinationDirectory_;
    }

    /**
     * Retrieves the destination file name that will be used for the uberjar
     * archive creation.
     *
     * @return the uberjar archive's destination file name
     * @since 1.5
     */
    public String destinationFileName() {
        return destinationFileName_;
    }

    /**
     * Retrieves the main class to run from the uberjar archive.
     *
     * @return the main class to run
     * @since 1.5
     */
    public String mainClass() {
        return mainClass_;
    }
}
