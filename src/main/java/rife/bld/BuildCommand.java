/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import java.lang.annotation.*;

/**
 * Declares a {@link BuildExecutor} method to be used as a build command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface BuildCommand {
    /**
     * When provided, specifies a name for the build command that can be
     * different from the method name.
     *
     * @return a string representing the build command name
     * @since 1.5
     */
    String value() default "";

    /**
     * When provided, specifies a short description about the command.
     *
     * @return the short summary, defaults to {@code ""}
     * @since 1.5.12
     */
    String summary() default "";

    /**
     * When provided, specifies the full help description of a command.
     *
     * @return the full help description, defaults to {@code ""}
     * @since 1.5.12
     */
    String description() default "";

    /**
     * When provided, specifies a class that provides help about the
     * build command.
     *
     * @return a class providing help information
     * @since 1.5
     */
    Class<? extends CommandHelp> help() default CommandHelp.class;
}
