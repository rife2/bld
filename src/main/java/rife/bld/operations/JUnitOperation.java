/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;

/**
 * Tests a Java application with JUnit.
 * <p>
 * If no JUnit options are specified, the {@link JUnitOptions#defaultOptions()}
 * are used. To tweak the default options, manually add them with this method
 * and use the other desired options.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.20
 */
public class JUnitOperation extends TestOperation<JUnitOperation, JUnitOptions> {
    public static final String DEFAULT_TEST_TOOL_JUNIT5 = "org.junit.platform.console.ConsoleLauncher";

    @Override
    protected JUnitOptions createTestToolOptions() {
        return new JUnitOptions();
    }

    @Override
    public JUnitOperation fromProject(BaseProject project) {
        super.fromProject(project);

        // use the default JUnit 5 console launcher as the test tool
        if (mainClass() == null) {
            mainClass(DEFAULT_TEST_TOOL_JUNIT5);
        }

        // add the default JUnit options if none were specified
        if (testToolOptions().isEmpty() && mainClass().equals(DEFAULT_TEST_TOOL_JUNIT5)) {
            testToolOptions().defaultOptions();
        }

        // evaluate the next arguments and pass them to the JUnit console launcher
        // if they meet the required conditions
        var arguments = project.arguments();
        while (!arguments.isEmpty()) {
            var argument = arguments.get(0);
            if (argument.startsWith("-")) {
                arguments.remove(0);
                if (argument.equals("--junit-help")) {
                    testToolOptions().add("--help");
                } else if (argument.equals("--junit-clear")) {
                    testToolOptions().clear();
                } else {
                    testToolOptions().add(argument);
                    // check whether this option could have the need for an additional argument
                    if (argument.length() == 2 && !arguments.isEmpty()) {
                        switch (argument.charAt(1)) {
                            // these are options in the form of -x where the next argument is separated
                            // by a space and should also be passed on to the JUnit console launcher
                            case 'f', 'd', 'o', 'p', 'c', 'm', 'r', 'i', 'n', 'N', 't', 'T', 'e', 'E' ->
                                    testToolOptions().add(arguments.remove(0));
                        }
                    }
                }
            } else {
                break;
            }
        }

        return this;
    }
}