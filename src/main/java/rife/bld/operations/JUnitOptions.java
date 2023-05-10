/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Options for JUnit 5.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.20
 */
public class JUnitOptions extends ArrayList<String> {
    public enum Details {
        NONE, SUMMARY, FLAT, TREE, VERBOSE
    }

    public enum Theme {
        ASCII, UNICODE
    }

    private void removeMutuallyExclusiveOptions(String element) {
        if (element.startsWith("--scan-classpath")) {
            removeIf(s -> s.startsWith("--select-"));
        } else if (element.startsWith("--select-")) {
            removeIf(s -> s.startsWith("--scan-classpath"));
        }
        switch (element) {
            // these are shorthand options for the longer --select-* options
            case "-u", "-f", "-d", "-o", "-p", "-c", "-m", "-r", "-i" ->
                    removeIf(s -> s.startsWith("--scan-classpath"));
        }
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        var result = super.addAll(c);
        if (result) {
            for (var element : c) {
                removeMutuallyExclusiveOptions(element);
            }
        }
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> c) {
        var result = super.addAll(index, c);
        if (result) {
            for (var element : c) {
                removeMutuallyExclusiveOptions(element);
            }
        }
        return result;
    }

    @Override
    public String set(int index, String element) {
        var result = super.set(index, element);
        removeMutuallyExclusiveOptions(element);
        return result;
    }

    @Override
    public boolean add(String s) {
        var result = super.add(s);
        removeMutuallyExclusiveOptions(s);
        return result;
    }

    @Override
    public void add(int index, String element) {
        super.add(index, element);
        removeMutuallyExclusiveOptions(element);
    }

