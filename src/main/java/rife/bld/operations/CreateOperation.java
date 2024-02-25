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

    private static final String BASE = "base";
    private static final String BLANK = "blank";
    private static final String LIB = "lib";
    private static final String RIFE2 = "rife2";

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
            System.out.printf("  1: %s%n", BASE);
            System.out.printf("  2: %s%n", BLANK);
            System.out.printf("  3: %s%n", LIB);
            System.out.printf("  4: %s%n", RIFE2);
            var number = System.console().readLine();
            switch (Integer.parseInt(number)) {
                case 1 -> type = BASE;
                case 2 -> type = BLANK;
                case 3 -> type = LIB;
                case 4 -> type = RIFE2;
            }
        } else {
            System.out.println("Using project type: " + type);
        }
        if (type == null) {
            throw new OperationOptionException("ERROR: Expecting the project type.");
        }

        AbstractCreateOperation<?, ?> create_operation = null;
        switch (type) {
            case BASE -> create_operation = new CreateBaseOperation();
            case BLANK -> create_operation = new CreateBlankOperation();
            case LIB -> create_operation = new CreateLibOperation();
            case RIFE2 -> create_operation = new CreateRife2Operation();
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
            String name_example;
            if (LIB.equals(type)) {
                name_example = "mylib";
            } else if (RIFE2.equals(type)) {
                name_example = "mywebapp";
            } else {
                name_example = "myapp";
            }
            System.out.printf("Please enter a project name (for instance: %s):%n", name_example);
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
