/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.tools.Convert;
import rife.tools.FileUtils;
import rife.tools.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Options for the standard javadoc tool.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.12
 */
public class JavadocOptions extends ArrayList<String> {
    public enum Level {
        PUBLIC, PROTECTED, PACKAGE, PRIVATE
    }

    public enum ModuleContent {
        API, ALL
    }

    public enum ModulePackages {
        EXPORTED, ALL
    }

    public enum Override {
        DETAILS, SUMMARY
    }

    public enum DocLinkOption {
        ALL("all"), NONE("none"),
        ACCESSIBILITY("accessibility"), HTML("html"), MISSING("missing"),
        REFERENCE("reference"), SYNTAX("syntax"),
        NO_ACCESSIBILITY("-accessibility"), NO_HTML("-html"), NO_MISSING("-missing"),
        NO_REFERENCE("-reference"), NO_SYNTAX("-syntax");

        private final String option_;

        DocLinkOption(String option) {
            option_ = option;
        }

        private String getOption() {
            return option_;
        }
    }

    /**
     * Root modules to resolve in addition to the initial modules,
     * or all modules on the module path if a module is
     * ALL-MODULE-PATH.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions addModules(String... modules) {
        return addModules(Arrays.asList(modules));
    }

    /**
     * Root modules to resolve in addition to the initial modules,
     * or all modules on the module path if a module is
     * ALL-MODULE-PATH.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavadocOptions addModules(List<String> modules) {
        add("--add-modules");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Compute first sentence with BreakIterator.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions breakIterator() {
        add("-breakiterator");
        return this;
    }

    /**
     * Generate output via alternate doclet
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions doclet(String className) {
        add("-doclet");
        add("className");
        return this;
    }

    /**
     * Specify where to find doclet class files
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions docletPath(String path) {
        add("-docletpath");
        add("path");
        return this;
    }

    /**
     * Enable preview language features. To be used in conjunction with {@link #release}.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions enablePreview() {
        add("--enable-preview");
        return this;
    }

    /**
     * Source file encoding name
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions encoding(String name) {
        add("-encoding");
        add("name");
        return this;
    }

    /**
     * Specify a list of packages to exclude
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions excludePackages(String... name) {
        return excludePackages(Arrays.asList(name));
    }

    /**
     * Specify a list of packages to exclude
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavadocOptions excludePackages(List<String> name) {
        add("-exclude");
        add(StringUtils.join(name, ","));
        return this;
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions extDirs(File... dirs) {
        return extDirs(Arrays.asList(dirs));
    }

    /**
     * Override location of installed extensions
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavadocOptions extDirs(List<File> dirs) {
        add("-extdirs");
        add(dirs.stream().map(File::getAbsolutePath).collect(Collectors.joining(",")));
        return this;
    }

    /**
     * Limit the universe of observable modules
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions limitModules(String... modules) {
        return limitModules(Arrays.asList(modules));
    }

    /**
     * Limit the universe of observable modules
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions limitModules(List<String> modules) {
        add("--limit-modules");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Locale to be used, e.g. en_US or en_US_WIN
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions locale(String name) {
        add("-locale");
        add("name");
        return this;
    }

    /**
     * Document the specified module(s)
     *
     * @return this list of options
     * @since 1.6.3
     */
    public JavadocOptions module(String... modules) {
        return module(Arrays.asList(modules));
    }

