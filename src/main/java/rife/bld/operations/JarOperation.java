/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.NamedFile;
import rife.bld.Project;
import rife.tools.FileUtils;
import rife.tools.StringUtils;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Pattern;

/**
 * Creates a jar archive of the provided sources and directories.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class JarOperation extends AbstractOperation<JarOperation> {
    private final Map<Attributes.Name, Object> manifestAttributes_ = new HashMap<>();
    private final List<File> sourceDirectories_ = new ArrayList<>();
    private final List<NamedFile> sourceFiles_ = new ArrayList<>();
    private File destinationDirectory_;
    private String destinationFileName_;
    private final List<Pattern> included_ = new ArrayList<>();
    private final List<Pattern> excluded_ = new ArrayList<>();

    private final byte[] buffer_ = new byte[1024];

    /**
     * Performs the jar operation.
     *
     * @throws IOException when an exception occurred during the jar creation process
     * @since 1.5
     */
    public void execute()
    throws IOException {
        executeCreateDestinationDirectory();
        executeCreateJarArchive();

        if (!silent()) {
            System.out.println("The jar archive was created at '" + destinationFile() + "'");
        }
    }

    /**
     * Part of the {@link #execute} operation, create the destination directory.
     *
     * @since 1.5
     */
    protected void executeCreateDestinationDirectory() {
        destinationDirectory().mkdirs();
    }

    /**
     * Part of the {@link #execute} operation, create the jar archive.
     *
     * @since 1.5
     */
    protected void executeCreateJarArchive()
    throws IOException {
        var out_file = new File(destinationDirectory(), destinationFileName());
        try (var jar = new JarOutputStream(new FileOutputStream(out_file), executeCreateManifest())) {
            for (var source_dir : sourceDirectories()) {
                for (var file_name : FileUtils.getFileList(source_dir)) {
                    var file = new File(source_dir, file_name);
                    if (StringUtils.filter(file.getAbsolutePath(), included(), excluded(), false)) {
                        executeAddFileToJar(jar, new NamedFile(file_name, file));
                    }
                }
            }
            for (var source_file : sourceFiles()) {
                if (StringUtils.filter(source_file.file().getAbsolutePath(), included(), excluded(), false)) {
                    executeAddFileToJar(jar, source_file);
                }
            }
            jar.flush();
        }
    }

    /**
     * Part of the {@link #execute} operation, create the manifest for the jar archive.
     *
     * @since 1.5
     */
    protected Manifest executeCreateManifest() {
        var manifest = new Manifest();
        var attributes = manifest.getMainAttributes();
        for (var entry : manifestAttributes().entrySet()) {
            // don't use putAll since Attributes does an instanceof check
            // on the map being passed in, causing it to fail if it's not
            // and instance of Attributes
            attributes.put(entry.getKey(), entry.getValue());
            // ^^^ READ above, don't use putAll
        }
        return manifest;
    }

    /**
     * Part of the {@link #execute} operation, add a single file to the jar archive.
     *
     * @since 1.5
     */
    protected void executeAddFileToJar(JarOutputStream jar, NamedFile file)
    throws IOException {
        var entry = new JarEntry(file.name().replace('\\', '/'));
        entry.setTime(file.file().lastModified());
        jar.putNextEntry(entry);

        try (var in = new BufferedInputStream(new FileInputStream(file.file()))) {
            int count;
            while ((count = in.read(buffer_)) != -1) {
                jar.write(buffer_, 0, count);
            }
            jar.closeEntry();
        }
    }

    /**
     * Configures a jar operation from a {@link BaseProject}.
     *
     * @param project the project to configure the jar operation from
     * @since 1.5
     */
    public JarOperation fromProject(BaseProject project) {
        return manifestAttributes(Map.of(Attributes.Name.MANIFEST_VERSION, "1.0"))
            .sourceDirectories(project.buildMainDirectory(), project.srcMainResourcesDirectory())
            .destinationDirectory(project.buildDistDirectory())
            .destinationFileName(project.jarFileName())
            .excluded(Pattern.compile("(?:(?:^.*[/\\\\])|^)\\.DS_Store$"), Pattern.compile("^\\Q" + project.srcMainResourcesTemplatesDirectory().getAbsolutePath() + "\\E.*"));
    }

    /**
     * Provides an attribute to put in the jar manifest.
     *
     * @param name  the attribute name to put in the manifest
     * @param value the attribute value to put in the manifest
     * @return this operation instance
     * @since 1.5.18
     */
    public JarOperation manifestAttribute(Attributes.Name name, Object value) {
        manifestAttributes_.put(name, value);
        return this;
    }

    /**
     * Provides a map of attributes to put in the jar manifest.
     * <p>
     * A copy will be created to allow this map to be independently modifiable.
     *
     * @param attributes the attributes to put in the manifest
     * @return this operation instance
     * @since 1.5
     */
    public JarOperation manifestAttributes(Map<Attributes.Name, Object> attributes) {
        manifestAttributes_.putAll(attributes);
        return this;
    }

    /**
     * Provides source directories that will be used for the jar archive creation.
     *
     * @param directories source directories
     * @return this operation instance
     * @since 1.5.18
     */
    public JarOperation sourceDirectories(File... directories) {
        sourceDirectories_.addAll(List.of(directories));
        return this;
    }

    /**
     * Provides a list of source directories that will be used for the jar archive creation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param directories a list of source directories
     * @return this operation instance
     * @since 1.5
     */
    public JarOperation sourceDirectories(List<File> directories) {
        sourceDirectories_.addAll(directories);
        return this;
    }

    /**
     * Provides source files that will be used for the jar archive creation.
     *
     * @param files source files
     * @return this operation instance
     * @since 1.5.18
     */
    public JarOperation sourceFiles(NamedFile... files) {
        sourceFiles_.addAll(List.of(files));
        return this;
    }

    /**
     * Provides a list of source files that will be used for the jar archive creation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param files a list of source files
     * @return this operation instance
     * @since 1.5
     */
    public JarOperation sourceFiles(List<NamedFile> files) {
        sourceFiles_.addAll(files);
        return this;
    }

    /**
     * Provides the destination directory in which the jar archive will be created.
     *
     * @param directory the jar destination directory
     * @return this operation instance
     * @since 1.5
     */
    public JarOperation destinationDirectory(File directory) {
        destinationDirectory_ = directory;
        return this;
    }

    /**
     * Provides the destination file name that will be used for the jar archive creation.
     *
     * @param name the jar archive destination file name
     * @return this operation instance
     * @since 1.5
     */
    public JarOperation destinationFileName(String name) {
        destinationFileName_ = name;
        return this;
    }

    /**
     * Provides regex patterns that will be found to determine which files
     * will be included in the javadoc generation.
     *
     * @param included inclusion patterns
     * @return this operation instance
     * @since 1.5.18
     */
    public JarOperation included(String... included) {
        included_.addAll(Arrays.stream(included).map(Pattern::compile).toList());
        return this;
    }

    /**
     * Provides patterns that will be found to determine which files
     * will be included in the jar archive.
     *
     * @param included inclusion patterns
     * @return this operation instance
     * @since 1.5.18
     */
    public JarOperation included(Pattern... included) {
        included_.addAll(List.of(included));
        return this;
    }

    /**
     * Provides a list of patterns that will be found to determine which files
     * will be included in the jar archive.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param included a list of inclusion patterns
     * @return this operation instance
     * @since 1.5
     */
    public JarOperation included(List<Pattern> included) {
        included_.addAll(included);
        return this;
    }

    /**
     * Provides regex patterns that will be found to determine which files
     * will be excluded from the javadoc generation.
     *
     * @param excluded exclusion patterns
     * @return this operation instance
     * @since 1.5.18
     */
    public JarOperation excluded(String... excluded) {
        excluded_.addAll(Arrays.stream(excluded).map(Pattern::compile).toList());
        return this;
    }

    /**
     * Provides patterns that will be found to determine which files
     * will be excluded from the jar archive.
     *
     * @param excluded exclusion patterns
     * @return this operation instance
     * @since 1.5.18
     */
    public JarOperation excluded(Pattern... excluded) {
        excluded_.addAll(List.of(excluded));
        return this;
    }

    /**
     * Provides a list of patterns that will be found to determine which files
     * will be excluded from the jar archive.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param excluded a list of exclusion patterns
     * @return this operation instance
     * @since 1.5
     */
    public JarOperation excluded(List<Pattern> excluded) {
        excluded_.addAll(excluded);
        return this;
    }

    /**
     * Retrieves the map of attributes that will be put in the jar manifest.
     * <p>
     * This is a modifiable map that can be retrieved and changed.
     *
     * @return the manifest's attributes map
     * @since 1.5
     */
    public Map<Attributes.Name, Object> manifestAttributes() {
        return manifestAttributes_;
    }

    /**
     * Retrieves the list of source directories that will be used for the
     * jar archive creation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the jar archive's source directories
     * @since 1.5
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Retrieves the list of source files that will be used for the
     * jar archive creation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the jar archive's source files
     * @since 1.5
     */
    public List<NamedFile> sourceFiles() {
        return sourceFiles_;
    }

    /**
     * Retrieves the destination directory in which the jar archive will
     * be created.
     *
     * @return the jar archive's destination directory
     * @since 1.5
     */
    public File destinationDirectory() {
        return destinationDirectory_;
    }

    /**
     * Retrieves the destination file name that will be used for the jar
     * archive creation.
     *
     * @return the jar archive's destination file name
     * @since 1.5
     */
    public String destinationFileName() {
        return destinationFileName_;
    }

    /**
     * Retrieves the destination file where the jar archive will be created.
     *
     * @return the jar archive's destination file
     * @since 1.5.18
     */
    public File destinationFile() {
        return new File(destinationDirectory(), destinationFileName());
    }

    /**
     * Retrieves the list of patterns that will be evaluated to determine which files
     * will be included in the jar archive.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the jar's archive's inclusion patterns
     * @since 1.5
     */
    public List<Pattern> included() {
        return included_;
    }

    /**
     * Retrieves the list of patterns that will be evaluated to determine which files
     * will be excluded the jar archive.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the jar's archive's exclusion patterns
     * @since 1.5
     */
    public List<Pattern> excluded() {
        return excluded_;
    }
}
