/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the instrument command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4
 */
public class InstrumentHelp implements CommandHelp {
    public String getSummary() {
        return "Instruments compiled classes ahead of time, as an alternative to the java agent";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Instruments the compiled classes ahead of time with RIFE2's bytecode
            transformations, as an alternative to the java agent: web engine
            continuations, workflow continuations, meta-data merging and
            lazy-loading. This makes the agent unnecessary at run time and
            enables these capabilities inside a GraalVM native image.

            Usage : ${topic}""", "${topic}", topic);
    }
}
