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
 * @since 1.7
 */
public class CreateHelp implements CommandHelp {
    public String getSummary() {
        return "Creates a new project from multiple choice";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Creates a new project from multiple choice.
                        
            Usage : ${topic} <type> <package> <name>
              type     The type of project to create (app, base, lib, rife2)
              package  The package of the project to create
              name     The name of the project to create""", "${topic}", topic);
    }
}
