/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the upgrade command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class UpgradeHelp implements CommandHelp {
    public String getSummary() {
        return "Upgrades the bld wrapper to the latest version";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Upgrades the bld wrapper to the latest version.
            This command should be executed in the root directory of
            your project.
                        
            Usage : ${topic}""", "${topic}", topic);
    }
}
