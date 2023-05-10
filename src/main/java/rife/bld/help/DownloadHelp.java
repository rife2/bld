/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the download command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class DownloadHelp implements CommandHelp {
    public String getSummary() {
        return "Downloads all dependencies of the project";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Downloads all dependencies of the project
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}
