/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.Project;
import rife.bld.blueprints.AppProjectBlueprint;

import java.io.File;

/**
 * Creates a new app project structure.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.8
 */
public class CreateAppOperation extends AbstractCreateOperation<CreateAppOperation, Project> {
    public CreateAppOperation() {
        super("bld.app.");
    }

    protected Project createProjectBlueprint() {
        return new AppProjectBlueprint(new File(workDirectory(), projectName()), packageName(), projectName());
    }
}
