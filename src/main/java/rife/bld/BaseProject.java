/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.bld.dependencies.*;
import rife.bld.help.*;
import rife.bld.operations.*;
import rife.tools.FileUtils;
import rife.tools.StringUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

import static rife.bld.dependencies.Scope.runtime;
import static rife.tools.FileUtils.JAR_FILE_PATTERN;

/**
 * Provides the base configuration and commands of a Java project for the
 * build system.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.20
 */
public class BaseProject extends BuildExecutor {
    /**
     * The work directory of the project.
     *
     * @see #workDirectory()
     * @since 1.5
     */
    protected File workDirectory = new File(System.getProperty("user.dir"));
    /**
     * The project's package.
     *
     * @see #pkg()
     * @since 1.5
     */
    protected String pkg = null;
    /**
     * The project's name.
     *
     * @see #name()
     * @since 1.5
     */
    protected String name = null;
    /**
     * The project's version.
     *
     * @see #version()
     * @since 1.5
     */
    protected VersionNumber version = null;
    /**
     * The project's main class.
     *
     * @see #mainClass()
     * @since 1.5
     */
    protected String mainClass = null;

    /**
     * The project's repositories for dependency resolution.
     *
     * @see #repositories()
     * @since 1.5
     */
    protected List<Repository> repositories = new ArrayList<>();
    /**
     * The project's dependencies.
     *
     * @see #dependencies()
     * @since 1.5
     */
    protected DependencyScopes dependencies = new DependencyScopes();

    /**
     * The project's Java release version for compilation.
     *
     * @see #javaRelease()
     * @since 1.5.6
     */
    protected Integer javaRelease = null;
    /**
     * The tool that is used for running the java main class.
     *
     * @see #javaTool()
     * @since 1.5
     */
    protected String javaTool = null;
    /**
     * The base name that is used for creating archives.
     *
     * @see #archiveBaseName()
     * @since 1.5
     */
    protected String archiveBaseName = null;
    /**
     * The filename to use for the main jar archive creation.
     *
     * @see #jarFileName()
     * @since 1.5
     */
    protected String jarFileName = null;
    /**
     * The filename to use for the sources jar archive creation.
     *
     * @see #sourcesJarFileName()
     * @since 1.5.10
     */
    protected String sourcesJarFileName = null;
    /**
     * The filename to use for the javadoc jar archive creation.
     *
     * @see #javadocJarFileName()
     * @since 1.5.10
     */
    protected String javadocJarFileName = null;
    /**
     * The filename to use for the uber jar archive creation.
     *
     * @see #uberJarFileName()
     * @since 1.5
     */
    protected String uberJarFileName = null;
    /**
     * The main class to run the UberJar with.
     *
     * @see #uberJarMainClass()
     * @since 1.5
     */
    protected String uberJarMainClass = null;
    /**
     * Indicates whether dependencies should be automatically downloaded and purged when changes are detected.
     *
     * @see #autoDownloadPurge()
     * @since 1.5.16
     */
    protected Boolean autoDownloadPurge = null;
    /**
     * Indicates whether sources should be downloaded for the dependencies.
     *
     * @see #downloadSources()
     * @since 1.5.6
     */
    protected Boolean downloadSources = null;
    /**
     * Indicates whether javadocs should be downloaded for the dependencies.
     *
     * @see #downloadJavadoc()
     * @since 1.5.6
     */
    protected Boolean downloadJavadoc = null;

    /**
     * The source code directory.
     *
     * @see #srcDirectory()
     * @since 1.5
     */
    protected File srcDirectory = null;
    /**
     * The bld source code directory.
     *
     * @see #srcBldDirectory()
     * @since 1.5
     */
    protected File srcBldDirectory = null;
    /**
     * The bld java source code directory.
     *
     * @see #srcBldJavaDirectory()
     * @since 1.5
     */
    protected File srcBldJavaDirectory = null;
    /**
     * The bld resources source code directory.
     *
     * @see #srcBldResourcesDirectory()
     * @since 1.5.6
     */
    protected File srcBldResourcesDirectory = null;
    /**
     * The main source code directory.
     *
     * @see #srcMainDirectory()
     * @since 1.5
     */
    protected File srcMainDirectory = null;
    /**
     * The main java source code directory.
     *
     * @see #srcMainJavaDirectory()
     * @since 1.5
     */
    protected File srcMainJavaDirectory = null;
    /**
     * The main resources source code directory.
     *
     * @see #srcMainResourcesDirectory()
     * @since 1.5
     */
    protected File srcMainResourcesDirectory = null;
    /**
     * The main template resources source code directory.
     *
     * @see #srcMainResourcesTemplatesDirectory()
     * @since 1.5
     */
    protected File srcMainResourcesTemplatesDirectory = null;
    /**
     * The test source code directory.
     *
     * @see #srcTestDirectory()
     * @since 1.5
     */
    protected File srcTestDirectory = null;
    /**
     * The test java source code directory.
     *
     * @see #srcTestJavaDirectory()
     * @since 1.5
     */
    protected File srcTestJavaDirectory = null;
    /**
     * The test resources source code directory.
     *
     * @see #srcTestResourcesDirectory()
     * @since 1.5.18
     */
    protected File srcTestResourcesDirectory = null;
    /**
     * The lib directory.
     *
     * @see #libDirectory()
     * @since 1.5
     */
    protected File libDirectory = null;
    /**
     * The compile scope lib directory.
     *
     * @see #libCompileDirectory()
     * @since 1.5
     */
    protected File libCompileDirectory = null;
    /**
     * The runtime scope lib directory.
     *
     * @see #libRuntimeDirectory()
     * @since 1.5
     */
    protected File libRuntimeDirectory = null;
    /**
     * The standalone scope lib directory.
     *
     * @see #libStandaloneDirectory()
     * @since 1.5
     */
    protected File libStandaloneDirectory = null;
    /**
     * The standalone scope lib directory.
     *
     * @see #libTestDirectory()
     * @since 1.5
     */
    protected File libTestDirectory = null;
    /**
     * The build directory.
     *
     * @see #buildDirectory()
     * @since 1.5
     */
    protected File buildDirectory = null;
    /**
     * The bld build directory.
     *
     * @see #buildBldDirectory()
     * @since 1.5
     */
    protected File buildBldDirectory = null;
    /**
     * The dist build directory.
     *
     * @see #buildDistDirectory()
     * @since 1.5
     */
    protected File buildDistDirectory = null;
    /**
     * The javadoc build directory.
     *
     * @see #buildJavadocDirectory()
     * @since 1.5.10
     */
    protected File buildJavadocDirectory = null;
    /**
     * The main build directory.
     *
     * @see #buildMainDirectory()
     * @since 1.5
     */
    protected File buildMainDirectory = null;
    /**
     * The templates build directory.
     *
     * @see #buildTemplatesDirectory()
     * @since 1.5
     */
    protected File buildTemplatesDirectory = null;
    /**
     * The test build directory.
     *
     * @see #buildTestDirectory()
     * @since 1.5
     */
    protected File buildTestDirectory = null;

