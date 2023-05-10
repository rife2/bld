/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.Project;
import rife.bld.blueprints.BlankProjectBlueprint;

import java.io.File;

/**
 * Creates a new blank project structure.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class CreateBlankOperation extends AbstractCreateOperation<CreateBlankOperation, Project> {
    public CreateBlankOperation() {
        super("bld.blank.");
    }

    protected Project createProjectBlueprint() {
        return new BlankProjectBlueprint(new File(workDirectory(), projectName()), packageName(), projectName());
    }
}
