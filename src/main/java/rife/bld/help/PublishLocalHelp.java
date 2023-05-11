/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the publish-local command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.7
 */
public class PublishLocalHelp implements CommandHelp {
    public String getSummary() {
        return "Publishes to the local maven repository";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Publishes the artifacts of the project to local maven repository,
            regardless of the repositories that are set up in your publish
            operation.
            
            The standard publish-local command will automatically also execute
            the jar, jar-sources and jar-javadoc commands beforehand.
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}