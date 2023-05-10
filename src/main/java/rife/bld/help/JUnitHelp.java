/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the JUnit test command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.20
 */
public class JUnitHelp implements CommandHelp {
    public String getSummary() {
        return "Tests the project with JUnit (takes options)";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Tests the project with JUnit.
            
            Additional JUnit console launcher options can be
            provided after the command.
            
            These commandline options are provided by this command:
            --junit-help   see the full list of JUnit launcher options
            --junit-clear  clear the JUnit launcher options the build uses
                           (needs to be provided before other options)
                        
            Usage : ${topic} [OPTIONS]""", "${topic}", topic);
    }
}
