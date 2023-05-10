/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the version command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.2
 */
public class VersionHelp implements CommandHelp {
    public String getSummary() {
        return "Outputs the version of the build system";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Outputs the version of the build system.
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}