    /*
     * Standard build commands
     */

    private final ArtifactRetriever retriever_ = ArtifactRetriever.cachingInstance();

    /**
     * Returns the artifact retriever that is used.
     *
     * @return the artifact retriever
     * @since 1.5.21
     */
    public ArtifactRetriever artifactRetriever() {
        return retriever_;
    }

    private final CleanOperation cleanOperation_ = new CleanOperation();
    private final CompileOperation compileOperation_ = new CompileOperation();
    private final DependencyTreeOperation dependencyTreeOperation_ = new DependencyTreeOperation();
    private final DownloadOperation downloadOperation_ = new DownloadOperation();
    private final PurgeOperation purgeOperation_ = new PurgeOperation();
    private final PublishOperation publishOperation_ = new PublishOperation();
    private final RunOperation runOperation_ = new RunOperation();
    private final TestOperation<?, ?> testOperation_ = new TestOperation<>();
    private final UpdatesOperation updatesOperation_ = new UpdatesOperation();
    private final VersionOperation versionOperation_ = new VersionOperation();

    /**
     * Retrieves the project's default clean operation.
     *
     * @return the default clean operation instance
     * @since 1.5.18
     */
    public CleanOperation cleanOperation() {
        return cleanOperation_;
    }

    /**
     * Retrieves the project's default compile operation.
     *
     * @return the default compile operation instance
     * @since 1.5.18
     */
    public CompileOperation compileOperation() {
        return compileOperation_;
    }

    /**
     * Retrieves the project's default dependency tree operation.
     *
     * @return the default dependency tree operation instance
     * @since 1.5.21
     */
    public DependencyTreeOperation dependencyTreeOperation() {
        return dependencyTreeOperation_;
    }

    /**
     * Retrieves the project's default download operation.
     *
     * @return the default download operation instance
     * @since 1.5.18
     */
    public DownloadOperation downloadOperation() {
        return downloadOperation_;
    }

    /**
     * Retrieves the project's default publish operation.
     *
     * @return the default publish operation instance
     * @since 1.5.18
     */
    public PublishOperation publishOperation() {
        return publishOperation_;
    }

    /**
     * Retrieves the project's default purge operation.
     *
     * @return the default purge operation instance
     * @since 1.5.18
     */
    public PurgeOperation purgeOperation() {
        return purgeOperation_;
    }

    /**
     * Retrieves the project's default run operation.
     *
     * @return the default run operation instance
     * @since 1.5.18
     */
    public RunOperation runOperation() {
        return runOperation_;
    }

    /**
     * Retrieves the project's default test operation.
     *
     * @return the default test operation instance
     * @since 1.5.18
     */
    public TestOperation<?, ?> testOperation() {
        return testOperation_;
    }

    /**
     * Retrieves the project's default updates operation.
     *
     * @return the default updates operation instance
     * @since 1.5.18
     */
    public UpdatesOperation updatesOperation() {
        return updatesOperation_;
    }

    /**
     * Retrieves the project's default version operation.
     *
     * @return the default version operation instance
     * @since 1.5.18
     */
    public VersionOperation versionOperation() {
        return versionOperation_;
    }

    /**
     * Standard build command, cleans the build files.
     *
     * @since 1.5
     */
    @BuildCommand(help = CleanHelp.class)
    public void clean()
    throws Exception {
        cleanOperation().executeOnce(() -> cleanOperation().fromProject(this));
    }

    /**
     * Standard build command, compiles the project.
     *
     * @since 1.5
     */
    @BuildCommand(help = CompileHelp.class)
    public void compile()
    throws Exception {
        compileOperation().executeOnce(() -> compileOperation().fromProject(this));
    }

    /**
     * Standard build command, output the dependency tree.
     *
     * @since 1.5.21
     */
    @BuildCommand(value = "dependency-tree", help = DependencyTreeHelp.class)
    public void dependencyTree()
    throws Exception {
        dependencyTreeOperation().executeOnce(() -> dependencyTreeOperation().fromProject(this));
    }

