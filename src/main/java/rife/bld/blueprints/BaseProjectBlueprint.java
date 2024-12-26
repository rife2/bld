/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.blueprints;

import rife.bld.Project;
import rife.bld.dependencies.VersionNumber;
import rife.tools.StringUtils;

import java.io.File;
import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Repository.SONATYPE_SNAPSHOTS;

/**
 * Provides the dependency information required to create a new base project.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.20
 */
public class BaseProjectBlueprint extends Project {
    public BaseProjectBlueprint(File work, String packageName, String projectName, String baseName) {
        this(work, packageName, projectName, baseName, new VersionNumber(0,0,1));
    }

    public BaseProjectBlueprint(File work, String packageName, String projectName, String baseName, VersionNumber versionNumber) {
        workDirectory = work;

        pkg = packageName;
        name = projectName;
        mainClass = packageName + "." + baseName;
        version = versionNumber;

        downloadSources = true;
        repositories = List.of(MAVEN_CENTRAL, SONATYPE_SNAPSHOTS);
    }
}