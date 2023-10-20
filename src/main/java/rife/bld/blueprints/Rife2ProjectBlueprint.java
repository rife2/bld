/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.blueprints;

import rife.bld.WebProject;
import rife.bld.dependencies.VersionNumber;
import rife.bld.operations.TemplateType;
import rife.tools.StringUtils;

import java.io.File;
import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Repository.SONATYPE_SNAPSHOTS;
import static rife.bld.dependencies.Scope.*;

/**
 * Provides the dependency information required to create a new RIFE2 project.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class Rife2ProjectBlueprint extends WebProject {
    public Rife2ProjectBlueprint(File work, String packageName, String projectName) {
        this(work, packageName, projectName, new VersionNumber(0,0,1));
    }

    public Rife2ProjectBlueprint(File work, String packageName, String projectName, VersionNumber versionNumber) {
        workDirectory = work;

        pkg = packageName;
        name = projectName;
        mainClass = packageName + "." + StringUtils.capitalize(projectName) + "Site";
        version = versionNumber;

        downloadSources = true;
        repositories = List.of(MAVEN_CENTRAL, SONATYPE_SNAPSHOTS);
        scope(compile)
            .include(dependency("com.uwyn.rife2", "rife2", version(1,7,2)));
        scope(test)
            .include(dependency("org.jsoup", "jsoup", version(1,16,2)))
            .include(dependency("org.junit.jupiter", "junit-jupiter", version(5,10,0)))
            .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1,10,0)));
        scope(standalone)
            .include(dependency("org.eclipse.jetty", "jetty-server", version(11,0,15)))
            .include(dependency("org.eclipse.jetty", "jetty-servlet", version(11,0,15)))
            .include(dependency("org.slf4j", "slf4j-simple", version(2,0,9)));

        precompileOperation().templateTypes(TemplateType.HTML);
    }
}