    /**
     * Standard build command, downloads all dependencies of the project.
     *
     * @since 1.5
     */
    @BuildCommand(help = DownloadHelp.class)
    public void download()
    throws Exception {
        downloadOperation().executeOnce(() -> downloadOperation().fromProject(this));
    }

    /**
     * Standard build command, purges all unused artifacts from the project.
     *
     * @since 1.5
     */
    @BuildCommand(help = PurgeHelp.class)
    public void purge()
    throws Exception {
        purgeOperation().executeOnce(() -> purgeOperation().fromProject(this));
    }

    /**
     * Standard build command, runs the project.
     *
     * @since 1.5
     */
    @BuildCommand(help = RunHelp.class)
    public void run()
    throws Exception {
        runOperation().executeOnce(() -> runOperation().fromProject(this));
    }

    /**
     * Standard build command, tests the project.
     *
     * @since 1.5
     */
    @BuildCommand(help = TestHelp.class)
    public void test()
    throws Exception {
        testOperation().executeOnce(() -> testOperation().fromProject(this));
    }

    /**
     * Standard build command, outputs the version of the build system.
     *
     * @since 1.5.2
     */
    @BuildCommand(value = "version", help = VersionHelp.class)
    public void printVersion()
    throws Exception {
        versionOperation().executeOnce();
    }

    /**
     * Standard build command, checks for updates of the project dependencies.
     *
     * @since 1.5
     */
    @BuildCommand(help = UpdatesHelp.class)
    public void updates()
    throws Exception {
        updatesOperation().executeOnce(() -> updatesOperation().fromProject(this));
    }

    /*
     * Useful methods
     */

    private Dependency rife2Agent_ = null;

    /**
     * Includes the RIFE2 instrumentation agent as a runtime dependency,
     * and activates it for run and test commands.
     *
     * @param version the version of the instrumentation agent to use
     * @since 1.5.5
     */
    public void useRife2Agent(VersionNumber version) {
        rife2Agent_ = new Dependency("com.uwyn.rife2", "rife2", version, "agent");
        scope(runtime).include(rife2Agent_);
    }

    /**
     * Indicates whether the RIFE2 instrumentation agent should be used.
     *
     * @return {@code true} if the RIFE2 instrumentation agent should be used; or
     * {@code false} otherwise
     * @since 1.5.18
     */
    public boolean usesRife2Agent() {
        return rife2Agent_ != null;
    }

    /**
     * Returns the jar file of the RIFE2 instrumentation agent that should be used.
     *
     * @return the jar file of the RIFE2 instrumentation agent; or
     * {@code null} if no agent should be used
     * @since 1.5.18
     */
    public File getRife2AgentFile() {
        if (rife2Agent_ == null) {
            return null;
        }
        return new File(libRuntimeDirectory(), rife2Agent_.toFileName());
    }

    /**
     * Creates a new repository instance.
     * <p>
     * Instead of providing the repository location, it's also possible to provide a name
     * that will be used to look up the repository credentials in the hierarchical
     * properties.
     * <p>
     * For instance, using the name {@code myrepo} will look for the following properties:<br>
     * {@code bld.repo.myrepo}<br>
     * {@code bld.repo.myrepo.username} (optional)<br>
     * {@code bld.repo.myrepo.password} (optional)
     *
     * @param locationOrName the repository location or name
     * @return a newly created {@code Repository} instance
     * @since 1.5.6
     */
    public Repository repository(String locationOrName) {
        return Repository.resolveRepository(properties(), locationOrName);
    }

    /**
     * Creates a new repository instance with basic username and password authentication.
     *
     * @param location the repository location
     * @param username the repository username
     * @param password the repository password
     * @return a newly created {@code Repository} instance
     * @since 1.5.7
     */
    public Repository repository(String location, String username, String password) {
        return new Repository(location, username, password);
    }

    /**
     * Creates a new version instance.
     *
     * @param major the major component of the version number
     * @return a newly created {@code VersionNumber} instance
     * @since 1.5
     */
    public VersionNumber version(int major) {
        return new VersionNumber(major);
    }

    /**
     * Creates a new version instance.
     *
     * @param major the major component of the version number
     * @param minor the minor component of the version number
     * @return a newly created {@code VersionNumber} instance
     * @since 1.5
     */
    public VersionNumber version(int major, int minor) {
        return new VersionNumber(major, minor);
    }

    /**
     * Creates a new version instance.
     *
     * @param major    the major component of the version number
     * @param minor    the minor component of the version number
     * @param revision the revision component of the version number
     * @return a newly created {@code VersionNumber} instance
     * @since 1.5
     */
    public VersionNumber version(int major, int minor, int revision) {
        return new VersionNumber(major, minor, revision);
    }

    /**
     * Creates a new version instance.
     *
     * @param major     the major component of the version number
     * @param minor     the minor component of the version number
     * @param revision  the revision component of the version number
     * @param qualifier the qualifier component of the version number
     * @return a newly created {@code VersionNumber} instance
     * @since 1.5
     */
    public VersionNumber version(int major, int minor, int revision, String qualifier) {
        return new VersionNumber(major, minor, revision, qualifier);
    }

    /**
     * Creates a new version instance.
     *
     * @param description the textual description of the version number
     * @return a newly created {@code VersionNumber} instance; or
     * {@link VersionNumber#UNKNOWN} if the description couldn't be parsed
     * @since 1.5
     */
    public VersionNumber version(String description) {
        return VersionNumber.parse(description);
    }

    /**
     * Retrieves the dependency set for a particular scope, initializing it
     * if it doesn't exist yet.
     *
     * @param scope the scope to retrieve dependencies for
     * @return the scope's dependency set
     * @since 1.5
     */
    public DependencySet scope(Scope scope) {
        return dependencies().scope(scope);
    }