    /**
     * Document the specified module(s)
     *
     * @return this list of options
     * @since 1.6.3
     */
    public JavadocOptions module(List<String> modules) {
        add("--module");
        add(StringUtils.join(modules, ","));
        return this;
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 1.6.3
     */
    public JavadocOptions modulePath(File... paths) {
        return modulePath(Arrays.asList(paths));
    }

    /**
     * Specify where to find application modules
     *
     * @return this list of options
     * @since 1.6.3
     */
    public JavadocOptions modulePath(List<File> paths) {
        add("--module-path");
        add(FileUtils.joinPaths(paths.stream().map(File::getAbsolutePath).toList()));
        return this;
    }

    /**
     * Specify where to find input source files for multiple modules
     *
     * @return this list of options
     * @since 1.6.3
     */
    public JavadocOptions moduleSourcePath(File path) {
        add("--module-source-path");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Show package/protected/public types and members.
     * <p>
     * For named modules, show all packages and all module details.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showPackage() {
        add("-package");
        return this;
    }

    /**
     * Show all types and members.
     * <p>
     * For named modules, show all packages and all module details.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showPrivate() {
        add("-private");
        return this;
    }

    /**
     * Show protected/public types and members (default).
     * <p>
     * For named modules, show exported packages and the module's API.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showProtected() {
        add("-protected");
        return this;
    }

    /**
     * Show only public types and members.
     * <p>
     * For named modules, show exported packages and the module's API.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showPublic() {
        add("-public");
        return this;
    }

    /**
     * Do not display status messages
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions quiet() {
        add("-quiet");
        return this;
    }

    /**
     * Indicates whether the Java SE release was set.
     *
     * @return {@code true} if the release was set; or
     * {@code false} otherwise
     * @since 1.5.18
     */
    public boolean containsRelease() {
        return contains("-release");
    }

    /**
     * Provide source compatibility with specified release
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions release(int version) {
        add("--release");
        add(Convert.toString(version));
        return this;
    }

    /**
     * Specifies which members (fields, methods, etc.) will be
     * documented, where value can be one of "public", "protected",
     * "package" or "private". The default is "protected", which will
     * show public and protected members, "public" will show only
     * public members, "package" will show public, protected and
     * package members and "private" will show all members.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showMembers(Level option) {
        add("--show-members");
        add(option.name().toLowerCase());
        return this;
    }

    /**
     * Specifies the documentation granularity of module
     * declarations. Possible values are "api" or "all".
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showModuleContent(ModuleContent option) {
        add("--show-module-contents");
        add(option.name().toLowerCase());
        return this;
    }

    /**
     * Specifies which modules packages will be documented. Possible
     * values are "exported" or "all" packages.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showPackages(ModulePackages option) {
        add("--show-packages");
        add(option.name().toLowerCase());
        return this;
    }

    /**
     * Specifies which types (classes, interfaces, etc.) will be
     * documented, where value can be one of "public", "protected",
     * "package" or "private". The default is "protected", which will
     * show public and protected types, "public" will show only
     * public types, "package" will show public, protected and
     * package types and "private" will show all types.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions showTypes(Level option) {
        add("--show-types");
        add(option.name().toLowerCase());
        return this;
    }

    /**
     * Add a script file to the generated documentation
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions addScript(File file) {
        add("--add-script");
        add(file.getAbsolutePath());
        return this;
    }

    /**
     * Add a stylesheet file to the generated documentation
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions addStylesheet(File file) {
        add("--add-stylesheet");
        add(file.getAbsolutePath());
        return this;
    }

    /**
     * Allow JavaScript in options and comments
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions allowScriptInComments() {
        add("--allow-script-in-comments");
        return this;
    }

    /**
     * Include @author paragraphs
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions author() {
        add("-author");
        return this;
    }

    /**
     * Include bottom text for each page
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions bottom(String html) {
        add("-bottom");
        add(html);
        return this;
    }

    /**
     * Include title for the overview page
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions docTitle(String html) {
        add("-doctitle");
        add(html);
        return this;
    }

    /**
     * Include footer text for each page
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions footer(String html) {
        add("-footer");
        add(html);
        return this;
    }

    /**
     * Include header text for each page
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions header(String html) {
        add("-header");
        add(html);
        return this;
    }

    /**
     * Include HTML meta tags with package, class and member info
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions keywords() {
        add("-keywords");
        return this;
    }

    /**
     * Create links to javadoc output at {@code url}
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions link(String url) {
        add("-link");
        add(url);
        return this;
    }

    /**
     * Link to docs at {@code url1} using package list at {@code url2}
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions linkOffline(String url1, String url2) {
        add("-linkoffline");
        add(url1);
        add(url2);
        return this;
    }

    /**
     * Link to platform documentation URLs declared in properties file at {@code url}
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions linkPlatformProperties(String url) {
        add("--link-platform-properties");
        add(url);
        return this;
    }

    /**
     * Generate source in HTML
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions linkSource() {
        add("-linksource");
        return this;
    }

    /**
     * File to change style of the generated documentation
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions stylesheet(File file) {
        add("--main-stylesheet");
        add(file.getAbsolutePath());
        return this;
    }

    /**
     * Suppress description and tags, generate only declarations
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noComment() {
        add("-nocomment");
        return this;
    }

    /**
     * Do not include @deprecated information
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noDeprecated() {
        add("-nodeprecated");
        return this;
    }

    /**
     * Do not generate deprecated list
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noDeprecatedList() {
        add("-nodeprecatedlist");
        return this;
    }

    /**
     * Do not generate help link
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noHelp() {
        add("-nohelp");
        return this;
    }

    /**
     * Do not generate index
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noIndex() {
        add("-noindex");
        return this;
    }

    /**
     * Do not generate navigation bar
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noNavbar() {
        add("-nonavbar");
        return this;
    }

    /**
     * Do not generate links to the platform documentation
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noPlatformLinks() {
        add("--no-platform-links");
        return this;
    }

    /**
     * Exclude the list of qualifiers from the output
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noQualifier(String... qualifiers) {
        return noQualifier(Arrays.asList(qualifiers));
    }

    /**
     * Exclude the list of qualifiers from the output
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavadocOptions noQualifier(List<String> qualifiers) {
        add("-noqualifier");
        add(StringUtils.join(qualifiers, ":"));
        return this;
    }

    /**
     * Do not include @since information
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noSince() {
        add("-nosince");
        return this;
    }

    /**
     * Do not include hidden time stamp
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noTimestamp() {
        add("-notimestamp");
        return this;
    }

    /**
     * Do not generate class hierarchy
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions noTree() {
        add("-notree");
        return this;
    }

    /**
     * Document overridden methods in the detail or summary sections.
     * The default is 'detail'.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions overrideMethods(Override option) {
        add("--override-methods");
        add(option.name().toLowerCase());
        return this;
    }

    /**
     * Read overview documentation from HTML file
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavadocOptions overview(File htmlFile) {
        add("-overview");
        add(htmlFile.getAbsolutePath());
        return this;
    }

    /**
     * Generate warning about @serial tag
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions serialWarn() {
        add("-serialwarn");
        return this;
    }

    /**
     * Document new and deprecated API in the specified releases
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions since(String... release) {
        return since(Arrays.asList(release));
    }

    /**
     * Document new and deprecated API in the specified releases
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavadocOptions since(List<String> release) {
        add("-since");
        add(StringUtils.join(release, ","));
        return this;
    }

    /**
     * Provide text to use in the heading of the "New API" page
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions sinceLabel(String text) {
        add("--since-label");
        add(text);
        return this;
    }

    /**
     * The path for external snippets
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions snippetPath(File path) {
        add("--snippet-path");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Specify the number of spaces each tab takes up in the source
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions sourceTab(int number) {
        add("-sourcetab");
        add(Convert.toString(number));
        return this;
    }

    /**
     * Split index into one file per letter
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions splitIndex() {
        add("-splitindex");
        return this;
    }

    /**
     * Specify single argument custom tags
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions tag(String name, String locations, String header) {
        add("-tag");
        add(name + ":" + locations + ":" + header);
        return this;
    }

    /**
     * The fully qualified name of Taglet to register
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions taglet(String name) {
        add("-taglet");
        add(name);
        return this;
    }

    /**
     * The path to Taglets
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions tagletPath(File path) {
        add("-tagletpath");
        add(path.getAbsolutePath());
        return this;
    }

    /**
     * Include top text for each page
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions top(String html) {
        add("-top");
        add(html);
        return this;
    }

    /**
     * Create class and package usage pages
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions use() {
        add("-use");
        return this;
    }

    /**
     * Include @version paragraphs
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions version() {
        add("-version");
        return this;
    }

    /**
     * Browser window title for the documentation
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions windowTitle(String text) {
        add("-windowtitle");
        add(text);
        return this;
    }

    /**
     * Enable recommended checks for problems in javadoc comments
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions docLint() {
        add("-Xdoclint");
        return this;
    }

    /**
     * Enable or disable specific checks for problems in javadoc
     * comments.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions docLint(DocLinkOption option) {
        add("-Xdoclint:" + option.getOption());
        return this;
    }

    /**
     * Enable or disable checks in specific packages.
     * <p>
     * A package specifier is either a qualified name of a package or a package
     * name prefix followed by .*, which expands to all sub-packages
     * of the given package. Prefix the package specifier with - to
     * disable checks for the specified packages.
     *
     * @return this list of options
     * @since 1.5.12
     */
    public JavadocOptions docLintPackage(String... packages) {
        return docLintPackage(Arrays.asList(packages));
    }

    /**
     * Enable or disable checks in specific packages.
     * <p>
     * A package specifier is either a qualified name of a package or a package
     * name prefix followed by .*, which expands to all sub-packages
     * of the given package. Prefix the package specifier with - to
     * disable checks for the specified packages.
     *
     * @return this list of options
     * @since 1.5.18
     */
    public JavadocOptions docLintPackage(List<String> packages) {
        add("-Xdoclint/package:" + (StringUtils.join(packages, ",")));
        return this;
    }
}