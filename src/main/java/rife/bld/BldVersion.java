/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.tools.FileUtils;

/**
 * Singleton class that provides access to the current bld version as a string.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.7
 */
public class BldVersion {
    private final String version_;

    BldVersion() {
        version_ = FileUtils.versionFromResource("BLD_VERSION");
    }

    private String getVersionString() {
        return version_;
    }

    public static String getVersion() {
        return BldVersionSingleton.INSTANCE.getVersionString();
    }
}