    /**
     * Creates a new dependency instance.
     *
     * @param groupId    the dependency group identifier
     * @param artifactId the dependency artifact identifier
     * @return a newly created {@code Dependency} instance
     * @since 1.5
     */
    public Dependency dependency(String groupId, String artifactId) {
        return new Dependency(groupId, artifactId);
    }

    /**
     * Creates a new dependency instance.
     *
     * @param groupId    the dependency group identifier
     * @param artifactId the dependency artifact identifier
     * @param version    the dependency version
     * @return a newly created {@code Dependency} instance
     * @since 1.5.16
     */
    public Dependency dependency(String groupId, String artifactId, String version) {
        return new Dependency(groupId, artifactId, version(version));
    }

    /**
     * Creates a new dependency instance.
     *
     * @param groupId    the dependency group identifier
     * @param artifactId the dependency artifact identifier
     * @param version    the dependency version
     * @param classifier the dependency classifier
     * @return a newly created {@code Dependency} instance
     * @since 1.5.16
     */
    public Dependency dependency(String groupId, String artifactId, String version, String classifier) {
        return new Dependency(groupId, artifactId, version(version), classifier);
    }

    /**
     * Creates a new dependency instance.
     *
     * @param groupId    the dependency group identifier
     * @param artifactId the dependency artifact identifier
     * @param version    the dependency version
     * @param classifier the dependency classifier
     * @param type       the dependency type
     * @return a newly created {@code Dependency} instance
     * @since 1.5.16
     */
    public Dependency dependency(String groupId, String artifactId, String version, String classifier, String type) {
        return new Dependency(groupId, artifactId, version(version), classifier, type);
    }

    /**
     * Creates a new dependency instance.
     *
     * @param groupId    the dependency group identifier
     * @param artifactId the dependency artifact identifier
     * @param version    the dependency version
     * @return a newly created {@code Dependency} instance
     * @since 1.5
     */
    public Dependency dependency(String groupId, String artifactId, VersionNumber version) {
        return new Dependency(groupId, artifactId, version);
    }

    /**
     * Creates a new dependency instance.
     *
     * @param groupId    the dependency group identifier
     * @param artifactId the dependency artifact identifier
     * @param version    the dependency version
     * @param classifier the dependency classifier
     * @return a newly created {@code Dependency} instance
     * @since 1.5
     */
    public Dependency dependency(String groupId, String artifactId, VersionNumber version, String classifier) {
        return new Dependency(groupId, artifactId, version, classifier);
    }

    /**
     * Creates a new dependency instance.
     *
     * @param groupId    the dependency group identifier
     * @param artifactId the dependency artifact identifier
     * @param version    the dependency version
     * @param classifier the dependency classifier
     * @param type       the dependency type
     * @return a newly created {@code Dependency} instance
     * @since 1.5
     */
    public Dependency dependency(String groupId, String artifactId, VersionNumber version, String classifier, String type) {
        return new Dependency(groupId, artifactId, version, classifier, type);
    }

    /**
     * Creates a new dependency instance from a string representation.
     * The format is {@code groupId:artifactId:version:classifier@type}.
     * The {@code version}, {@code classifier} and {@code type} are optional.
     * <p>
     * If the string can't be successfully parsed, {@code null} will be returned.
     *
     * @param description the dependency string to parse
     * @return a parsed instance of {@code Dependency}; or
     * {@code null} when the string couldn't be parsed
     * @since 1.5.2
     */
    public Dependency dependency(String description) {
        return Dependency.parse(description);
    }

    /**
     * Creates a local dependency instance.
     * <p>
     * If the local dependency points to a directory, it will be scanned for jar files.
     *
     * @param path the file system path of the local dependency
     * @since 1.5.2
     */

    public LocalDependency local(String path) {
        return new LocalDependency(path);
    }

    /*
     * Project directories
     */

    /**
     * {@inheritDoc}
     *
     * @since 1.5
     */
    public File workDirectory() {
        return Objects.requireNonNullElseGet(workDirectory, super::workDirectory);
    }

    /**
     * Returns the project source code directory.
     * Defaults to {@code "src"} relative to {@link #workDirectory()}.
     *
     * @since 1.5
     */
    public File srcDirectory() {
        return Objects.requireNonNullElseGet(srcDirectory, () -> new File(workDirectory(), "src"));
    }

    /**
     * Returns the project bld source code directory.
     * Defaults to {@code "bld"} relative to {@link #srcDirectory()}.
     *
     * @since 1.5
     */
    public File srcBldDirectory() {
        return Objects.requireNonNullElseGet(srcBldDirectory, () -> new File(srcDirectory(), "bld"));
    }

    /**
     * Returns the project bld java source code directory.
     * Defaults to {@code "java"} relative to {@link #srcBldDirectory()}.
     *
     * @since 1.5
     */
    public File srcBldJavaDirectory() {
        return Objects.requireNonNullElseGet(srcBldJavaDirectory, () -> new File(srcBldDirectory(), "java"));
    }

    /**
     * Returns the project bld resources source code directory.
     * Defaults to {@code "resources"} relative to {@link #srcMainDirectory()}.
     *
     * @since 1.5.5
     */
    public File srcBldResourcesDirectory() {
        return Objects.requireNonNullElseGet(srcBldResourcesDirectory, () -> new File(srcBldDirectory(), "resources"));
    }

    /**
     * Returns the project main source code directory.
     * Defaults to {@code "main"} relative to {@link #srcDirectory()}.
     *
     * @since 1.5
     */
    public File srcMainDirectory() {
        return Objects.requireNonNullElseGet(srcMainDirectory, () -> new File(srcDirectory(), "main"));
    }

