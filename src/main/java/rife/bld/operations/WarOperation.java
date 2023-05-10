/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.*;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Creates a war archive.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class WarOperation extends AbstractOperation<WarOperation> {
    private final List<File> libSourceDirectories_ = new ArrayList<>();
    private final List<File> classesSourceDirectories_ = new ArrayList<>();
    private final List<NamedFile> jarSourceFiles_ = new ArrayList<>();
    private File webappDirectory_;
    private File webXmlFile_;
    private File destinationDirectory_;
    private String destinationFileName_;

    /**
     * Performs the war operation.
     *
     * @throws IOException             when an exception occurred during the war creation process
     * @throws FileUtilsErrorException when an exception occurred war the uberjar creation process
     * @since 1.5
     */
    public void execute()
    throws IOException, FileUtilsErrorException {
        var tmp_dir = Files.createTempDirectory("war").toFile();

        try {
            var web_inf_dir = executeCreateWebInfDirectory(tmp_dir);
            executeCopyWebappDirectory(tmp_dir);
            executeCopyWebInfLibJars(web_inf_dir);
            executeCopyWebInfClassesFiles(web_inf_dir);
            executeCopyWebXmlFile(web_inf_dir);

            executeCreateWarArchive(tmp_dir);

            if (!silent()) {
                System.out.println("The war archive was created at '" + new File(destinationDirectory(), destinationFileName()) + "'");
            }
        } finally {
            FileUtils.deleteDirectory(tmp_dir);
        }
    }

    /**
     * Part of the {@link #execute} operation, create the staging {@code WEB-INF} directory.
     *
     * @since 1.5
     */
    protected File executeCreateWebInfDirectory(File stagingDirectory) {
        var web_inf_dir = new File(stagingDirectory, "WEB-INF");
        web_inf_dir.mkdirs();
        return web_inf_dir;
    }

    /**
     * Part of the {@link #execute} operation, create the staging webapp directory.
     *
     * @since 1.5
     */
    protected void executeCopyWebappDirectory(File stagingDirectory)
    throws FileUtilsErrorException {
        if (webappDirectory() != null) {
            FileUtils.copyDirectory(webappDirectory(), stagingDirectory);
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the staging {@code WEB-INF} jars.
     *
     * @since 1.5
     */
    protected void executeCopyWebInfLibJars(File stagingWebInfDirectory)
    throws FileUtilsErrorException {
        var web_inf_lib_dir = new File(stagingWebInfDirectory, "lib");
        if (!libSourceDirectories().isEmpty()) {
            web_inf_lib_dir.mkdirs();
            for (var dir : libSourceDirectories()) {
                FileUtils.copyDirectory(dir, web_inf_lib_dir);
            }
        }

        if (!jarSourceFiles().isEmpty()) {
            web_inf_lib_dir.mkdirs();
            for (var file : jarSourceFiles()) {
                FileUtils.copy(file.file(), new File(web_inf_lib_dir, file.name()));
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the staging {@code WEB-INF} classes.
     *
     * @since 1.5
     */
    protected void executeCopyWebInfClassesFiles(File stagingWebInfDirectory)
    throws FileUtilsErrorException {
        var web_inf_classes_dir = new File(stagingWebInfDirectory, "classes");
        if (!classesSourceDirectories().isEmpty()) {
            web_inf_classes_dir.mkdirs();
            for (var dir : classesSourceDirectories()) {
                FileUtils.copyDirectory(dir, web_inf_classes_dir);
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, copy the staging {@code web.xml} file.
     *
     * @since 1.5
     */
    protected void executeCopyWebXmlFile(File stagingWebInfDirectory)
    throws FileUtilsErrorException {
        if (webXmlFile() != null) {
            FileUtils.copy(webXmlFile(), new File(stagingWebInfDirectory, "web.xml"));
        }
    }

    /**
     * Part of the {@link #execute} operation, create the war archive from the staging directory.
     *
     * @since 1.5
     */
    protected void executeCreateWarArchive(File stagingDirectory)
    throws IOException {
        new JarOperation()
            .sourceDirectories(stagingDirectory)
            .destinationDirectory(destinationDirectory())
            .destinationFileName(destinationFileName())
            .excluded(Pattern.compile("(?:(?:^.*[/\\\\])|^)\\.DS_Store$"))
            .silent(true)
            .execute();
    }

    /**
     * Configures a war operation from a {@link Project}.
     *
     * @param project the project to configure the war operation from
     * @since 1.5
     */
    public WarOperation fromProject(WebProject project) {
        var jar_source_files = new ArrayList<NamedFile>();
        jar_source_files.add(new NamedFile(project.jarFileName(), new File(project.buildDistDirectory(), project.jarFileName())));

        var class_path_jars = new ArrayList<File>();
        class_path_jars.addAll(project.compileClasspathJars());
        class_path_jars.addAll(project.runtimeClasspathJars());
        for (var jar_file : class_path_jars) {
            jar_source_files.add(new NamedFile(jar_file.getName(), jar_file));
        }

        return jarSourceFiles(jar_source_files)
            .webappDirectory(project.srcMainWebappDirectory())
            .destinationDirectory(project.buildDistDirectory())
            .destinationFileName(project.warFileName());
    }

    /**
     * Provides lib source directories that will be used for the war archive creation.
     *
     * @param directories lib source directories
     * @return this operation instance
     * @since 1.5.18
     */
    public WarOperation libSourceDirectories(File... directories) {
        libSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of lib source directories that will be used for the war archive creation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of lib source directories
     * @return this operation instance
     * @since 1.5
     */
    public WarOperation libSourceDirectories(List<File> directories) {
        libSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides classes source directories that will be used for the war archive creation.
     *
     * @param directories classes source directories
     * @return this operation instance
     * @since 1.5.18
     */
    public WarOperation classesSourceDirectories(File... directories) {
        classesSourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of classes source directories that will be used for the war archive creation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of classes source directories
     * @return this operation instance
     * @since 1.5
     */
    public WarOperation classesSourceDirectories(List<File> directories) {
        classesSourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides jar files that will be included in the war archive creation.
     *
     * @param files jar source directories
     * @return this operation instance
     * @since 1.5.18
     */
    public WarOperation jarSourceFiles(NamedFile... files) {
        jarSourceFiles_.addAll(List.of(files));
        return this;
    }

    /**
     * Provides a list of jar files that will be included in the war archive creation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param files a list of jar source directories
     * @return this operation instance
     * @since 1.5
     */
    public WarOperation jarSourceFiles(List<NamedFile> files) {
        jarSourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides web application directory that will provide resources for the war archive creation.
     *
     * @param directory the webapp directory
     * @return this operation instance
     * @since 1.5
     */
    public WarOperation webappDirectory(File directory) {
        webappDirectory_ = directory;
        return this;
    }

    /**
     * Provides web.xml file that will be used for the war archive creation.
     *
     * @param file the web.xml file
     * @return this operation instance
     * @since 1.5
     */
    public WarOperation webXmlFile(File file) {
        webXmlFile_ = file;
        return this;
    }

    /**
     * Provides the destination directory in which the war archive will be created.
     *
     * @param directory the war destination directory
     * @return this operation instance
     * @since 1.5
     */
    public WarOperation destinationDirectory(File directory) {
        destinationDirectory_ = directory;
        return this;
    }

    /**
     * Provides the destination file name that will be used for the war archive creation.
     *
     * @param name the war archive destination file name
     * @return this operation instance
     * @since 1.5
     */
    public WarOperation destinationFileName(String name) {
        destinationFileName_ = name;
        return this;
    }

    /**
     * Retrieves the lib source directories that will be used for the war archive creation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the lib source directories
     * @since 1.5
     */
    public List<File> libSourceDirectories() {
        return libSourceDirectories_;
    }

    /**
     * Retrieves the classes source directories that will be used for the war archive creation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the classes source directories
     * @since 1.5
     */
    public List<File> classesSourceDirectories() {
        return classesSourceDirectories_;
    }

    /**
     * Retrieves jar files that will be included in the war archive creation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the jar source directories
     * @since 1.5
     */
    public List<NamedFile> jarSourceFiles() {
        return jarSourceFiles_;
    }

    /**
     * Retrieves web application directory that will provide resources for the war archive creation.
     *
     * @return the webapp directory
     * @since 1.5
     */
    public File webappDirectory() {
        return webappDirectory_;
    }

    /**
     * Retrieves web.xml file that will be used for the war archive creation.
     *
     * @return the web.xml file
     * @since 1.5
     */
    public File webXmlFile() {
        return webXmlFile_;
    }

    /**
     * Retrieves the destination directory in which the war archive will
     * be created.
     *
     * @return the war archive's destination directory
     * @since 1.5
     */
    public File destinationDirectory() {
        return destinationDirectory_;
    }

    /**
     * Retrieves the destination file name that will be used for the war
     * archive creation.
     *
     * @return the war archive's destination file name
     * @since 1.5
     */
    public String destinationFileName() {
        return destinationFileName_;
    }
}
