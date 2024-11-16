/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the create-base command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.20
 */
public class CreateBaseHelp implements CommandHelp {
    public String getSummary() {
        return "Creates a new Java baseline project";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Creates a new Java baseline project.
            
            Usage : ${topic} <package> <name> <base>
              package  The package of the project to create
              name     The name of the project to create
              base     The base name for generated project classes""", "${topic}", topic);
    }
}
