/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the jar-javadoc command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.10
 */
public class JarJavadocHelp implements CommandHelp {
    public String getSummary() {
        return "Creates a javadoc jar archive for the project";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Creates a javadoc jar archive for the project.
            The standard jar-javadoc command will automatically also execute
            the compile and javadoc commands beforehand.
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}
