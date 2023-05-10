/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.LocalDependency;
import rife.bld.dependencies.Scope;
import rife.bld.operations.exceptions.ExitStatusException;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TestCreateRife2Operation {
    @Test
    void testInstantiation() {
        var operation = new CreateRife2Operation();
        assertNotNull(operation.workDirectory());
        assertTrue(operation.workDirectory().exists());
        assertTrue(operation.workDirectory().isDirectory());
        assertTrue(operation.workDirectory().canWrite());
        assertFalse(operation.downloadDependencies());
        assertNull(operation.packageName());
        assertNull(operation.projectName());
    }

    @Test
    void testPopulation()
    throws Exception {
        var work_directory = Files.createTempDirectory("test").toFile();
        try {
            var download_dependencies = true;
            var package_name = "packageName";
            var project_name = "projectName";

            var operation = new CreateRife2Operation();
            operation
                .workDirectory(work_directory)
                .downloadDependencies(download_dependencies)
                .packageName(package_name)
                .projectName(project_name);

            assertEquals(work_directory, operation.workDirectory());
            assertEquals(download_dependencies, operation.downloadDependencies());
            assertEquals(package_name, operation.packageName());
            assertEquals(project_name, operation.projectName());
        } finally {
            FileUtils.deleteDirectory(work_directory);
        }
    }

    @Test
    void testExecute()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateRife2Operation()
                .workDirectory(tmp)
                .packageName("com.example")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation.execute();

            assertTrue(Pattern.compile("""
                    /myapp
                    /myapp/\\.gitignore
                    /myapp/\\.idea
                    /myapp/\\.idea/app\\.iml
                    /myapp/\\.idea/bld\\.iml
                    /myapp/\\.idea/libraries
                    /myapp/\\.idea/libraries/bld\\.xml
                    /myapp/\\.idea/libraries/compile\\.xml
                    /myapp/\\.idea/libraries/runtime\\.xml
                    /myapp/\\.idea/libraries/standalone\\.xml
                    /myapp/\\.idea/libraries/test\\.xml
                    /myapp/\\.idea/misc\\.xml
                    /myapp/\\.idea/modules\\.xml
                    /myapp/\\.idea/runConfigurations
                    /myapp/\\.idea/runConfigurations/Run Main\\.xml
                    /myapp/\\.idea/runConfigurations/Run Tests\\.xml
                    /myapp/\\.vscode
                    /myapp/\\.vscode/launch\\.json
                    /myapp/\\.vscode/settings\\.json
                    /myapp/bld
                    /myapp/bld\\.bat
                    /myapp/lib
                    /myapp/lib/bld
                    /myapp/lib/bld/bld-wrapper\\.jar
                    /myapp/lib/bld/bld-wrapper\\.properties
                    /myapp/lib/compile
                    /myapp/lib/compile/rife2-.+-sources\\.jar
                    /myapp/lib/compile/rife2-.+\\.jar
                    /myapp/lib/runtime
                    /myapp/lib/standalone
                    /myapp/lib/standalone/jetty-http-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-http-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-io-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-io-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-jakarta-servlet-api-5\\.0\\.2-sources\\.jar
                    /myapp/lib/standalone/jetty-jakarta-servlet-api-5\\.0\\.2\\.jar
                    /myapp/lib/standalone/jetty-security-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-security-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-server-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-server-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-servlet-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-servlet-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-util-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-util-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/slf4j-api-2\\.0\\.7-sources\\.jar
                    /myapp/lib/standalone/slf4j-api-2\\.0\\.7\\.jar
                    /myapp/lib/standalone/slf4j-simple-2\\.0\\.7-sources\\.jar
                    /myapp/lib/standalone/slf4j-simple-2\\.0\\.7\\.jar
                    /myapp/lib/test
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                    /myapp/lib/test/jsoup-1\\.16\\.1-sources\\.jar
                    /myapp/lib/test/jsoup-1\\.16\\.1\\.jar
                    /myapp/lib/test/junit-jupiter-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-jupiter-api-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-api-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-jupiter-engine-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-engine-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-jupiter-params-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-params-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-platform-commons-1\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-platform-commons-1\\.9\\.3\\.jar
                    /myapp/lib/test/junit-platform-console-standalone-1\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-platform-console-standalone-1\\.9\\.3\\.jar
                    /myapp/lib/test/junit-platform-engine-1\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-platform-engine-1\\.9\\.3\\.jar
                    /myapp/lib/test/opentest4j-1\\.2\\.0-sources\\.jar
                    /myapp/lib/test/opentest4j-1\\.2\\.0\\.jar
                    /myapp/src
                    /myapp/src/bld
                    /myapp/src/bld/java
                    /myapp/src/bld/java/com
                    /myapp/src/bld/java/com/example
                    /myapp/src/bld/java/com/example/MyappBuild\\.java
                    /myapp/src/bld/resources
                    /myapp/src/main
                    /myapp/src/main/java
                    /myapp/src/main/java/com
                    /myapp/src/main/java/com/example
                    /myapp/src/main/java/com/example/MyappSite\\.java
                    /myapp/src/main/java/com/example/MyappSiteUber\\.java
                    /myapp/src/main/resources
                    /myapp/src/main/resources/templates
                    /myapp/src/main/resources/templates/hello\\.html
                    /myapp/src/main/webapp
                    /myapp/src/main/webapp/WEB-INF
                    /myapp/src/main/webapp/WEB-INF/web\\.xml
                    /myapp/src/main/webapp/css
                    /myapp/src/main/webapp/css/style\\.css
                    /myapp/src/test
                    /myapp/src/test/java
                    /myapp/src/test/java/com
                    /myapp/src/test/java/com/example
                    /myapp/src/test/java/com/example/MyappTest\\.java
                    /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
            assertTrue(Pattern.compile("""
                    /myapp
                    /myapp/\\.gitignore
                    /myapp/\\.idea
                    /myapp/\\.idea/app\\.iml
                    /myapp/\\.idea/bld\\.iml
                    /myapp/\\.idea/libraries
                    /myapp/\\.idea/libraries/bld\\.xml
                    /myapp/\\.idea/libraries/compile\\.xml
                    /myapp/\\.idea/libraries/runtime\\.xml
                    /myapp/\\.idea/libraries/standalone\\.xml
                    /myapp/\\.idea/libraries/test\\.xml
                    /myapp/\\.idea/misc\\.xml
                    /myapp/\\.idea/modules\\.xml
                    /myapp/\\.idea/runConfigurations
                    /myapp/\\.idea/runConfigurations/Run Main\\.xml
                    /myapp/\\.idea/runConfigurations/Run Tests\\.xml
                    /myapp/\\.vscode
                    /myapp/\\.vscode/launch\\.json
                    /myapp/\\.vscode/settings\\.json
                    /myapp/bld
                    /myapp/bld\\.bat
                    /myapp/build
                    /myapp/build/main
                    /myapp/build/main/com
                    /myapp/build/main/com/example
                    /myapp/build/main/com/example/MyappSite\\.class
                    /myapp/build/main/com/example/MyappSiteUber\\.class
                    /myapp/build/test
                    /myapp/build/test/com
                    /myapp/build/test/com/example
                    /myapp/build/test/com/example/MyappTest\\.class
                    /myapp/lib
                    /myapp/lib/bld
                    /myapp/lib/bld/bld-wrapper\\.jar
                    /myapp/lib/bld/bld-wrapper\\.properties
                    /myapp/lib/compile
                    /myapp/lib/compile/rife2-.+-sources\\.jar
                    /myapp/lib/compile/rife2-.+\\.jar
                    /myapp/lib/runtime
                    /myapp/lib/standalone
                    /myapp/lib/standalone/jetty-http-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-http-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-io-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-io-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-jakarta-servlet-api-5\\.0\\.2-sources\\.jar
                    /myapp/lib/standalone/jetty-jakarta-servlet-api-5\\.0\\.2\\.jar
                    /myapp/lib/standalone/jetty-security-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-security-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-server-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-server-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-servlet-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-servlet-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/jetty-util-11\\.0\\.15-sources\\.jar
                    /myapp/lib/standalone/jetty-util-11\\.0\\.15\\.jar
                    /myapp/lib/standalone/slf4j-api-2\\.0\\.7-sources\\.jar
                    /myapp/lib/standalone/slf4j-api-2\\.0\\.7\\.jar
                    /myapp/lib/standalone/slf4j-simple-2\\.0\\.7-sources\\.jar
                    /myapp/lib/standalone/slf4j-simple-2\\.0\\.7\\.jar
                    /myapp/lib/test
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                    /myapp/lib/test/jsoup-1\\.16\\.1-sources\\.jar
                    /myapp/lib/test/jsoup-1\\.16\\.1\\.jar
                    /myapp/lib/test/junit-jupiter-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-jupiter-api-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-api-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-jupiter-engine-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-engine-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-jupiter-params-5\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-jupiter-params-5\\.9\\.3\\.jar
                    /myapp/lib/test/junit-platform-commons-1\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-platform-commons-1\\.9\\.3\\.jar
                    /myapp/lib/test/junit-platform-console-standalone-1\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-platform-console-standalone-1\\.9\\.3\\.jar
                    /myapp/lib/test/junit-platform-engine-1\\.9\\.3-sources\\.jar
                    /myapp/lib/test/junit-platform-engine-1\\.9\\.3\\.jar
                    /myapp/lib/test/opentest4j-1\\.2\\.0-sources\\.jar
                    /myapp/lib/test/opentest4j-1\\.2\\.0\\.jar
                    /myapp/src
                    /myapp/src/bld
                    /myapp/src/bld/java
                    /myapp/src/bld/java/com
                    /myapp/src/bld/java/com/example
                    /myapp/src/bld/java/com/example/MyappBuild\\.java
                    /myapp/src/bld/resources
                    /myapp/src/main
                    /myapp/src/main/java
                    /myapp/src/main/java/com
                    /myapp/src/main/java/com/example
                    /myapp/src/main/java/com/example/MyappSite\\.java
                    /myapp/src/main/java/com/example/MyappSiteUber\\.java
                    /myapp/src/main/resources
                    /myapp/src/main/resources/templates
                    /myapp/src/main/resources/templates/hello\\.html
                    /myapp/src/main/webapp
                    /myapp/src/main/webapp/WEB-INF
                    /myapp/src/main/webapp/WEB-INF/web\\.xml
                    /myapp/src/main/webapp/css
                    /myapp/src/main/webapp/css/style\\.css
                    /myapp/src/test
                    /myapp/src/test/java
                    /myapp/src/test/java/com
                    /myapp/src/test/java/com/example
                    /myapp/src/test/java/com/example/MyappTest\\.java
                    /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var run_operation = new RunOperation().fromProject(create_operation.project());
            var executor = Executors.newSingleThreadScheduledExecutor();
            var checked_url = new URL("http://localhost:8080");
            var check_result = new StringBuilder();
            executor.schedule(() -> {
                try {
                    check_result.append(FileUtils.readString(checked_url));
                } catch (FileUtilsErrorException e) {
                    throw new RuntimeException(e);
                }
            }, 1, TimeUnit.SECONDS);
            executor.schedule(() -> run_operation.process().destroy(), 2, TimeUnit.SECONDS);
            assertThrows(ExitStatusException.class, run_operation::execute);

            assertTrue(check_result.toString().contains("<p>Hello World Myapp</p>"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteNoDownload()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateRife2Operation()
                .workDirectory(tmp)
                .packageName("org.stuff")
                .projectName("yourthing");
            create_operation.execute();

            assertEquals("""
                    /yourthing
                    /yourthing/.gitignore
                    /yourthing/.idea
                    /yourthing/.idea/app.iml
                    /yourthing/.idea/bld.iml
                    /yourthing/.idea/libraries
                    /yourthing/.idea/libraries/bld.xml
                    /yourthing/.idea/libraries/compile.xml
                    /yourthing/.idea/libraries/runtime.xml
                    /yourthing/.idea/libraries/standalone.xml
                    /yourthing/.idea/libraries/test.xml
                    /yourthing/.idea/misc.xml
                    /yourthing/.idea/modules.xml
                    /yourthing/.idea/runConfigurations
                    /yourthing/.idea/runConfigurations/Run Main.xml
                    /yourthing/.idea/runConfigurations/Run Tests.xml
                    /yourthing/.vscode
                    /yourthing/.vscode/launch.json
                    /yourthing/.vscode/settings.json
                    /yourthing/bld
                    /yourthing/bld.bat
                    /yourthing/lib
                    /yourthing/lib/bld
                    /yourthing/lib/bld/bld-wrapper.jar
                    /yourthing/lib/bld/bld-wrapper.properties
                    /yourthing/lib/compile
                    /yourthing/lib/runtime
                    /yourthing/lib/standalone
                    /yourthing/lib/test
                    /yourthing/src
                    /yourthing/src/bld
                    /yourthing/src/bld/java
                    /yourthing/src/bld/java/org
                    /yourthing/src/bld/java/org/stuff
                    /yourthing/src/bld/java/org/stuff/YourthingBuild.java
                    /yourthing/src/bld/resources
                    /yourthing/src/main
                    /yourthing/src/main/java
                    /yourthing/src/main/java/org
                    /yourthing/src/main/java/org/stuff
                    /yourthing/src/main/java/org/stuff/YourthingSite.java
                    /yourthing/src/main/java/org/stuff/YourthingSiteUber.java
                    /yourthing/src/main/resources
                    /yourthing/src/main/resources/templates
                    /yourthing/src/main/resources/templates/hello.html
                    /yourthing/src/main/webapp
                    /yourthing/src/main/webapp/WEB-INF
                    /yourthing/src/main/webapp/WEB-INF/web.xml
                    /yourthing/src/main/webapp/css
                    /yourthing/src/main/webapp/css/style.css
                    /yourthing/src/test
                    /yourthing/src/test/java
                    /yourthing/src/test/java/org
                    /yourthing/src/test/java/org/stuff
                    /yourthing/src/test/java/org/stuff/YourthingTest.java
                    /yourthing/src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));

            var compile_operation = new CompileOperation() {
                public void executeProcessDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
                    // don't output errors
                }
            };
            compile_operation.fromProject(create_operation.project());
            assertThrows(ExitStatusException.class, compile_operation::execute);
            var diagnostics = compile_operation.diagnostics();
            assertEquals(16, diagnostics.size());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteLocalDependencies()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateRife2Operation()
                .workDirectory(tmp)
                .packageName("com.example")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation.execute();

            var project = create_operation.project();
            var lib_local = new File(project.libDirectory(), "local");
            lib_local.mkdirs();
            for (var lib : FileUtils.getFileList(project.libCompileDirectory())) {
                if (!lib.endsWith("-sources.jar")) {
                    project.dependencies().scope(Scope.compile).include(new LocalDependency(Path.of("lib", "local", lib).toString()));
                }
                new File(project.libCompileDirectory(), lib).renameTo(new File(lib_local, lib));
            }
            for (var lib : FileUtils.getFileList(project.libStandaloneDirectory())) {
                if (!lib.endsWith("-sources.jar")) {
                    project.dependencies().scope(Scope.standalone).include(new LocalDependency(Path.of("lib", "local", lib).toString()));
                }
                new File(project.libStandaloneDirectory(), lib).renameTo(new File(lib_local, lib));
            }
            for (var lib : FileUtils.getFileList(project.libTestDirectory())) {
                if (!lib.endsWith("-sources.jar")) {
                    project.dependencies().scope(Scope.test).include(new LocalDependency(Path.of("lib", "local", lib).toString()));
                }
                new File(project.libTestDirectory(), lib).renameTo(new File(lib_local, lib));
            }

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
            assertTrue(Pattern.compile("""
                /myapp
                /myapp/\\.gitignore
                /myapp/\\.idea
                /myapp/\\.idea/app\\.iml
                /myapp/\\.idea/bld\\.iml
                /myapp/\\.idea/libraries
                /myapp/\\.idea/libraries/bld\\.xml
                /myapp/\\.idea/libraries/compile\\.xml
                /myapp/\\.idea/libraries/runtime\\.xml
                /myapp/\\.idea/libraries/standalone\\.xml
                /myapp/\\.idea/libraries/test\\.xml
                /myapp/\\.idea/misc\\.xml
                /myapp/\\.idea/modules\\.xml
                /myapp/\\.idea/runConfigurations
                /myapp/\\.idea/runConfigurations/Run Main\\.xml
                /myapp/\\.idea/runConfigurations/Run Tests\\.xml
                /myapp/\\.vscode
                /myapp/\\.vscode/launch\\.json
                /myapp/\\.vscode/settings\\.json
                /myapp/bld
                /myapp/bld\\.bat
                /myapp/build
                /myapp/build/main
                /myapp/build/main/com
                /myapp/build/main/com/example
                /myapp/build/main/com/example/MyappSite\\.class
                /myapp/build/main/com/example/MyappSiteUber\\.class
                /myapp/build/test
                /myapp/build/test/com
                /myapp/build/test/com/example
                /myapp/build/test/com/example/MyappTest\\.class
                /myapp/lib
                /myapp/lib/bld
                /myapp/lib/bld/bld-wrapper\\.jar
                /myapp/lib/bld/bld-wrapper\\.properties
                /myapp/lib/compile
                /myapp/lib/local
                /myapp/lib/local/apiguardian-api-1\\.1\\.2-sources\\.jar
                /myapp/lib/local/apiguardian-api-1\\.1\\.2\\.jar
                /myapp/lib/local/jetty-http-11\\.0\\.15-sources\\.jar
                /myapp/lib/local/jetty-http-11\\.0\\.15\\.jar
                /myapp/lib/local/jetty-io-11\\.0\\.15-sources\\.jar
                /myapp/lib/local/jetty-io-11\\.0\\.15\\.jar
                /myapp/lib/local/jetty-jakarta-servlet-api-5\\.0\\.2-sources\\.jar
                /myapp/lib/local/jetty-jakarta-servlet-api-5\\.0\\.2\\.jar
                /myapp/lib/local/jetty-security-11\\.0\\.15-sources\\.jar
                /myapp/lib/local/jetty-security-11\\.0\\.15\\.jar
                /myapp/lib/local/jetty-server-11\\.0\\.15-sources\\.jar
                /myapp/lib/local/jetty-server-11\\.0\\.15\\.jar
                /myapp/lib/local/jetty-servlet-11\\.0\\.15-sources\\.jar
                /myapp/lib/local/jetty-servlet-11\\.0\\.15\\.jar
                /myapp/lib/local/jetty-util-11\\.0\\.15-sources\\.jar
                /myapp/lib/local/jetty-util-11\\.0\\.15\\.jar
                /myapp/lib/local/jsoup-1\\.16\\.1-sources\\.jar
                /myapp/lib/local/jsoup-1\\.16\\.1\\.jar
                /myapp/lib/local/junit-jupiter-5\\.9\\.3-sources\\.jar
                /myapp/lib/local/junit-jupiter-5\\.9\\.3\\.jar
                /myapp/lib/local/junit-jupiter-api-5\\.9\\.3-sources\\.jar
                /myapp/lib/local/junit-jupiter-api-5\\.9\\.3\\.jar
                /myapp/lib/local/junit-jupiter-engine-5\\.9\\.3-sources\\.jar
                /myapp/lib/local/junit-jupiter-engine-5\\.9\\.3\\.jar
                /myapp/lib/local/junit-jupiter-params-5\\.9\\.3-sources\\.jar
                /myapp/lib/local/junit-jupiter-params-5\\.9\\.3\\.jar
                /myapp/lib/local/junit-platform-commons-1\\.9\\.3-sources\\.jar
                /myapp/lib/local/junit-platform-commons-1\\.9\\.3\\.jar
                /myapp/lib/local/junit-platform-console-standalone-1\\.9\\.3-sources\\.jar
                /myapp/lib/local/junit-platform-console-standalone-1\\.9\\.3\\.jar
                /myapp/lib/local/junit-platform-engine-1\\.9\\.3-sources\\.jar
                /myapp/lib/local/junit-platform-engine-1\\.9\\.3\\.jar
                /myapp/lib/local/opentest4j-1\\.2\\.0-sources\\.jar
                /myapp/lib/local/opentest4j-1\\.2\\.0\\.jar
                /myapp/lib/local/rife2-.*-sources\\.jar
                /myapp/lib/local/rife2-.*\\.jar
                /myapp/lib/local/slf4j-api-2\\.0\\.7-sources\\.jar
                /myapp/lib/local/slf4j-api-2\\.0\\.7\\.jar
                /myapp/lib/local/slf4j-simple-2\\.0\\.7-sources\\.jar
                /myapp/lib/local/slf4j-simple-2\\.0\\.7\\.jar
                /myapp/lib/runtime
                /myapp/lib/standalone
                /myapp/lib/test
                /myapp/src
                /myapp/src/bld
                /myapp/src/bld/java
                /myapp/src/bld/java/com
                /myapp/src/bld/java/com/example
                /myapp/src/bld/java/com/example/MyappBuild\\.java
                /myapp/src/bld/resources
                /myapp/src/main
                /myapp/src/main/java
                /myapp/src/main/java/com
                /myapp/src/main/java/com/example
                /myapp/src/main/java/com/example/MyappSite\\.java
                /myapp/src/main/java/com/example/MyappSiteUber\\.java
                /myapp/src/main/resources
                /myapp/src/main/resources/templates
                /myapp/src/main/resources/templates/hello\\.html
                /myapp/src/main/webapp
                /myapp/src/main/webapp/WEB-INF
                /myapp/src/main/webapp/WEB-INF/web\\.xml
                /myapp/src/main/webapp/css
                /myapp/src/main/webapp/css/style\\.css
                /myapp/src/test
                /myapp/src/test/java
                /myapp/src/test/java/com
                /myapp/src/test/java/com/example
                /myapp/src/test/java/com/example/MyappTest\\.java
                /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var run_operation = new RunOperation().fromProject(create_operation.project());
            var executor = Executors.newSingleThreadScheduledExecutor();
            var checked_url = new URL("http://localhost:8080");
            var check_result = new StringBuilder();
            executor.schedule(() -> {
                try {
                    check_result.append(FileUtils.readString(checked_url));
                } catch (FileUtilsErrorException e) {
                    throw new RuntimeException(e);
                }
            }, 1, TimeUnit.SECONDS);
            executor.schedule(() -> run_operation.process().destroy(), 2, TimeUnit.SECONDS);
            assertThrows(ExitStatusException.class, run_operation::execute);

            assertTrue(check_result.toString().contains("<p>Hello World Myapp</p>"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteLocalDependenciesFolders()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateRife2Operation()
                .workDirectory(tmp)
                .packageName("com.example")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation.execute();

            var project = create_operation.project();
            var lib_local_compile = new File(project.libDirectory(), "local_compile");
            lib_local_compile.mkdirs();
            project.dependencies().scope(Scope.compile).include(new LocalDependency(Path.of("lib", "local_compile").toString()));
            for (var lib : FileUtils.getFileList(project.libCompileDirectory())) {
                new File(project.libCompileDirectory(), lib).renameTo(new File(lib_local_compile, lib));
            }
            var lib_local_standalone = new File(project.libDirectory(), "local_standalone");
            lib_local_standalone.mkdirs();
            project.dependencies().scope(Scope.standalone).include(new LocalDependency(Path.of("lib", "local_standalone").toString()));
            for (var lib : FileUtils.getFileList(project.libStandaloneDirectory())) {
                new File(project.libStandaloneDirectory(), lib).renameTo(new File(lib_local_standalone, lib));
            }
            var lib_local_test = new File(project.libDirectory(), "local_test");
            lib_local_test.mkdirs();
            project.dependencies().scope(Scope.test).include(new LocalDependency(Path.of("lib", "local_test").toString()));
            for (var lib : FileUtils.getFileList(project.libTestDirectory())) {
                new File(project.libTestDirectory(), lib).renameTo(new File(lib_local_test, lib));
            }

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
            assertTrue(Pattern.compile("""
                /myapp
                /myapp/\\.gitignore
                /myapp/\\.idea
                /myapp/\\.idea/app\\.iml
                /myapp/\\.idea/bld\\.iml
                /myapp/\\.idea/libraries
                /myapp/\\.idea/libraries/bld\\.xml
                /myapp/\\.idea/libraries/compile\\.xml
                /myapp/\\.idea/libraries/runtime\\.xml
                /myapp/\\.idea/libraries/standalone\\.xml
                /myapp/\\.idea/libraries/test\\.xml
                /myapp/\\.idea/misc\\.xml
                /myapp/\\.idea/modules\\.xml
                /myapp/\\.idea/runConfigurations
                /myapp/\\.idea/runConfigurations/Run Main\\.xml
                /myapp/\\.idea/runConfigurations/Run Tests\\.xml
                /myapp/\\.vscode
                /myapp/\\.vscode/launch\\.json
                /myapp/\\.vscode/settings\\.json
                /myapp/bld
                /myapp/bld\\.bat
                /myapp/build
                /myapp/build/main
                /myapp/build/main/com
                /myapp/build/main/com/example
                /myapp/build/main/com/example/MyappSite\\.class
                /myapp/build/main/com/example/MyappSiteUber\\.class
                /myapp/build/test
                /myapp/build/test/com
                /myapp/build/test/com/example
                /myapp/build/test/com/example/MyappTest\\.class
                /myapp/lib
                /myapp/lib/bld
                /myapp/lib/bld/bld-wrapper\\.jar
                /myapp/lib/bld/bld-wrapper\\.properties
                /myapp/lib/compile
                /myapp/lib/local_compile
                /myapp/lib/local_compile/rife2-.*-sources\\.jar
                /myapp/lib/local_compile/rife2-.*\\.jar
                /myapp/lib/local_standalone
                /myapp/lib/local_standalone/jetty-http-11\\.0\\.15-sources\\.jar
                /myapp/lib/local_standalone/jetty-http-11\\.0\\.15\\.jar
                /myapp/lib/local_standalone/jetty-io-11\\.0\\.15-sources\\.jar
                /myapp/lib/local_standalone/jetty-io-11\\.0\\.15\\.jar
                /myapp/lib/local_standalone/jetty-jakarta-servlet-api-5\\.0\\.2-sources\\.jar
                /myapp/lib/local_standalone/jetty-jakarta-servlet-api-5\\.0\\.2\\.jar
                /myapp/lib/local_standalone/jetty-security-11\\.0\\.15-sources\\.jar
                /myapp/lib/local_standalone/jetty-security-11\\.0\\.15\\.jar
                /myapp/lib/local_standalone/jetty-server-11\\.0\\.15-sources\\.jar
                /myapp/lib/local_standalone/jetty-server-11\\.0\\.15\\.jar
                /myapp/lib/local_standalone/jetty-servlet-11\\.0\\.15-sources\\.jar
                /myapp/lib/local_standalone/jetty-servlet-11\\.0\\.15\\.jar
                /myapp/lib/local_standalone/jetty-util-11\\.0\\.15-sources\\.jar
                /myapp/lib/local_standalone/jetty-util-11\\.0\\.15\\.jar
                /myapp/lib/local_standalone/slf4j-api-2\\.0\\.7-sources\\.jar
                /myapp/lib/local_standalone/slf4j-api-2\\.0\\.7\\.jar
                /myapp/lib/local_standalone/slf4j-simple-2\\.0\\.7-sources\\.jar
                /myapp/lib/local_standalone/slf4j-simple-2\\.0\\.7\\.jar
                /myapp/lib/local_test
                /myapp/lib/local_test/apiguardian-api-1\\.1\\.2-sources\\.jar
                /myapp/lib/local_test/apiguardian-api-1\\.1\\.2\\.jar
                /myapp/lib/local_test/jsoup-1\\.16\\.1-sources\\.jar
                /myapp/lib/local_test/jsoup-1\\.16\\.1\\.jar
                /myapp/lib/local_test/junit-jupiter-5\\.9\\.3-sources\\.jar
                /myapp/lib/local_test/junit-jupiter-5\\.9\\.3\\.jar
                /myapp/lib/local_test/junit-jupiter-api-5\\.9\\.3-sources\\.jar
                /myapp/lib/local_test/junit-jupiter-api-5\\.9\\.3\\.jar
                /myapp/lib/local_test/junit-jupiter-engine-5\\.9\\.3-sources\\.jar
                /myapp/lib/local_test/junit-jupiter-engine-5\\.9\\.3\\.jar
                /myapp/lib/local_test/junit-jupiter-params-5\\.9\\.3-sources\\.jar
                /myapp/lib/local_test/junit-jupiter-params-5\\.9\\.3\\.jar
                /myapp/lib/local_test/junit-platform-commons-1\\.9\\.3-sources\\.jar
                /myapp/lib/local_test/junit-platform-commons-1\\.9\\.3\\.jar
                /myapp/lib/local_test/junit-platform-console-standalone-1\\.9\\.3-sources\\.jar
                /myapp/lib/local_test/junit-platform-console-standalone-1\\.9\\.3\\.jar
                /myapp/lib/local_test/junit-platform-engine-1\\.9\\.3-sources\\.jar
                /myapp/lib/local_test/junit-platform-engine-1\\.9\\.3\\.jar
                /myapp/lib/local_test/opentest4j-1\\.2\\.0-sources\\.jar
                /myapp/lib/local_test/opentest4j-1\\.2\\.0\\.jar
                /myapp/lib/runtime
                /myapp/lib/standalone
                /myapp/lib/test
                /myapp/src
                /myapp/src/bld
                /myapp/src/bld/java
                /myapp/src/bld/java/com
                /myapp/src/bld/java/com/example
                /myapp/src/bld/java/com/example/MyappBuild\\.java
                /myapp/src/bld/resources
                /myapp/src/main
                /myapp/src/main/java
                /myapp/src/main/java/com
                /myapp/src/main/java/com/example
                /myapp/src/main/java/com/example/MyappSite\\.java
                /myapp/src/main/java/com/example/MyappSiteUber\\.java
                /myapp/src/main/resources
                /myapp/src/main/resources/templates
                /myapp/src/main/resources/templates/hello\\.html
                /myapp/src/main/webapp
                /myapp/src/main/webapp/WEB-INF
                /myapp/src/main/webapp/WEB-INF/web\\.xml
                /myapp/src/main/webapp/css
                /myapp/src/main/webapp/css/style\\.css
                /myapp/src/test
                /myapp/src/test/java
                /myapp/src/test/java/com
                /myapp/src/test/java/com/example
                /myapp/src/test/java/com/example/MyappTest\\.java
                /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var run_operation = new RunOperation().fromProject(create_operation.project());
            var executor = Executors.newSingleThreadScheduledExecutor();
            var checked_url = new URL("http://localhost:8080");
            var check_result = new StringBuilder();
            executor.schedule(() -> {
                try {
                    check_result.append(FileUtils.readString(checked_url));
                } catch (FileUtilsErrorException e) {
                    throw new RuntimeException(e);
                }
            }, 1, TimeUnit.SECONDS);
            executor.schedule(() -> run_operation.process().destroy(), 2, TimeUnit.SECONDS);
            assertThrows(ExitStatusException.class, run_operation::execute);

            assertTrue(check_result.toString().contains("<p>Hello World Myapp</p>"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
