/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

/**
 * Interface that provides help texts to display about {@link BuildExecutor} commands.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public interface CommandHelp {
    /**
     * Returns a short description about the command.
     *
     * @return the short summary, defaults to {@code ""}
     * @since 1.5
     */
    default String getSummary() {
        return "";
    }

    /**
     * Returns the full help description of a command.
     *
     * @return the full help description, defaults to {@code ""}
     * @since 1.5
     */
    default String getDescription(String topic) {
        return "";
    }
}
