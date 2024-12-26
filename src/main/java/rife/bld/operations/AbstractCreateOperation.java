/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BldVersion;
import rife.bld.Project;
import rife.bld.dependencies.VersionNumber;
import rife.bld.operations.exceptions.OperationOptionException;
import rife.bld.wrapper.Wrapper;
import rife.template.TemplateFactory;
import rife.tools.FileUtils;
import rife.tools.StringUtils;
import rife.tools.exceptions.FileUtilsErrorException;
import rife.validation.ValidityChecks;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Provides the baseline foundation for creating a project structure.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public abstract class AbstractCreateOperation<T extends AbstractCreateOperation<T, P>, P extends Project> extends AbstractOperation<AbstractCreateOperation<T, P>> {
    private String packageName_;
    private String projectName_;
    private String baseName_;

    final String templateBase_;

    File workDirectory_ = new File(System.getProperty("user.dir"));
    boolean downloadDependencies_;

    P project_;

    String projectBuildName_;
    String projectMainName_;
    String projectMainUberName_;
    String projectTestName_;

    File bldPackageDirectory_;
    File mainPackageDirectory_;
    File testPackageDirectory_;
    File ideaDirectory_;
    File ideaLibrariesDirectory_;
    File ideaRunConfigurationsDirectory_;
    File vscodeDirectory_;

    protected AbstractCreateOperation(String templateBase) {
        templateBase_ = templateBase;
    }

    /**
     * Performs the creation operation.
     *
     * @throws FileUtilsErrorException when an error occurred during the creation operation
     * @throws IOException             when an error occurred during the creation operation
     * @since 1.5
     */
    public void execute()
    throws IOException {
        if (packageName() == null || projectName() == null) {
            System.err.println("ERROR: Missing package or project name.");
            return;
        }

        executeConfigure();
        executeCreateProjectStructure();
        executePopulateProjectStructure();
        executePopulateIdeaProject();
        executePopulateVscodeProject();
        if (downloadDependencies()) {
            executeDownloadDependencies();
        }
        if (!silent()) {
            System.out.println("The project was successfully created at '" + project_.workDirectory() + "'.");
        }
    }

    /**
     * Create a blueprint of the project.
     *
     * @return a blueprint for project creation
     * @since 1.5
     */
    protected abstract P createProjectBlueprint();

    /**
     * Part of the {@link #execute} operation, configures the project.
     *
     * @since 1.5
     */
    protected void executeConfigure() {
        project_ = createProjectBlueprint();

        // standard names
        var base_name = baseName();
        projectBuildName_ = projectBuildClassName(base_name);
        projectMainName_ = projectMainClassName(base_name);
        projectMainUberName_ = projectMainUberClassName(base_name);
        projectTestName_ = projectTestClassName(base_name);

        // create the main project structure
        ideaDirectory_ = new File(project_.workDirectory(), ".idea");
        ideaLibrariesDirectory_ = new File(ideaDirectory_, "libraries");
        ideaRunConfigurationsDirectory_ = new File(ideaDirectory_, "runConfigurations");
        vscodeDirectory_ = new File(project_.workDirectory(), ".vscode");

        var package_dir = project_.pkg().replace('.', File.separatorChar);
        bldPackageDirectory_ = new File(project_.srcBldJavaDirectory(), package_dir);
        mainPackageDirectory_ = new File(project_.srcMainJavaDirectory(), package_dir);
        testPackageDirectory_ = new File(project_.srcTestJavaDirectory(), package_dir);
    }

    /**
     * Generates the build class name from the project class name
     * @param projectClassName the project class name
     * @return the generated build class name
     * @since 1.6
     */
    protected String projectBuildClassName(String projectClassName) {
        return projectClassName + "Build";
    }

    /**
     * Generates the main class name from the project class name
     * @param projectClassName the project class name
     * @return the generated main class name
     * @since 1.6
     */
    protected String projectMainClassName(String projectClassName) {
        return projectClassName;
    }

    /**
     * Generates the main uber class name from the project class name
     * @param projectClassName the project class name
     * @return the generated main uber class name
     * @since 1.6
     */
    protected String projectMainUberClassName(String projectClassName) {
        return projectClassName;
    }

    /**
     * Generates the test class name from the project class name
     * @param projectClassName the project class name
     * @return the generated test class name
     * @since 1.6
     */
    protected String projectTestClassName(String projectClassName) {
        return projectClassName + "Test";
    }

    /**
     * Part of the {@link #execute} operation, creates the project structure.
     *
     * @since 1.5
     */
    protected void executeCreateProjectStructure() {
        project_.createProjectStructure();

        bldPackageDirectory_.mkdirs();
        mainPackageDirectory_.mkdirs();
        testPackageDirectory_.mkdirs();

        ideaDirectory_.mkdirs();
        ideaLibrariesDirectory_.mkdirs();
        ideaRunConfigurationsDirectory_.mkdirs();
        vscodeDirectory_.mkdirs();
    }


    /**
     * Part of the {@link #execute} operation, populates the project structure.
     *
     * @since 1.5
     */
    protected void executePopulateProjectStructure()
    throws IOException {
        // project gitignore
        FileUtils.writeString(
            TemplateFactory.TXT.get(templateBase_ + "project_gitignore").getContent(),
            new File(project_.workDirectory(), ".gitignore"));

        // project main
        var site_template = TemplateFactory.TXT.get(templateBase_ + "project_main");
        site_template.setValue("package", project_.pkg());
        site_template.setValue("projectMain", projectMainName_);
        var project_main_file = new File(mainPackageDirectory_, projectMainName_ + ".java");
        FileUtils.writeString(site_template.getContent(), project_main_file);

        // project test
        var test_template = TemplateFactory.TXT.get(templateBase_ + "project_test");
        test_template.setValue("package", project_.pkg());
        test_template.setValue("projectTest", projectTestName_);
        test_template.setValue("projectMain", projectMainName_);
        if (test_template.hasValueId("project")) {
            test_template.setValue("project", project_.name());
        }
        var project_test_file = new File(testPackageDirectory_, projectTestName_ + ".java");
        FileUtils.writeString(test_template.getContent(), project_test_file);

        // project build
        var build_template = TemplateFactory.TXT.get(templateBase_ + "project_build");
        build_template.setValue("projectBuild", projectBuildName_);
        if (build_template.hasValueId("package")) {
            build_template.setValue("package", project_.pkg());
        }
        if (build_template.hasValueId("project")) {
            build_template.setValue("project", project_.name());
        }
        if (build_template.hasValueId("projectMain")) {
            build_template.setValue("projectMain", projectMainName_);
        }
        if (build_template.hasValueId("projectTest")) {
            build_template.setValue("projectTest", projectTestName_);
        }
        if (build_template.hasValueId("projectMainUber")) {
            build_template.setValue("projectMainUber", projectMainUberName_);
        }
        for (var entry : project_.dependencies().entrySet()) {
            build_template.blankValue("dependencies");

            for (var dependency : entry.getValue()) {
                build_template.setValue("groupId", dependency.groupId());
                build_template.setValue("artifactId", dependency.artifactId());
                var version = dependency.version();
                var version_string = "";
                if (version instanceof VersionNumber versionNumber) {
                    version_string = versionNumber.major() + "," + versionNumber.minor() + "," + versionNumber.revision();
                    if (!version.qualifier().isEmpty()) {
                        version_string += ",\"" + version.qualifier() + "\"";
                    }
                }
                else {
                    version_string = "\"" + version.toString() + "\"";
                }
                build_template.setValue("version", version_string);
                build_template.appendBlock("dependencies", "dependency");
            }
            build_template.setValue("name", entry.getKey().name());
            build_template.appendBlock("scopes", "scope");
        }
        var project_build_file = new File(bldPackageDirectory_, projectBuildName_ + ".java");
        FileUtils.writeString(build_template.getContent(), project_build_file);

        // build shell scripts
        var build_sh_template = TemplateFactory.TXT.get("bld.bld");
        build_sh_template.setValue("projectBuild", projectBuildName_);
        build_sh_template.setValue("package", project_.pkg());
        var build_sh_file = new File(project_.workDirectory(), "bld");
        FileUtils.writeString(build_sh_template.getContent(), build_sh_file);
        build_sh_file.setExecutable(true);

        var build_bat_template = TemplateFactory.TXT.get("bld.bld_bat");
        build_bat_template.setValue("projectBuild", projectBuildName_);
        build_bat_template.setValue("package", project_.pkg());
        var build_bat_file = new File(project_.workDirectory(), "bld.bat");
        FileUtils.writeString(build_bat_template.getContent(), build_bat_file);

        // create the wrapper files
        new Wrapper().createWrapperFiles(project_.libBldDirectory(), BldVersion.getVersion());
    }

    /**
     * Part of the {@link #execute} operation, populates the IDEA project structure.
     *
     * @since 1.5
     */
    protected void executePopulateIdeaProject()
    throws FileUtilsErrorException {
        // IDEA project files
        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.app_iml").getContent(),
            new File(ideaDirectory_, "app.iml"));
        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.bld_iml").getContent(),
            new File(ideaDirectory_, "bld.iml"));
        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.misc").getContent(),
            new File(ideaDirectory_, "misc.xml"));
        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.modules").getContent(),
            new File(ideaDirectory_, "modules.xml"));

        var bld_xml_template = TemplateFactory.XML.get(templateBase_ + "idea.libraries.bld");
        bld_xml_template.setValue("version", BldVersion.getVersion());
        var bld_xml_file = new File(ideaLibrariesDirectory_, "bld.xml");
        FileUtils.writeString(bld_xml_template.getContent(), bld_xml_file);

        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.libraries.compile").getContent(),
            new File(ideaLibrariesDirectory_, "compile.xml"));
        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.libraries.runtime").getContent(),
            new File(ideaLibrariesDirectory_, "runtime.xml"));
        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.libraries.test").getContent(),
            new File(ideaLibrariesDirectory_, "test.xml"));

        // IDEA run site
        if (createIdeaRunMain()) {
            var run_site_template = TemplateFactory.XML.get(templateBase_ + "idea.runConfigurations.Run_Main");
            run_site_template.setValue("package", project_.pkg());
            run_site_template.setValue("projectMain", projectMainName_);
            var run_site_file = new File(ideaRunConfigurationsDirectory_, "Run Main.xml");
            FileUtils.writeString(run_site_template.getContent(), run_site_file);
        }

        // IDEA run tests
        var run_tests_template = TemplateFactory.XML.get(templateBase_ + "idea.runConfigurations.Run_Tests");
        run_tests_template.setValue("package", project_.pkg());
        if (run_tests_template.hasValueId("projectTest")) {
            run_tests_template.setValue("projectTest", projectTestName_);
        }
        var run_tests_file = new File(ideaRunConfigurationsDirectory_, "Run Tests.xml");
        FileUtils.writeString(run_tests_template.getContent(), run_tests_file);
    }

    /**
     * Indicates whether the IDEA main run target should be generated
     * @return {@code true} of it should be generated; or {@code false} otherwise
     * @since 1.6
     */
    protected boolean createIdeaRunMain() {
        return true;
    }

    /**
     * Part of the {@link #execute} operation, populates the vscode project structure.
     *
     * @since 1.5.6
     */
    protected void executePopulateVscodeProject()
    throws FileUtilsErrorException {
        var launch_template = TemplateFactory.JSON.get(templateBase_ + "vscode.launch");
        launch_template.setValue("package", project_.pkg());
        if (launch_template.hasValueId("projectMain")) {
            launch_template.setValue("projectMain", projectMainName_);
        }
        if (launch_template.hasValueId("projectTest")) {
            launch_template.setValue("projectTest", projectTestName_);
        }
        var launch_file = new File(vscodeDirectory_, "launch.json");
        FileUtils.writeString(launch_template.getContent(), launch_file);

        var settings_template = TemplateFactory.JSON.get(templateBase_ + "vscode.settings");
        if (settings_template.hasValueId("version")) {
            settings_template.setValue("version", BldVersion.getVersion());
        }
        var settings_file = new File(vscodeDirectory_, "settings.json");
        FileUtils.writeString(settings_template.getContent(), settings_file);
    }

    /**
     * Part of the {@link #execute} operation, downloads the dependencies, when enabled.
     *
     * @since 1.5
     */
    protected void executeDownloadDependencies() {
        new DownloadOperation().fromProject(project_).execute();
    }

    /**
     * Configures a creation operation from command-line arguments.
     *
     * @param arguments the arguments that will be considered
     * @return this operation instance
     * @since 1.5
     */
    public T fromArguments(List<String> arguments) {
        String package_name = null;
        String project_name = null;
        String base_name = null;
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

        if (package_name == null || package_name.isEmpty()) {
            System.out.println("Please enter a package name (for instance: com.example):");
            package_name = System.console().readLine();
        } else {
            System.out.println("Using package name: " + package_name);
        }

        if (project_name == null || project_name.isEmpty()) {
            System.out.println("Please enter a project name (for instance: my-app):");
            project_name = System.console().readLine();
        } else {
            System.out.println("Using project name: " + project_name);
        }

        if (base_name == null || base_name.isEmpty()) {
            var default_base_name = generateBaseName(project_name);
            System.out.println("Please enter the base name for generated project classes (default: " + default_base_name + "):");
            base_name = System.console().readLine();
            if (base_name == null || base_name.trim().isEmpty()) {
                base_name = default_base_name;
                System.out.println("Using base name: " + base_name);
            }
        } else {
            System.out.println("Using base name: " + base_name);
        }

        return workDirectory(new File(System.getProperty("user.dir")))
            .packageName(package_name)
            .projectName(project_name)
            .baseName(base_name)
            .downloadDependencies(true);
    }

    /**
     * Provides the work directory in which the project will be created.
     * <p>
     * If no work directory is provided, the JVM working directory will be used.
     *
     * @param directory the directory to use as a work directory
     * @return this operation instance
     * @since 1.5
     */
    public T workDirectory(File directory) {
        if (!directory.exists()) {
            throw new OperationOptionException("ERROR: The work directory '" + directory + "' doesn't exist.");
        }
        if (!directory.isDirectory()) {
            throw new OperationOptionException("ERROR: '" + directory + "' is not a directory.");
        }
        if (!directory.canWrite()) {
            throw new OperationOptionException("ERROR: The work directory '" + directory + "' is not writable.");
        }

        workDirectory_ = directory;
        return (T) this;
    }

    /**
     * Provides the package of the project that will be created.
     *
     * @param name the package name
     * @return this operation instance
     * @since 1.5
     */
    public T packageName(String name) {
        packageName_ = StringUtils.trim(name);
        if (packageName_.isEmpty()) {
            throw new OperationOptionException("ERROR: The package name should not be blank.");
        }

        if (!ValidityChecks.checkJavaPackage(packageName_)) {
            throw new OperationOptionException("ERROR: The package name is invalid.");
        }

        return (T) this;
    }

    /**
     * Provides the name of the project that will be created.
     *
     * @param name the project name
     * @return this operation instance
     * @since 1.5
     */
    public T projectName(String name) {
        projectName_ = StringUtils.trim(name);
        if (projectName_.isEmpty()) {
            throw new OperationOptionException("ERROR: The project name should not be blank.");
        }

        return (T) this;
    }

    /**
     * Provides the base name for the project classes to generate.
     *
     * @param name the base name
     * @return this operation instance
     * @since 2.2
     */
    public T baseName(String name) {
        baseName_ = StringUtils.trim(name);
        if (baseName_.isEmpty()) {
            throw new OperationOptionException("ERROR: The base name should not be blank.");
        }

        if (!ValidityChecks.checkJavaIdentifier(baseName_)) {
            throw new OperationOptionException("ERROR: The base name is invalid.");
        }

        return (T) this;
    }

    /**
     * Indicates whether the dependencies for the project should be downloaded
     * upon creation, by default this is {@code false}.
     *
     * @param flag {@code true} if the dependencies should be downloaded; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 1.5
     */
    public T downloadDependencies(boolean flag) {
        downloadDependencies_ = flag;
        return (T) this;
    }

    /**
     * Retrieves the work directory that is used for the project creation.
     *
     * @return the work directory
     * @since 1.5
     */
    public File workDirectory() {
        return workDirectory_;
    }

    /**
     * Retrieves the package that is used for the project creation.
     *
     * @return the package name
     * @since 1.5
     */
    public String packageName() {
        return packageName_;
    }

    /**
     * Retrieves the name that is used for the project creation.
     *
     * @return the project name
     * @since 1.5
     */
    public String projectName() {
        return projectName_;
    }

    static String generateBaseName(String projectName) {
        if (projectName != null) {
            return StringUtils.filterAsIdentifier(projectName.trim(), true);
        }

        return null;
    }

    /**
     * Retrieves the base name for the project classes to generate.
     * <p>
     * If no base name was provided, one will be generated from the project name.
     *
     * @return the base name
     * @since 2.2
     */
    public String baseName() {
        if (baseName_ == null || baseName_.isEmpty()) {
            return generateBaseName(projectName());
        }

        return baseName_;
    }

    /**
     * Retrieves whether dependencies will be downloaded at project creation.
     *
     * @return {@code true} if dependencies will be downloaded; or
     * {@code false} otherwise
     * @since 1.5
     */
    public boolean downloadDependencies() {
        return downloadDependencies_;
    }

    /**
     * Retrieves the project instance that was used as a blueprint for the
     * project creation.
     *
     * @return the project creation blueprint instance
     * @since 1.5
     */
    public P project() {
        return project_;
    }
}