    /**
     * Returns the project main java source code directory.
     * Defaults to {@code "java"} relative to {@link #srcMainDirectory()}.
     *
     * @since 1.5
     */
    public File srcMainJavaDirectory() {
        return Objects.requireNonNullElseGet(srcMainJavaDirectory, () -> new File(srcMainDirectory(), "java"));
    }

    /**
     * Returns the project main resources source code directory.
     * Defaults to {@code "resources"} relative to {@link #srcMainDirectory()}.
     *
     * @since 1.5
     */
    public File srcMainResourcesDirectory() {
        return Objects.requireNonNullElseGet(srcMainResourcesDirectory, () -> new File(srcMainDirectory(), "resources"));
    }

    /**
     * Returns the project main template resources source code directory.
     * Defaults to {@code "templates"} relative to {@link #srcMainResourcesDirectory()}.
     *
     * @since 1.5
     */
    public File srcMainResourcesTemplatesDirectory() {
        return Objects.requireNonNullElseGet(srcMainResourcesTemplatesDirectory, () -> new File(srcMainResourcesDirectory(), "templates"));
    }

    /**
     * Returns the project test source code directory.
     * Defaults to {@code "test"} relative to {@link #srcDirectory()}.
     *
     * @since 1.5
     */
    public File srcTestDirectory() {
        return Objects.requireNonNullElseGet(srcTestDirectory, () -> new File(srcDirectory(), "test"));
    }

    /**
     * Returns the project test resources source code directory.
     * Defaults to {@code "resources"} relative to {@link #srcTestDirectory()}.
     *
     * @since 1.5.18
     */
    public File srcTestResourcesDirectory() {
        return Objects.requireNonNullElseGet(srcTestResourcesDirectory, () -> new File(srcTestDirectory(), "resources"));
    }

    /**
     * Returns the project test java source code directory.
     * Defaults to {@code "java"} relative to {@link #srcTestDirectory()}.
     *
     * @since 1.5
     */
    public File srcTestJavaDirectory() {
        return Objects.requireNonNullElseGet(srcTestJavaDirectory, () -> new File(srcTestDirectory(), "java"));
    }

    /**
     * Returns the project lib directory.
     * Defaults to {@code "lib"} relative to {@link #workDirectory()}.
     *
     * @since 1.5
     */
    public File libDirectory() {
        return Objects.requireNonNullElseGet(libDirectory, () -> new File(workDirectory(), "lib"));
    }

    /**
     * Returns the {@code lib/bld} directory relative to {@link #workDirectory()}.
     *
     * @since 1.5
     */
    public final File libBldDirectory() {
        return new File(new File(workDirectory(), "lib"), "bld");
    }

    /**
     * Returns the project compile scope lib directory.
     * Defaults to {@code "compile"} relative to {@link #libDirectory()}.
     *
     * @since 1.5
     */
    public File libCompileDirectory() {
        return Objects.requireNonNullElseGet(libCompileDirectory, () -> new File(libDirectory(), "compile"));
    }

    /**
     * Returns the project runtime scope lib directory.
     * Defaults to {@code "runtime"} relative to {@link #libDirectory()}.
     *
     * @since 1.5
     */
    public File libRuntimeDirectory() {
        return Objects.requireNonNullElseGet(libRuntimeDirectory, () -> new File(libDirectory(), "runtime"));
    }

    /**
     * Returns the project standalone scope lib directory.
     * Defaults to {@code null}.
     *
     * @since 1.5
     */
    public File libStandaloneDirectory() {
        return null;
    }

    /**
     * Returns the project test scope lib directory.
     * Defaults to {@code "test"} relative to {@link #libDirectory()}.
     *
     * @since 1.5
     */
    public File libTestDirectory() {
        return Objects.requireNonNullElseGet(libTestDirectory, () -> new File(libDirectory(), "test"));
    }

    /**
     * Returns the project build directory.
     * Defaults to {@code "build"} relative to {@link #workDirectory()}.
     *
     * @since 1.5
     */
    public File buildDirectory() {
        return Objects.requireNonNullElseGet(buildDirectory, () -> new File(workDirectory(), "build"));
    }

    /**
     * Returns the project bld build directory.
     * Defaults to {@code "bld"} relative to {@link #buildDirectory()}.
     *
     * @since 1.5
     */
    public File buildBldDirectory() {
        return Objects.requireNonNullElseGet(buildBldDirectory, () -> new File(buildDirectory(), "bld"));
    }

    /**
     * Returns the project dist build directory.
     * Defaults to {@code "dist"} relative to {@link #buildDirectory()}.
     *
     * @since 1.5
     */
    public File buildDistDirectory() {
        return Objects.requireNonNullElseGet(buildDistDirectory, () -> new File(buildDirectory(), "dist"));
    }

    /**
     * Returns the project javadoc build directory.
     * Defaults to {@code "javadoc"} relative to {@link #buildDirectory()}.
     *
     * @since 1.5.10
     */
    public File buildJavadocDirectory() {
        return Objects.requireNonNullElseGet(buildJavadocDirectory, () -> new File(buildDirectory(), "javadoc"));
    }

    /**
     * Returns the project main build directory.
     * Defaults to {@code "main"} relative to {@link #buildDirectory()}.
     *
     * @since 1.5
     */
    public File buildMainDirectory() {
        return Objects.requireNonNullElseGet(buildMainDirectory, () -> new File(buildDirectory(), "main"));
    }

