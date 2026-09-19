/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.bld.operations.DevOperation;

/**
 * Provides help for the dev command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 3.1.0
 */
public class DevHelp implements CommandHelp {
    public String getSummary() {
        return "Runs the project and reloads it while developing (takes options)";
    }

    public String getDescription(String topic) {
        return String.format("""
                Runs the project through the reloader of RIFE2 1.11.0 or later
                and recompiles the main sources when they change. The
                application restarts after class changes, and the browser
                reloads by itself after those, as well as after changes to
                templates, resource bundles and static files. When the sources
                don't compile, the application keeps running with its previous
                classes until they do. Press Ctrl-C to stop.

                With %s the application can be debugged on port 5005, or on
                the port that is provided. When a debugger already listens
                there, the application connects to it after every restart,
                otherwise it waits for one to attach.

                The arguments for the application go after %s, quote them
                when there are several, for instance %s"8080 --verbose".

                Usage : %s [%s[=PORT]] [%sARG...]""",
            DevOperation.DEBUG_OPTION, DevOperation.ARGS_OPTION, DevOperation.ARGS_OPTION,
            topic, DevOperation.DEBUG_OPTION, DevOperation.ARGS_OPTION);
    }
}
