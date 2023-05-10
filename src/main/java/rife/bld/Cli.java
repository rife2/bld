/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.bld.help.*;
import rife.bld.operations.*;

/**
 * Implements the RIFE2 CLI build executor that is available when running
 * the RIFE2 jar as an executable jar.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class Cli extends BuildExecutor {
    private final CreateBaseOperation createBaseOperation_ = new CreateBaseOperation();
    private final CreateBlankOperation createBlankOperation_ = new CreateBlankOperation();
    private final CreateLibOperation createLibOperation_ = new CreateLibOperation();
    private final CreateRife2Operation createRife2Operation_ = new CreateRife2Operation();
    private final UpgradeOperation upgradeOperation_ = new UpgradeOperation();
    private final VersionOperation versionOperation_ = new VersionOperation();

    /**
     * The standard {@code create} command.
     *
     * @throws Exception when an error occurred during the creation process
     * @since 1.5
     */
    @BuildCommand(help = CreateRife2Help.class)
    public void create()
    throws Exception {
        createRife2Operation_.executeOnce(() -> createRife2Operation_.fromArguments(arguments()));
    }

    /**
     * The standard {@code create-blank} command.
     *
     * @throws Exception when an error occurred during the creation process
     * @since 1.5
     */
    @BuildCommand(value = "create-blank", help = CreateBlankHelp.class)
    public void createBlank()
    throws Exception {
        createBlankOperation_.executeOnce(() -> createBlankOperation_.fromArguments(arguments()));
    }

    /**
     * The standard {@code create-base} command.
     *
     * @throws Exception when an error occurred during the creation process
     * @since 1.5.20
     */
    @BuildCommand(value = "create-base", help = CreateBaseHelp.class)
    public void createBase()
    throws Exception {
        createBaseOperation_.executeOnce(() -> createBaseOperation_.fromArguments(arguments()));
    }

    /**
     * The standard {@code create-lib} command.
     *
     * @throws Exception when an error occurred during the creation process
     * @since 1.6
     */
    @BuildCommand(value = "create-lib", help = CreateLibHelp.class)
    public void createLib()
    throws Exception {
        createLibOperation_.executeOnce(() -> createLibOperation_.fromArguments(arguments()));
    }

    /**
     * The standard {@code upgrade} command.
     *
     * @throws Exception when an error occurred during the upgrade process
     * @since 1.5
     */
    @BuildCommand(help = UpgradeHelp.class)
    public void upgrade()
    throws Exception {
        upgradeOperation_.executeOnce();
    }

    /**
     * The standard {@code version} command.
     *
     * @since 1.5.2
     */
    @BuildCommand(help = VersionHelp.class)
    public void version()
    throws Exception {
        versionOperation_.executeOnce();
    }

    public static void main(String[] arguments)
    throws Exception {
        new Cli().start(arguments);
    }
}
