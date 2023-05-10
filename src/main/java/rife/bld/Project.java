/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.bld.help.*;
import rife.bld.operations.*;

import java.util.*;
import java.util.jar.Attributes;

/**
 * Provides the configuration and commands of a Java project for the
 * build system with all standard commands ready to go.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class Project extends BaseProject {
    /*
     * Standard build commands
     */

    private final JavadocOperation javadocOperation_ = new JavadocOperation();
    private final PrecompileOperation precompileOperation_ = new PrecompileOperation();
    private final JarOperation jarOperation_ = new JarOperation();
    private final JarOperation jarSourcesOperation_ = new JarOperation();
    private final JarOperation jarJavadocOperation_ = new JarOperation();
    private final JUnitOperation junitTestOperation_ = new JUnitOperation();
    private final UberJarOperation uberJarOperation_ = new UberJarOperation();

    @Override
    public JUnitOperation testOperation() {
        return junitTestOperation_;
    }

    /**
     * Retrieves the project's default javadoc operation.
     *
     * @return the default javadoc operation instance
     * @since 1.5.18
     */
    public JavadocOperation javadocOperation() {
        return javadocOperation_;
    }

    /**
     * Retrieves the project's default precompile operation.
     *
     * @return the default precompile operation instance
     * @since 1.5.18
     */
    public PrecompileOperation precompileOperation() {
        return precompileOperation_;
    }

    /**
     * Retrieves the project's default jar operation.
     *
     * @return the default jar operation instance
     * @since 1.5.18
     */
    public JarOperation jarOperation() {
        return jarOperation_;
    }

    /**
     * Retrieves the project's default jar operation for sources.
     *
     * @return the default jar operation instance for sources
     * @since 1.5.18
     */
    public JarOperation jarSourcesOperation() {
        return jarSourcesOperation_;
    }

    /**
     * Retrieves the project's default jar operation for javadoc.
     *
     * @return the default jar operation instance for javadoc
     * @since 1.5.18
     */
    public JarOperation jarJavadocOperation() {
        return jarJavadocOperation_;
    }

    /**
     * Retrieves the project's default uberjar operation.
     *
     * @return the default uberjar operation instance
     * @since 1.5.18
     */
    public UberJarOperation uberJarOperation() {
        return uberJarOperation_;
    }

    @BuildCommand(help = JUnitHelp.class)
    @Override
    public void test()
    throws Exception {
        super.test();
    }

    /**
     * Standard build command, generates javadoc.
     *
     * @since 1.5.10
     */
    @BuildCommand(help = JavadocHelp.class)
    public void javadoc()
    throws Exception {
        javadocOperation().executeOnce(() -> javadocOperation().fromProject(this));
    }

    /**
     * Standard build command, pre-compiles RIFE2 templates to class files.
     *
     * @since 1.5
     */
    @BuildCommand(help = PrecompileHelp.class)
    public void precompile()
    throws Exception {
        precompileOperation().executeOnce(() -> precompileOperation().fromProject(this));
    }

    /**
     * Standard build command, creates a jar archive for the project.
     *
     * @since 1.5
     */
    @BuildCommand(help = JarHelp.class)
    public void jar()
    throws Exception {
        compile();
        precompile();
        jarOperation().executeOnce(() -> jarOperation().fromProject(this));
    }

    /**
     * Standard build command, creates a sources jar archive for the project.
     *
     * @since 1.5.10
     */
    @BuildCommand(value = "jar-sources", help = JarSourcesHelp.class)
    public void jarSources()
    throws Exception {
        jarSourcesOperation().executeOnce(() -> jarSourcesOperation()
            .manifestAttributes(Map.of(Attributes.Name.MANIFEST_VERSION, "1.0"))
            .sourceDirectories(List.of(srcMainJavaDirectory()))
            .destinationDirectory(buildDistDirectory())
            .destinationFileName(sourcesJarFileName()));
    }

    /**
     * Standard build command, creates a javadoc jar archive for the project.
     *
     * @since 1.5.10
     */
    @BuildCommand(value = "jar-javadoc", help = JarJavadocHelp.class)
    public void jarJavadoc()
    throws Exception {
        compile();
        javadoc();
        jarJavadocOperation().executeOnce(() -> jarJavadocOperation().manifestAttributes(Map.of(Attributes.Name.MANIFEST_VERSION, "1.0"))
            .sourceDirectories(List.of(buildJavadocDirectory()))
            .destinationDirectory(buildDistDirectory())
            .destinationFileName(javadocJarFileName()));
    }
    /**
     * Standard build command, creates an UberJar archive for the project.
     *
     * @since 1.5
     */
    @BuildCommand(help = UberJarHelp.class)
    public void uberjar()
    throws Exception {
        jar();
        uberJarOperation().executeOnce(() -> uberJarOperation().fromProject(this));
    }

    /**
     * Standard publish command, uploads artifacts to the publication repository.
     *
     * @since 1.5.7
     */
    @BuildCommand(help = PublishHelp.class)
    public void publish()
    throws Exception {
        jar();
        jarSources();
        jarJavadoc();
        publishOperation().executeOnce(() -> publishOperation().fromProject(this));
    }
}
