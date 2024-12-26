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
import static rife.bld.dependencies.Scope.test;

/**
 * Provides the dependency information required to create a new app project.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.9
 */
public class AppProjectBlueprint extends Project {
    public AppProjectBlueprint(File work, String packageName, String projectName, String baseName) {
        this(work, packageName, projectName, baseName, new VersionNumber(0,0,1));
    }

    public AppProjectBlueprint(File work, String packageName, String projectName, String baseName, VersionNumber versionNumber) {
        workDirectory = work;

        pkg = packageName;
        name = projectName;
        mainClass = packageName + "." + baseName;
        version = versionNumber;

        downloadSources = true;
        repositories = List.of(MAVEN_CENTRAL, SONATYPE_SNAPSHOTS);
        scope(test)
            .include(dependency("org.junit.jupiter", "junit-jupiter", version(5,11,4)))
            .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1,11,4)));
    }
}