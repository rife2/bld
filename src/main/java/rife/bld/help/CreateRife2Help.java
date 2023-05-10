/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the create command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class CreateRife2Help implements CommandHelp {
    public String getSummary() {
        return "Creates a new RIFE2 project";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Creates a new RIFE2 project.
                        
            Usage : ${topic} <package> <name>
              package  The package of the project to create
              name     The name of the project to create""", "${topic}", topic);
    }
}