    /**
     * Returns the project templates build directory.
     * Defaults to {@link #buildMainDirectory()}.
     *
     * @since 1.5
     */
    public File buildTemplatesDirectory() {
        return Objects.requireNonNullElseGet(buildTemplatesDirectory, this::buildMainDirectory);
    }

    /**
     * Returns the project test build directory.
     * Defaults to {@code "test"} relative to {@link #buildDirectory()}.
     *
     * @since 1.5
     */
    public File buildTestDirectory() {
        return Objects.requireNonNullElseGet(buildTestDirectory, () -> new File(buildDirectory(), "test"));
    }

    /**
     * Creates the project structure based on the directories that are specified in the other methods.
     *
     * @since 1.5
     */
    public void createProjectStructure() {
        srcDirectory().mkdirs();
        srcBldDirectory().mkdirs();
        srcBldJavaDirectory().mkdirs();
        srcBldResourcesDirectory().mkdirs();
        srcMainDirectory().mkdirs();
        srcMainJavaDirectory().mkdirs();
        srcMainResourcesDirectory().mkdirs();
        srcMainResourcesTemplatesDirectory().mkdirs();
        srcTestDirectory().mkdirs();
        srcTestJavaDirectory().mkdirs();
        srcTestResourcesDirectory().mkdirs();
        libDirectory().mkdirs();
        libBldDirectory().mkdirs();
        libCompileDirectory().mkdirs();
        libRuntimeDirectory().mkdirs();
        if (libStandaloneDirectory() != null) {
            libStandaloneDirectory().mkdirs();
        }
        libTestDirectory().mkdirs();
    }

    /**
     * Creates the project build structure based on the directories that are specified in the other methods.
     *
     * @since 1.5
     */
    public void createBuildStructure() {
        buildDirectory().mkdirs();
        buildBldDirectory().mkdirs();
        buildDistDirectory().mkdirs();
        buildJavadocDirectory().mkdirs();
        buildMainDirectory().mkdirs();
        buildTemplatesDirectory().mkdirs();
        buildTestDirectory().mkdirs();
    }

    /*
     * Project options
     */

    /**
     * Returns the project's package.
     *
     * @since 1.5
     */
    public String pkg() {
        if (pkg == null) {
            throw new IllegalStateException("The pkg variable has to be set.");
        }
        return pkg;
    }

    /**
     * Returns the project's name.
     *
     * @since 1.5
     */
    public String name() {
        if (name == null) {
            throw new IllegalStateException("The name variable has to be set.");
        }
        return name;
    }

    /**
     * Returns the project's version.
     *
     * @since 1.5
     */
    public VersionNumber version() {
        if (version == null) {
            throw new IllegalStateException("The version variable has to be set.");
        }
        return version;
    }

    /**
     * Returns the project's main class.
     *
     * @since 1.5
     */
    public String mainClass() {
        if (mainClass == null) {
            throw new IllegalStateException("The mainClass variable has to be set.");
        }
        return mainClass;
    }

    /**
     * Returns the list of repositories for this project.
     * <p>
     * This list can be modified to change the repositories that the project uses.
     *
     * @since 1.5
     */
    public List<Repository> repositories() {
        if (repositories == null) {
            repositories = new ArrayList<>();
        }
        return repositories;
    }

    /**
     * Adds repositories to this project.
     *
     * @param repositories the repositories to add
     * @since 1.5.6
     */
    public void repositories(Repository... repositories) {
        for (var repository : repositories) {
            repositories().add(repository);
        }
    }

    /**
     * Returns the project's dependencies.
     * <p>
     * This collection can be modified to change the dependencies that the project uses.
     *
     * @since 1.5
     */
    public DependencyScopes dependencies() {
        if (dependencies == null) {
            dependencies = new DependencyScopes();
        }
        return dependencies;
    }

    /**
     * Returns the java release targets for this project.
     *
     * @since 1.5.18
     */
    public Integer javaRelease() {
        return javaRelease;
    }

    /**
     * Returns the tool that is used for running the java main class.
     *
     * @since 1.5
     */
    public String javaTool() {
        return Objects.requireNonNullElse(javaTool, "java");
    }

    /**
     * Returns the base name that is used for creating archives.
     * By default, this returns the lower-cased project name.
     *
     * @since 1.5
     */
    public String archiveBaseName() {
        return Objects.requireNonNullElseGet(archiveBaseName, () -> name().toLowerCase(Locale.ENGLISH));
    }

    /**
     * Returns the filename to use for the main jar archive creation.
     * By default, appends the version and the {@code jar} extension to the {@link #archiveBaseName()}.
     *
     * @since 1.5
     */
    public String jarFileName() {
        return Objects.requireNonNullElseGet(jarFileName, () -> archiveBaseName() + "-" + version() + ".jar");
    }

    /**
     * Returns the filename to use for the sources jar archive creation.
     * By default, appends the version, {@code "sources"} and the {@code jar} extension to the {@link #archiveBaseName()}.
     *
     * @since 1.5.10
     */
    public String sourcesJarFileName() {
        return Objects.requireNonNullElseGet(sourcesJarFileName, () -> archiveBaseName() + "-" + version() + "-sources" + ".jar");
    }

    /**
     * Returns the filename to use for the javadoc jar archive creation.
     * By default, appends the version, {@code "javadoc"} and the {@code jar} extension to the {@link #archiveBaseName()}.
     *
     * @since 1.5.10
     */
    public String javadocJarFileName() {
        return Objects.requireNonNullElseGet(javadocJarFileName, () -> archiveBaseName() + "-" + version() + "-javadoc" + ".jar");
    }

