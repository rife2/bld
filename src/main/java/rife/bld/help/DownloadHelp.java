/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.bld.operations.DownloadOperation;

/**
 * Provides help for the download command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class DownloadHelp implements CommandHelp {
    public String getSummary() {
        return "Downloads all dependencies of the project (take option)";
    }

    public String getDescription(String topic) {
        return String.format("""
                Downloads all dependencies of the project
                
                Usage : %s [%s]""", topic, DownloadOperation.AUTO_OPTION);
    }
}