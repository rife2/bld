/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the jar-sources command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.10
 */
public class JarSourcesHelp implements CommandHelp {
    public String getSummary() {
        return "Creates a sources jar archive for the project";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Creates a sources jar archive for the project.
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}
