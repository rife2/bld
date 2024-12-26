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
    private static final String APP = "app";
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
        String base_name = null;
        if (!arguments.isEmpty()) {
            type = arguments.remove(0);
        }
        if (!arguments.isEmpty()) {
            package_name = arguments.remove(0);
        }
        if (!arguments.isEmpty()) {
            project_name = arguments.remove(0);
        }
        if (!arguments.isEmpty()) {
            base_name = arguments.remove(0);
        }
        if ((package_name == null || project_name == null || base_name == null) && System.console() == null) {
            throw new OperationOptionException("ERROR: Expecting the package, project and base names as the arguments.");
        }

        if (type == null || type.isBlank()) {
            System.out.println("Please enter a number for the project type:");
            System.out.printf("  1: %s   (Java baseline project)%n", BASE);
            System.out.printf("  2: %s    (Java application project)%n", APP);
            System.out.printf("  3: %s    (Java library project)%n", LIB);
            System.out.printf("  4: %s  (RIFE2 web application)%n", RIFE2);
            var number = System.console().readLine();
            switch (Integer.parseInt(number)) {
                case 1 -> type = BASE;
                case 2 -> type = APP;
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
            case APP -> create_operation = new CreateAppOperation();
            case LIB -> create_operation = new CreateLibOperation();
            case RIFE2 -> create_operation = new CreateRife2Operation();
        }
        if (create_operation == null) {
            throw new OperationOptionException("ERROR: Unsupported project type.");
        }

        if (package_name == null || package_name.isBlank()) {
            System.out.println("Please enter a package name (for instance: com.example):");
            package_name = System.console().readLine();
            if (package_name == null || package_name.isEmpty()) {
                throw new OperationOptionException("ERROR: package name is required.");
            }
        } else {
            System.out.println("Using package name: " + package_name);
        }

        if (project_name == null || project_name.isBlank()) {
            String name_example;
            if (LIB.equals(type)) {
                name_example = "my-lib";
            } else if (RIFE2.equals(type)) {
                name_example = "my-webapp";
            } else {
                name_example = "my-app";
            }
            System.out.println("Please enter a project name (for instance: " + name_example + ")");
            project_name = System.console().readLine();
            if (project_name == null || project_name.isEmpty()) {
                throw new OperationOptionException("ERROR: project name is required.");
            }
        } else {
            System.out.println("Using project name: " + project_name);
        }

        if (base_name == null || base_name.isBlank()) {
            var default_base_name = AbstractCreateOperation.generateBaseName(project_name);
            System.out.println("Please enter the base name for generated project classes (default: " + default_base_name + "):");
            base_name = System.console().readLine();
            if (base_name == null || base_name.isBlank()) {
                base_name = default_base_name;
                System.out.println("Using base name: " + base_name);
            }
        } else {
            System.out.println("Using base name: " + base_name);
        }

        return create_operation.workDirectory(new File(System.getProperty("user.dir")))
            .packageName(package_name)
            .projectName(project_name)
            .baseName(base_name)
            .downloadDependencies(true);
    }
}
