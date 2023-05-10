/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the war command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class WarHelp implements CommandHelp {
    public String getSummary() {
        return "Creates a war archive for the project";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Creates a war archive for the project.
            The standard war command will automatically also execute
            the jar command beforehand.
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}
