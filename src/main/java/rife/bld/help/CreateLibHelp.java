/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the create-lib command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.6
 */
public class CreateLibHelp implements CommandHelp {
    public String getSummary() {
        return "Creates a new Java library with minimal commands";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Creates a new library Java project with minimal commands.
                        
            Usage : ${topic} <package> <name>
              package  The package of the project to create
              name     The name of the project to create""", "${topic}", topic);
    }
}
