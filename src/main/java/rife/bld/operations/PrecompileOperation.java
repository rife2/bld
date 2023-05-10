/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.Project;
import rife.template.TemplateDeployer;
import rife.template.TemplateFactory;
import rife.tools.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Pre-compiles RIFE2 templates.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class PrecompileOperation extends AbstractOperation<PrecompileOperation> {
    private final List<TemplateType> templateTypes_ = new ArrayList<>();
    private final List<File> sourceDirectories_ = new ArrayList<>();
    private File destinationDirectory_;

    /**
     * Performs the precompile operation.
     *
     * @since 1.5
     */
    public void execute() {
        if (templateTypes_.isEmpty()) {
            return;
        }

        if (destinationDirectory() != null) {
            destinationDirectory().mkdirs();
        }
        executeCreateTemplateDeployer().execute();
        if (!silent()) {
            System.out.println("Template pre-compilation finished successfully.");
        }
    }

    /**
     * Part of the {@link #execute} operation, gets the template factories for
     * the registered template types.
     *
     * @since 1.5
     */
    protected List<TemplateFactory> executeGetTemplateFactories() {
        var template_factories = new ArrayList<TemplateFactory>();
        for (var type : templateTypes()) {
            var factory = TemplateFactory.getFactory(type.identifier());
            if (factory == null) {
                System.err.println("ERROR: unknown template type '" + type.identifier() + "'/");
            } else {
                template_factories.add(factory);
            }
        }

        return template_factories;
    }

    /**
     * Part of the {@link #execute} operation, creates the {@code TemplateDeployer}
     * that will precompile the templates.
     *
     * @since 1.5
     */
    protected TemplateDeployer executeCreateTemplateDeployer() {
        return new TemplateDeployer()
            .verbose(true)
            .directoryPaths(FileUtils.combineToAbsolutePaths(sourceDirectories()))
            .generationPath(destinationDirectory().getAbsolutePath())
            .templateFactories(executeGetTemplateFactories());
    }

    /**
     * Configures a precompile operation from a {@link BaseProject}.
     *
     * @param project the project to configure the precompile operation from
     * @since 1.5
     */
    public PrecompileOperation fromProject(BaseProject project) {
        return sourceDirectories(project.srcMainResourcesTemplatesDirectory())
            .destinationDirectory(project.buildTemplatesDirectory());
    }

    /**
     * Provides template types that will be pre-compiled.
     *
     * @param types pre-compiled template types
     * @return this operation instance
     * @since 1.5.18
     */
    public PrecompileOperation templateTypes(TemplateType... types) {
        templateTypes_.addAll(List.of(types));
        return this;
    }

    /**
     * Provides a list of template types that will be pre-compiled.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param types a list of pre-compiled template types
     * @return this operation instance
     * @since 1.5
     */
    public PrecompileOperation templateTypes(List<TemplateType> types) {
        templateTypes_.addAll(types);
        return this;
    }

    /**
     * Provides source directories that will be used for the template pre-compilation.
     *
     * @param sources source directories
     * @return this operation instance
     * @since 1.5.18
     */
    public PrecompileOperation sourceDirectories(File... sources) {
        sourceDirectories_.addAll(List.of(sources));
        return this;
    }

    /**
     * Provides a list of source directories that will be used for the template pre-compilation.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param sources a list of source directories
     * @return this operation instance
     * @since 1.5
     */
    public PrecompileOperation sourceDirectories(List<File> sources) {
        sourceDirectories_.addAll(sources);
        return this;
    }

    /**
     * Provides the destination directory in which the pre-compiled templates will be stored.
     *
     * @param directory the pre-compilation destination directory
     * @return this operation instance
     * @since 1.5
     */
    public PrecompileOperation destinationDirectory(File directory) {
        destinationDirectory_ = directory;
        return this;
    }

    /**
     * Retrieves the template types that will be pre-compiled.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the pre-compiled template types
     * @since 1.5
     */
    public List<TemplateType> templateTypes() {
        return templateTypes_;
    }

    /**
     * Retrieves the source directories that will be used for the template pre-compilation.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the source directories
     * @since 1.5
     */
    public List<File> sourceDirectories() {
        return sourceDirectories_;
    }

    /**
     * Provides the destination directory in which the pre-compiled templates will be stored.
     *
     * @return the pre-compilation destination directory
     * @since 1.5
     */
    public File destinationDirectory() {
        return destinationDirectory_;
    }
}
