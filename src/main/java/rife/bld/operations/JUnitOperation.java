/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.tools.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
    protected List<String> executeConstructProcessCommandList() {
        if (mainClass() == null) {
            throw new IllegalArgumentException("ERROR: Missing main class for test execution.");
        }

        var args = new ArrayList<String>();
        args.add(javaTool());
        args.addAll(javaOptions());
        args.add("-cp");
        var classpath = FileUtils.joinPaths(classpath());
        args.add(classpath);
        args.add(mainClass());
        // the JUnit console launcher syntax changed in v1.10.x,
        // this logic defaults to the new syntax but if it finds an older
        // JUnit jar in the classpath, uses the old syntax
        var junit_version_1_10_and_later = true;
        var junit_version_pattern = Pattern.compile("junit-platform-console-standalone-(\\d+)\\.(\\d+)\\.");
        var junit_version_matcher = junit_version_pattern.matcher(classpath);
        if (junit_version_matcher.find() &&
            (Integer.parseInt(junit_version_matcher.group(1)) < 1 ||
             (Integer.parseInt(junit_version_matcher.group(1)) == 1 &&
              Integer.parseInt(junit_version_matcher.group(2)) < 10))) {
            junit_version_1_10_and_later = false;
        }
        if (junit_version_1_10_and_later) {
            args.add("execute");
        }
        args.addAll(testToolOptions());

        return args;
    }

    @Override
    public JUnitOperation fromProject(BaseProject project) {
        super.fromProject(project);

        // use the default JUnit 5 console launcher as the test tool
        if (mainClass() == null) {
            mainClass(DEFAULT_TEST_TOOL_JUNIT5);
        }

        // add the default JUnit options if none were specified
        if (testToolOptions().isEmpty() && DEFAULT_TEST_TOOL_JUNIT5.equals(mainClass())) {
            testToolOptions().defaultOptions();
        }

        // evaluate the next arguments and pass them to the JUnit console launcher
        // if they meet the required conditions
        var arguments = project.arguments();
        while (!arguments.isEmpty()) {
            var argument = arguments.get(0);
            if (argument.startsWith("-")) {
                arguments.remove(0);
                if ("--junit-help".equals(argument)) {
                    testToolOptions().add("--help");
                } else if ("--junit-clear".equals(argument)) {
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