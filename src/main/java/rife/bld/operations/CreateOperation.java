/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.operations.exceptions.OperationOptionException;

import java.io.File;
import java.util.List;

/**
 * Creates a new project structure
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.7
 */
public class CreateOperation {
    /**
     * Configures a creation operation from command-line arguments.
     *
     * @param arguments the arguments that will be considered
     * @return this operation instance
     * @since 1.7
     */
    public AbstractCreateOperation<?, ?> fromArguments(List<String> arguments) {
        String type = null;
        String package_name = null;
        String project_name = null;
        if (!arguments.isEmpty()) {
            type = arguments.remove(0);
        }
        if (!arguments.isEmpty()) {
            package_name = arguments.remove(0);
        }
        if (!arguments.isEmpty()) {
            project_name = arguments.remove(0);
        }
        if ((type == null || package_name == null || project_name == null) && System.console() == null) {
            throw new OperationOptionException("ERROR: Expecting the type, package and project names as the arguments.");
        }

        if (type == null || type.isEmpty()) {
            System.out.println("Please enter a number for the project type:");
            System.out.println("  1: base");
            System.out.println("  2: blank");
            System.out.println("  3: lib");
            System.out.println("  4: rife2");
            var number = System.console().readLine();
            switch (Integer.parseInt(number)) {
                case 1 -> type = "base";
                case 2 -> type = "blank";
                case 3 -> type = "lib";
                case 4 -> type = "rife2";
            }
        } else {
            System.out.println("Using project type: " + type);
        }
        if (type == null) {
            throw new OperationOptionException("ERROR: Expecting the project type.");
        }

        AbstractCreateOperation<?, ?> create_operation = null;
        switch (type) {
            case "base" -> create_operation = new CreateBaseOperation();
            case "blank" -> create_operation = new CreateBlankOperation();
            case "lib" -> create_operation = new CreateLibOperation();
            case "rife2" -> create_operation = new CreateRife2Operation();
        }
        if (create_operation == null) {
            throw new OperationOptionException("ERROR: Unsupported project type.");
        }

        if (package_name == null || package_name.isEmpty()) {
            System.out.println("Please enter a package name (for instance: com.example):");
            package_name = System.console().readLine();
        } else {
            System.out.println("Using package name: " + package_name);
        }

        if (project_name == null || project_name.isEmpty()) {
            System.out.println("Please enter a project name (for instance: myapp):");
            project_name = System.console().readLine();
        } else {
            System.out.println("Using project name: " + project_name);
        }

        return create_operation.workDirectory(new File(System.getProperty("user.dir")))
            .packageName(package_name)
            .projectName(project_name)
            .downloadDependencies(true);
    }
}