    /**
     * Configures the default options that RIFE2 uses when no
     * options have been explicitly set.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions defaultOptions() {
        config("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$ClassName");
        details(Details.VERBOSE);
        scanClassPath();
        disableBanner();
        disableAnsiColors();
        excludeEngine("junit-platform-suite");
        excludeEngine("junit-vintage");
        failIfNoTests();

        return this;
    }

    /**
     * Scan all directories on the classpath.
     * <p>
     * Only directories on the system classpath as well as additional classpath
     * entries supplied via -cp (directories and JAR files) are scanned.
     * <p>
     * Removes all the {@code select} options since they are mutually exclusive.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions scanClassPath() {
        add("--scan-classpath");
        return this;
    }

    /**
     * Scan an explicit classpath root.
     * <p>
     * Explicit classpath roots that are not on the classpath will be silently
     * ignored.
     * <p>
     * Removes all the {@code select} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions scanClassPath(String path) {
        add("--scan-classpath=" + path);
        return this;
    }

    /**
     * EXPERIMENTAL: Scan all resolved modules for test discovery
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions scanModules() {
        add("--scan-modules");
        return this;
    }

    /**
     * Select a URI for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectUri(String uri) {
        add("--select-uri=" + uri);
        return this;
    }

    /**
     * Select a file for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectFile(File file) {
        add("--select-file=" + file);
        return this;
    }

    /**
     * Select a directory for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectDirectory(File file) {
        add("--select-directory=" + file);
        return this;
    }

    /**
     * EXPERIMENTAL: Select single module for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectModule(String name) {
        add("--select-module=" + name);
        return this;
    }

    /**
     * Select a package for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectPackage(String name) {
        add("--select-package=" + name);
        return this;
    }

    /**
     * Select a class for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectClass(String name) {
        add("--select-class=" + name);
        return this;
    }

    /**
     * Select a method for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectMethod(String name) {
        add("--select-method=" + name);
        return this;
    }

    /**
     * Select a classpath resource for test discovery.
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectResource(String resource) {
        add("--select-resource=" + resource);
        return this;
    }

    /**
     * Select iterations for test discovery of format {@code TYPE:VALUE[INDEX(..INDEX)?(,INDEX(..INDEX)?)*]}
     * (e.g. method:com.acme.Foo#m()[1..2]).
     * <p>
     * Removes all the {@code scanClasspath} options since they are mutually exclusive.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions selectIteration(String iteration) {
        add("--select-iteration=" + iteration);
        return this;
    }

    /**
     * Provide a regular expression to include only classes whose fully
     * qualified names match. To avoid loading classes unnecessarily,
     * the default pattern only includes class names that begin with
     * "Test" or end with "Test" or "Tests". When this option is
     * repeated, all patterns will be combined using OR semantics.
     * Default: ^(Test.*|.+[.$]Test.*|.*Tests?)$
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions includeClassname(String regexPattern) {
        add("--include-classname=" + regexPattern);
        return this;
    }

    /**
     * Provide a regular expression to exclude those classes whose fully
     * qualified names match. When this option is repeated, all
     * patterns will be combined using OR semantics.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions excludeClassname(String regexPattern) {
        add("--exclude-classname=" + regexPattern);
        return this;
    }

    /**
     * Provide a package to be included in the test run. This option can
     * be repeated.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions includePackage(String name) {
        add("--include-package=" + name);
        return this;
    }

    /**
     * Provide a package to be excluded from the test run. This option
     * can be repeated.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions excludePackage(String name) {
        add("--exclude-package=" + name);
        return this;
    }

    /**
     * Provide a tag or tag expression to include only tests whose tags
     * match. When this option is repeated, all patterns will be
     * combined using OR semantics.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions includeTag(String tag) {
        add("--include-tag=" + tag);
        return this;
    }

    /**
     * Provide a tag or tag expression to exclude those tests whose tags
     * match. When this option is repeated, all patterns will be
     * combined using OR semantics.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions excludeTag(String tag) {
        add("--exclude-tag=" + tag);
        return this;
    }

    /**
     * Provide the ID of an engine to be included in the test run.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions includeEngine(String id) {
        add("--include-engine=" + id);
        return this;
    }

    /**
     * Provide the ID of an engine to be excluded in the test run.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions excludeEngine(String id) {
        add("--exclude-engine=" + id);
        return this;
    }

    /**
     * Set a configuration parameter for test discovery and execution.
     * <p>
     * This option can be repeated.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions config(String key, String value) {
        add("--config=" + key + "=" + value);
        return this;
    }

    /**
     * Fail and return exit status code 2 if no tests are found.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions failIfNoTests() {
        add("--fail-if-no-tests");
        return this;
    }

    /**
     * Enable report output into a specified local directory (will be
     * created if it does not exist).
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions reportsDir(File dir) {
        add("--reports-dir=" + dir);
        return this;
    }

    /**
     * Disable ANSI colors in output (not supported by all terminals).
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions disableAnsiColors() {
        add("--disable-ansi-colors");
        return this;
    }

    /**
     * Specify a path to a properties file to customize ANSI style of
     * output (not supported by all terminals).
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions colorPalette(File file) {
        add("--color-palette=" + file);
        return this;
    }

    /**
     * Style test output using only text attributes, no color (not
     * supported by all terminals).
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions singleColor() {
        add("--single-color");
        return this;
    }

    /**
     * Disable print out of the welcome message.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions disableBanner() {
        add("--disable-banner");
        return this;
    }

    /**
     * Select an output details mode for when tests are executed.
     * <p>
     * If 'none' is selected, then only the summary and test failures are shown.
     * <p>
     * Default: {@link Details#TREE}.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions details(Details details) {
        add("--details=" + details.name().toLowerCase());
        return this;
    }

    /**
     * Select an output details tree theme for when tests are executed.
     * <p>
     * Default is detected based on default character encoding.
     *
     * @return this list of options
     * @since 1.5.20
     */
    public JUnitOptions detailsTheme(Theme theme) {
        add("--details-theme=" + theme.name().toLowerCase());
        return this;
    }
}