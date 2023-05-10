/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.Project;
import rife.bld.blueprints.BaseProjectBlueprint;
import rife.bld.blueprints.LibProjectBlueprint;

import java.io.File;

/**
 * Creates a new lib project structure.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.6
 */
public class CreateLibOperation extends AbstractCreateOperation<CreateLibOperation, Project> {
    public CreateLibOperation() {
        super("bld.lib.");
    }

    protected Project createProjectBlueprint() {
        return new LibProjectBlueprint(new File(workDirectory(), projectName()), packageName(), projectName());
    }

    protected String projectMainClassName(String projectClassName) {
        return projectClassName + "Lib";
    }

    protected boolean createIdeaRunMain() {
        return false;
    }
}
