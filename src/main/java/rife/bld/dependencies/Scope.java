/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

/**
 * Provides all the dependency scopes that are supported by
 * the RIFE2 build system.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public enum Scope {
    /**
     * Used for compiling the main source code.
     * @since 1.5
     */
    compile,

    /**
     * Used when running the main source code.
     * @since 1.5
     */
    runtime,

    /**
     * Used when compiling and running the test source code.
     * @since 1.5
     */
    test,

    /**
     * Used when running the main source code without container.
     * @since 1.5
     */
    standalone,

    /**
     * Provided by a container when running the main source code.
     * @since 1.5
     */
    provided
}