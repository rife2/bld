/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.bld.help.*;
import rife.bld.operations.*;
import rife.bld.publish.PublishArtifact;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Provides the configuration and commands of a Java web project for the
 * build system.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class WebProject extends Project {
    /**
     * The main war archive creation.
     *
     * @see #warFileName()
     * @since 1.5.0
     */
    protected String warFileName = null;
    /**
     * The main webapp directory.
     *
     * @see #srcMainWebappDirectory()
     * @since 1.5
     */
    protected File srcMainWebappDirectory = null;

    /*
     * Standard build commands
     */

    private final WarOperation warOperation_ = new WarOperation();

    /**
     * Retrieves the project's default war operation.
     *
     * @return the default war operation instance
     * @since 1.5.18
     */
    protected WarOperation warOperation() {
        return warOperation_;
    }

    /**
     * Standard build command, creates an UberJar archive for the web project.
     *
     * @since 1.5
     */
    @Override
    public void uberjar()
    throws Exception {
        jar();
        uberJarOperation().executeOnce(() -> {
            uberJarOperation().fromProject(this);
            uberJarOperation().sourceDirectories(List.of(new NamedFile("webapp", srcMainWebappDirectory())));
            uberJarOperation().jarSourceFiles().addAll(standaloneClasspathJars());
        });
    }

    /**
     * Standard build command, creates a war archive for the web project.
     *
     * @since 1.5
     */
    @BuildCommand(help = WarHelp.class)
    public void war()
    throws Exception {
        jar();
        warOperation().executeOnce(() -> warOperation().fromProject(this));
    }

    /**
     * Standard publish-web command, uploads artifacts to the publication repository.
     *
     * @since 1.5.7
     */
    @BuildCommand(help = PublishWebHelp.class)
    public void publish()
    throws Exception {
        jar();
        jarSources();
        jarJavadoc();
        uberjar();
        war();
        publishOperation().executeOnce(() -> {
            publishOperation().fromProject(this);
            publishOperation().artifacts().add(new PublishArtifact(new File(buildDistDirectory(), uberJarFileName()), "uber", "jar"));
            publishOperation().artifacts().add(new PublishArtifact(new File(buildDistDirectory(), warFileName()), "", "war"));
        });
    }

    /*
     * Project directories
     */

    @Override
    public File libStandaloneDirectory() {
        return Objects.requireNonNullElseGet(libStandaloneDirectory, () -> new File(libDirectory(), "standalone"));
    }

    /**
     * Returns the project main webapp directory.
     * Defaults to {@code "webapp"} relative to {@link #srcMainDirectory()}.
     *
     * @since 1.5
     */
    public File srcMainWebappDirectory() {
        return Objects.requireNonNullElseGet(srcMainWebappDirectory, () -> new File(srcMainDirectory(), "webapp"));
    }

    /*
     * Project options
     */

    /**
     * Returns filename to use for the main war archive creation.
     * By default, appends the version and the {@code war} extension to the {@link #archiveBaseName()}.
     *
     * @since 1.5.0
     */
    public String warFileName() {
        return Objects.requireNonNullElseGet(warFileName, () -> archiveBaseName() + "-" + version() + ".war");
    }
}
