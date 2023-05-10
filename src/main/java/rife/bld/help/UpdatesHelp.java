/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the updates command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class UpdatesHelp implements CommandHelp {
    public String getSummary() {
        return "Checks for updates of the project dependencies";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Checks which updates are available for the project dependencies.
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}
