/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

/**
 * Defines the logic for a build command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public interface CommandDefinition {
    /**
     * Called when a build command is executed.
     *
     * @throws Throwable when an error occurred during the execution of the build command
     * @since 1.5
     */
    void execute() throws Throwable;

    /**
     * Retrieves the help information of a build command.
     * <p>
     * Defaults to blank help sections.
     *
     * @return this build command's help information
     * @since 1.5
     */
    default CommandHelp getHelp() {
        return new CommandHelp() {};
    }
}