    /**
     * Returns the filename to use for the uber jar archive creation.
     * By default, appends the version, the {@code "-uber"} suffix and the {@code jar} extension to the {@link #archiveBaseName()}.
     *
     * @since 1.5
     */
    public String uberJarFileName() {
        return Objects.requireNonNullElseGet(uberJarFileName, () -> archiveBaseName() + "-" + version() + "-uber.jar");
    }

    /**
     * Returns main class to run the UberJar with.
     * By default, returns the same as {@link #mainClass()}.
     *
     * @since 1.5
     */
    public String uberJarMainClass() {
        return Objects.requireNonNullElseGet(uberJarMainClass, this::mainClass);
    }

    /**
     * Indicates whether dependencies should be automatically downloaded and purged when changes are detected.
     * By default, returns {@code false}.
     *
     * @since 1.5.6
     */
    public boolean autoDownloadPurge() {
        return Objects.requireNonNullElse(autoDownloadPurge, Boolean.FALSE);
    }

    /**
     * Returns whether sources should be downloaded for the dependencies.
     * By default, returns {@code false}.
     *
     * @since 1.5.6
     */
    public boolean downloadSources() {
        return Objects.requireNonNullElse(downloadSources, Boolean.FALSE);
    }

    /**
     * Returns whether javadocs should be downloaded for the dependencies.
     * By default, returns {@code false}.
     *
     * @since 1.5.6
     */
    public boolean downloadJavadoc() {
        return Objects.requireNonNullElse(downloadJavadoc, Boolean.FALSE);
    }

    /*
     * File collections
     */

    /**
     * Returns all a list with all the main Java source files.
     *
     * @since 1.5
     */
    public List<File> mainSourceFiles() {
        return FileUtils.getJavaFileList(srcMainJavaDirectory());
    }

    /**
     * Returns all a list with all the test Java source files.
     *
     * @since 1.5
     */
    public List<File> testSourceFiles() {
        return FileUtils.getJavaFileList(srcTestJavaDirectory());
    }

    /*
     * Project classpaths
     */

