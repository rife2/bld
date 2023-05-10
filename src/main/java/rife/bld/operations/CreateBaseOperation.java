/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.Project;
import rife.bld.blueprints.BaseProjectBlueprint;

import java.io.File;

/**
 * Creates a new base project structure.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.20
 */
public class CreateBaseOperation extends AbstractCreateOperation<CreateBaseOperation, Project> {
    public CreateBaseOperation() {
        super("bld.base.");
    }

    protected Project createProjectBlueprint() {
        return new BaseProjectBlueprint(new File(workDirectory(), projectName()), packageName(), projectName());
    }
}
