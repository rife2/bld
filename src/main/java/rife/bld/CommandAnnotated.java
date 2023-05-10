/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class CommandAnnotated implements CommandDefinition {
    private final BuildExecutor executor_;
    private final Method method_;
    private final CommandHelp help_;

    CommandAnnotated(BuildExecutor executor, Method method, CommandHelp help) {
        executor_ = executor;
        method_ = method;
        if (help == null) {
            help = new CommandHelp() {};
        }
        help_ = help;
    }

    public void execute()
    throws Throwable {
        try {
            method_.invoke(executor_);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public CommandHelp getHelp() {
        return help_;
    }
}
