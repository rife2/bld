/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

/**
 * Provides common features across all operations
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.2
 */
public abstract class AbstractOperation<T extends AbstractOperation<T>> {
    private boolean silent_ = false;
    private boolean executed_ = false;

    /**
     * Changes whether the operation should be silent or not.
     * <p>
     * Defaults to not silent.
     *
     * @param silent {@code true} if the operation should be silent;
     *               {@code false} otherwise
     * @return this operation instance
     * @since 1.5.2
     */
    public T silent(boolean silent) {
        silent_ = silent;
        return (T) this;
    }

    /**
     * Indicates whether the operation should be silent or not.
     *
     * @return {@code true} if the operation should be silent;
     * {@code false} otherwise
     * @since 1.5.2
     */
    public boolean silent() {
        return silent_;
    }

    /**
     * Ensures that this operation instance is executed once and only once.
     *
     * @throws Exception when an exception was thrown by the {@link #execute()} call
     * @see #executeOnce(Runnable)
     * @since 1.5.17
     */
    public void executeOnce()
    throws Exception {
        executeOnce(null);
    }

    /**
     * Ensures that this operation instance is executed once and only once.
     * <p>
     * A setup lambda can be provided that is called when the only execution takes place.
     *
     * @param setup the setup lambda that will be called with the only execution
     * @throws Exception when an exception was thrown by the {@link #execute()} call
     * @see #executeOnce()
     * @since 1.5.17
     */
    public void executeOnce(Runnable setup)
    throws Exception {
        if (executed_) {
            return;
        }
        executed_ = true;

        if (setup != null) {
            setup.run();
        }
        execute();
    }

    /**
     * Performs the operation execution that can be wrapped by the {@code #executeOnce} call.
     *
     * @throws Exception when an exception occurs during the execution
     * @since 1.5.10
     */
    public abstract void execute()
    throws Exception;
}
