/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Executes tasks in parallel while preserving the order of their results
 * and the sequential semantics of failures: the first task that fails in
 * order will have its exception rethrown.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4.0
 */
final class ParallelExecution {
    private ParallelExecution() {
    }

    static <T> List<T> execute(List<Supplier<T>> tasks, int parallelism) {
        var result = new ArrayList<T>(tasks.size());

        parallelism = Math.min(tasks.size(), parallelism);
        if (parallelism <= 1) {
            for (var task : tasks) {
                result.add(task.get());
            }
            return result;
        }

        var executor = Executors.newFixedThreadPool(parallelism);
        try {
            var futures = new ArrayList<Future<T>>(tasks.size());
            for (var task : tasks) {
                futures.add(executor.submit(task::get));
            }
            for (var future : futures) {
                try {
                    result.add(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Parallel execution was interrupted", e);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    if (e.getCause() instanceof Error error) {
                        throw error;
                    }
                    throw new IllegalStateException(e.getCause());
                }
            }
        } finally {
            executor.shutdownNow();
        }
        return result;
    }
}