    private static final Pattern JAR_EXCLUDE_SOURCES_PATTERN = Pattern.compile("^.*-sources\\.jar$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAR_EXCLUDE_JAVADOC_PATTERN = Pattern.compile("^.*-javadoc\\.jar$", Pattern.CASE_INSENSITIVE);
    private static final Pattern[] INCLUDED_JARS = new Pattern[]{JAR_FILE_PATTERN};
    private static final Pattern[] EXCLUDED_JARS = new Pattern[]{JAR_EXCLUDE_SOURCES_PATTERN, JAR_EXCLUDE_JAVADOC_PATTERN};

    /**
     * Returns all the jar files that are in the compile scope classpath.
     * <p>
     * By default, this collects all the jar files in the {@link #libCompileDirectory()}
     * and adds all the jar files from the compile scope local dependencies.
     *
     * @since 1.5
     */
    public List<File> compileClasspathJars() {
        // detect the jar files in the compile lib directory
        var dir_abs = libCompileDirectory().getAbsoluteFile();
        var jar_files = FileUtils.getFileList(dir_abs, INCLUDED_JARS, EXCLUDED_JARS);

        // build the compilation classpath
        var classpath = new ArrayList<>(jar_files.stream().map(file -> new File(dir_abs, file)).toList());
        addLocalDependencies(classpath, Scope.compile);
        addLocalDependencies(classpath, Scope.provided);
        return classpath;
    }

    /**
     * Returns all the jar files that are in the runtime scope classpath.
     * <p>
     * By default, this collects all the jar files in the {@link #libRuntimeDirectory()}
     * and adds all the jar files from the runtime scope local dependencies.
     *
     * @since 1.5
     */
    public List<File> runtimeClasspathJars() {
        // detect the jar files in the runtime lib directory
        var dir_abs = libRuntimeDirectory().getAbsoluteFile();
        var jar_files = FileUtils.getFileList(dir_abs, INCLUDED_JARS, EXCLUDED_JARS);

        // build the runtime classpath
        var classpath = new ArrayList<>(jar_files.stream().map(file -> new File(dir_abs, file)).toList());
        addLocalDependencies(classpath, Scope.runtime);
        return classpath;
    }

    /**
     * Returns all the jar files that are in the standalone scope classpath.
     * <p>
     * By default, this collects all the jar files in the {@link #libStandaloneDirectory()}
     * and adds all the jar files from the standalone scope local dependencies.
     *
     * @since 1.5
     */
    public List<File> standaloneClasspathJars() {
        // build the standalone classpath
        List<File> classpath;
        if (libStandaloneDirectory() == null) {
            classpath = new ArrayList<>();
        } else {
            // detect the jar files in the standalone lib directory
            var dir_abs = libStandaloneDirectory().getAbsoluteFile();
            var jar_files = FileUtils.getFileList(dir_abs, INCLUDED_JARS, EXCLUDED_JARS);

            classpath = new ArrayList<>(jar_files.stream().map(file -> new File(dir_abs, file)).toList());
        }
        addLocalDependencies(classpath, Scope.standalone);
        return classpath;
    }

    /**
     * Returns all the jar files that are in the test scope classpath.
     * <p>
     * By default, this collects all the jar files in the {@link #libTestDirectory()}
     * and adds all the jar files from the test scope local dependencies.
     *
     * @since 1.5
     */
    public List<File> testClasspathJars() {
        // detect the jar files in the test lib directory
        var dir_abs = libTestDirectory().getAbsoluteFile();
        var jar_files = FileUtils.getFileList(dir_abs, INCLUDED_JARS, EXCLUDED_JARS);

        // build the test classpath
        var classpath = new ArrayList<>(jar_files.stream().map(file -> new File(dir_abs, file)).toList());
        addLocalDependencies(classpath, Scope.test);
        return classpath;
    }

    private void addLocalDependencies(List<File> classpath, Scope scope) {
        if (dependencies.get(scope) == null) {
            return;
        }

        for (var dependency : dependencies.get(scope).localDependencies()) {
            var local_file = new File(workDirectory(), dependency.path());
            if (local_file.exists()) {
                if (local_file.isDirectory()) {
                    var local_jar_files = FileUtils.getFileList(local_file.getAbsoluteFile(), INCLUDED_JARS, EXCLUDED_JARS);
                    classpath.addAll(new ArrayList<>(local_jar_files.stream().map(file -> new File(local_file, file)).toList()));
                } else {
                    classpath.add(local_file);
                }
            }
        }
    }

    /**
     * Returns all the classpath entries for compiling the main sources.
     * <p>
     * By default, this converts the files from {@link #compileClasspathJars()} to absolute paths.
     *
     * @since 1.5
     */
    public List<String> compileMainClasspath() {
        return FileUtils.combineToAbsolutePaths(compileClasspathJars());
    }

    /**
     * Returns all the classpath entries for compiling the test sources.
     * <p>
     * By default, this converts the files from {@link #compileClasspathJars()} and
     * {@link #testClasspathJars()} to absolute paths, as well as the {@link #buildMainDirectory()}
     *
     * @since 1.5
     */
    public List<String> compileTestClasspath() {
        var paths = new ArrayList<String>();
        paths.add(buildMainDirectory().getAbsolutePath());
        paths.addAll(FileUtils.combineToAbsolutePaths(compileClasspathJars(), testClasspathJars()));
        return paths;
    }

    /**
     * Returns all the classpath entries for running the application.
     * <p>
     * By default, this converts the files from {@link #compileClasspathJars()},
     * {@link #runtimeClasspathJars()} and {@link #standaloneClasspathJars()} to absolute paths,
     * as well as the {@link #srcMainResourcesDirectory()} and {@link #buildMainDirectory()}
     *
     * @since 1.5
     */
    public List<String> runClasspath() {
        var paths = new ArrayList<String>();
        paths.add(srcMainResourcesDirectory().getAbsolutePath());
        paths.add(buildMainDirectory().getAbsolutePath());
        paths.addAll(FileUtils.combineToAbsolutePaths(compileClasspathJars(), runtimeClasspathJars(), standaloneClasspathJars()));
        return paths;
    }

    /**
     * Returns all the classpath entries for testing the application.
     * <p>
     * By default, this converts the files from {@link #compileClasspathJars()},
     * {@link #runtimeClasspathJars()}  and {@link #testClasspathJars()}
     * to absolute paths, as well as the {@link #srcMainResourcesDirectory()},
     * {@link #buildMainDirectory()} and {@link #buildTestDirectory()}
     *
     * @since 1.5
     */
    public List<String> testClasspath() {
        var paths = new ArrayList<String>();
        paths.add(srcMainResourcesDirectory().getAbsolutePath());
        paths.add(srcTestResourcesDirectory().getAbsolutePath());
        paths.add(buildMainDirectory().getAbsolutePath());
        paths.add(buildTestDirectory().getAbsolutePath());
        paths.addAll(FileUtils.combineToAbsolutePaths(compileClasspathJars(), runtimeClasspathJars(), standaloneClasspathJars(), testClasspathJars()));
        return paths;
    }

    /**
     * Executes download and purge commands automatically when the
     * {@code autoDownloadPurge} flag is set and changes have been detected.
     *
     * @throws Exception when an exception occurs during the auto download purge operation
     * @since 1.6.0
     */
    public void executeAutoDownloadPurge()
    throws Exception {
        download();
        purge();
    }

    private static final String BLD_BUILD_HASH = "bld-build.hash";

    private void performAutoDownloadPurge() {
        // verify and update the fingerprint hash file,
        // don't download and purge if the hash is identical
        var hash_file = new File(libBldDirectory(), BLD_BUILD_HASH);
        var hash = createHash();
        if (validateHash(hash_file, hash)) {
            return;
        }

        try {
            executeAutoDownloadPurge();

            writeHash(hash_file, hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createHash() {
        var finger_print = new StringBuilder();
        for (var repository : repositories()) {
            finger_print.append(repository.toString());
            finger_print.append("\n");
        }
        for (var entry : dependencies().entrySet()) {
            finger_print.append(entry.getKey());
            finger_print.append("\n");
            if (entry.getValue() != null) {
                for (var dependency : entry.getValue()) {
                    finger_print.append(dependency.toString());
                    finger_print.append("\n");
                }
            }
        }
        finger_print.append(downloadSources());
        finger_print.append("\n");
        finger_print.append(downloadJavadoc());
        finger_print.append("\n");

        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(finger_print.toString().getBytes(StandardCharsets.UTF_8));
            return StringUtils.encodeHexLower(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    private boolean validateHash(File hashFile, String hash) {
        try {
            if (hashFile.exists()) {
                var current_hash = FileUtils.readString(hashFile);
                if (current_hash.equals(hash)) {
                    return true;
                }
                hashFile.delete();
            }
            return false;
        } catch (FileUtilsErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeHash(File hashFile, String hash) {
        try {
            hashFile.getParentFile().mkdirs();
            FileUtils.writeString(hash, hashFile);
        } catch (FileUtilsErrorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int execute(String[] arguments) {
        if (autoDownloadPurge()) {
            performAutoDownloadPurge();
        }

        return super.execute(arguments);
    }
}
