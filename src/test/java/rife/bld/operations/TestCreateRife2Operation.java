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
                .projectName("my-app")
                .baseName("MyApp")
                .downloadDependencies(true);
            create_operation.execute();

            assertTrue(Pattern.compile("""
                    /my-app
                    /my-app/\\.gitignore
                    /my-app/\\.idea
                    /my-app/\\.idea/app\\.iml
                    /my-app/\\.idea/bld\\.iml
                    /my-app/\\.idea/libraries
                    /my-app/\\.idea/libraries/bld\\.xml
                    /my-app/\\.idea/libraries/compile\\.xml
                    /my-app/\\.idea/libraries/runtime\\.xml
                    /my-app/\\.idea/libraries/standalone\\.xml
                    /my-app/\\.idea/libraries/test\\.xml
                    /my-app/\\.idea/misc\\.xml
                    /my-app/\\.idea/modules\\.xml
                    /my-app/\\.idea/runConfigurations
                    /my-app/\\.idea/runConfigurations/Run Main\\.xml
                    /my-app/\\.idea/runConfigurations/Run Tests\\.xml
                    /my-app/\\.vscode
                    /my-app/\\.vscode/launch\\.json
                    /my-app/\\.vscode/settings\\.json
                    /my-app/bld
                    /my-app/bld\\.bat
                    /my-app/lib
                    /my-app/lib/bld
                    /my-app/lib/bld/bld-wrapper\\.jar
                    /my-app/lib/bld/bld-wrapper\\.properties
                    /my-app/lib/compile
                    /my-app/lib/compile/modules
                    /my-app/lib/compile/rife2-.+-sources\\.jar
                    /my-app/lib/compile/rife2-.+\\.jar
                    /my-app/lib/provided
                    /my-app/lib/provided/modules
                    /my-app/lib/runtime
                    /my-app/lib/runtime/modules
                    /my-app/lib/standalone
                    /my-app/lib/standalone/jakarta\\.servlet-api-6\\.0\\.0-sources\\.jar
                    /my-app/lib/standalone/jakarta\\.servlet-api-6\\.0\\.0\\.jar
                    /my-app/lib/standalone/jetty-ee10-servlet-12.0.12-sources\\.jar
                    /my-app/lib/standalone/jetty-ee10-servlet-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-http-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-http-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-io-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-io-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-security-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-security-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-server-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-server-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-session-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-session-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-util-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-util-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/modules
                    /my-app/lib/standalone/slf4j-api-2\\.0\\.13-sources\\.jar
                    /my-app/lib/standalone/slf4j-api-2\\.0\\.13\\.jar
                    /my-app/lib/standalone/slf4j-simple-2\\.0\\.13-sources\\.jar
                    /my-app/lib/standalone/slf4j-simple-2\\.0\\.13\\.jar
                    /my-app/lib/test
                    /my-app/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                    /my-app/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                    /my-app/lib/test/jsoup-1\\.18\\.1-sources\\.jar
                    /my-app/lib/test/jsoup-1\\.18\\.1\\.jar
                    /my-app/lib/test/junit-jupiter-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-jupiter-api-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-api-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-jupiter-engine-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-engine-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-jupiter-params-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-params-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-platform-commons-1\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-platform-commons-1\\.11\\.0\\.jar
                    /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.0\\.jar
                    /my-app/lib/test/junit-platform-engine-1\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-platform-engine-1\\.11\\.0\\.jar
                    /my-app/lib/test/modules
                    /my-app/lib/test/opentest4j-1\\.3\\.0-sources\\.jar
                    /my-app/lib/test/opentest4j-1\\.3\\.0\\.jar
                    /my-app/src
                    /my-app/src/bld
                    /my-app/src/bld/java
                    /my-app/src/bld/java/com
                    /my-app/src/bld/java/com/example
                    /my-app/src/bld/java/com/example/MyAppBuild\\.java
                    /my-app/src/bld/resources
                    /my-app/src/main
                    /my-app/src/main/java
                    /my-app/src/main/java/com
                    /my-app/src/main/java/com/example
                    /my-app/src/main/java/com/example/MyAppSite\\.java
                    /my-app/src/main/java/com/example/MyAppSiteUber\\.java
                    /my-app/src/main/resources
                    /my-app/src/main/resources/templates
                    /my-app/src/main/resources/templates/hello\\.html
                    /my-app/src/main/webapp
                    /my-app/src/main/webapp/WEB-INF
                    /my-app/src/main/webapp/WEB-INF/web\\.xml
                    /my-app/src/main/webapp/css
                    /my-app/src/main/webapp/css/style\\.css
                    /my-app/src/test
                    /my-app/src/test/java
                    /my-app/src/test/java/com
                    /my-app/src/test/java/com/example
                    /my-app/src/test/java/com/example/MyAppTest\\.java
                    /my-app/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
            assertTrue(Pattern.compile("""
                    /my-app
                    /my-app/\\.gitignore
                    /my-app/\\.idea
                    /my-app/\\.idea/app\\.iml
                    /my-app/\\.idea/bld\\.iml
                    /my-app/\\.idea/libraries
                    /my-app/\\.idea/libraries/bld\\.xml
                    /my-app/\\.idea/libraries/compile\\.xml
                    /my-app/\\.idea/libraries/runtime\\.xml
                    /my-app/\\.idea/libraries/standalone\\.xml
                    /my-app/\\.idea/libraries/test\\.xml
                    /my-app/\\.idea/misc\\.xml
                    /my-app/\\.idea/modules\\.xml
                    /my-app/\\.idea/runConfigurations
                    /my-app/\\.idea/runConfigurations/Run Main\\.xml
                    /my-app/\\.idea/runConfigurations/Run Tests\\.xml
                    /my-app/\\.vscode
                    /my-app/\\.vscode/launch\\.json
                    /my-app/\\.vscode/settings\\.json
                    /my-app/bld
                    /my-app/bld\\.bat
                    /my-app/build
                    /my-app/build/main
                    /my-app/build/main/com
                    /my-app/build/main/com/example
                    /my-app/build/main/com/example/MyAppSite\\.class
                    /my-app/build/main/com/example/MyAppSiteUber\\.class
                    /my-app/build/test
                    /my-app/build/test/com
                    /my-app/build/test/com/example
                    /my-app/build/test/com/example/MyAppTest\\.class
                    /my-app/lib
                    /my-app/lib/bld
                    /my-app/lib/bld/bld-wrapper\\.jar
                    /my-app/lib/bld/bld-wrapper\\.properties
                    /my-app/lib/compile
                    /my-app/lib/compile/modules
                    /my-app/lib/compile/rife2-.+-sources\\.jar
                    /my-app/lib/compile/rife2-.+\\.jar
                    /my-app/lib/provided
                    /my-app/lib/provided/modules
                    /my-app/lib/runtime
                    /my-app/lib/runtime/modules
                    /my-app/lib/standalone
                    /my-app/lib/standalone/jakarta\\.servlet-api-6\\.0\\.0-sources\\.jar
                    /my-app/lib/standalone/jakarta\\.servlet-api-6\\.0\\.0\\.jar
                    /my-app/lib/standalone/jetty-ee10-servlet-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-ee10-servlet-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-http-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-http-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-io-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-io-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-security-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-security-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-server-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-server-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-session-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-session-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/jetty-util-12\\.0\\.12-sources\\.jar
                    /my-app/lib/standalone/jetty-util-12\\.0\\.12\\.jar
                    /my-app/lib/standalone/modules
                    /my-app/lib/standalone/slf4j-api-2\\.0\\.13-sources\\.jar
                    /my-app/lib/standalone/slf4j-api-2\\.0\\.13\\.jar
                    /my-app/lib/standalone/slf4j-simple-2\\.0\\.13-sources\\.jar
                    /my-app/lib/standalone/slf4j-simple-2\\.0\\.13\\.jar
                    /my-app/lib/test
                    /my-app/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                    /my-app/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                    /my-app/lib/test/jsoup-1\\.18\\.1-sources\\.jar
                    /my-app/lib/test/jsoup-1\\.18\\.1\\.jar
                    /my-app/lib/test/junit-jupiter-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-jupiter-api-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-api-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-jupiter-engine-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-engine-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-jupiter-params-5\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-jupiter-params-5\\.11\\.0\\.jar
                    /my-app/lib/test/junit-platform-commons-1\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-platform-commons-1\\.11\\.0\\.jar
                    /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.0\\.jar
                    /my-app/lib/test/junit-platform-engine-1\\.11\\.0-sources\\.jar
                    /my-app/lib/test/junit-platform-engine-1\\.11\\.0\\.jar
                    /my-app/lib/test/modules
                    /my-app/lib/test/opentest4j-1\\.3\\.0-sources\\.jar
                    /my-app/lib/test/opentest4j-1\\.3\\.0\\.jar
                    /my-app/src
                    /my-app/src/bld
                    /my-app/src/bld/java
                    /my-app/src/bld/java/com
                    /my-app/src/bld/java/com/example
                    /my-app/src/bld/java/com/example/MyAppBuild\\.java
                    /my-app/src/bld/resources
                    /my-app/src/main
                    /my-app/src/main/java
                    /my-app/src/main/java/com
                    /my-app/src/main/java/com/example
                    /my-app/src/main/java/com/example/MyAppSite\\.java
                    /my-app/src/main/java/com/example/MyAppSiteUber\\.java
                    /my-app/src/main/resources
                    /my-app/src/main/resources/templates
                    /my-app/src/main/resources/templates/hello\\.html
                    /my-app/src/main/webapp
                    /my-app/src/main/webapp/WEB-INF
                    /my-app/src/main/webapp/WEB-INF/web\\.xml
                    /my-app/src/main/webapp/css
                    /my-app/src/main/webapp/css/style\\.css
                    /my-app/src/test
                    /my-app/src/test/java
                    /my-app/src/test/java/com
                    /my-app/src/test/java/com/example
                    /my-app/src/test/java/com/example/MyAppTest\\.java
                    /my-app/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

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
            }, 2, TimeUnit.SECONDS);
            executor.schedule(() -> run_operation.process().destroy(), 4, TimeUnit.SECONDS);
            assertThrows(ExitStatusException.class, run_operation::execute);
            Thread.sleep(2000);

            assertTrue(check_result.toString().contains("<p>Hello World My-app</p>"));
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
                .projectName("your-thing")
                .baseName("YourThing");
            create_operation.execute();

            assertEquals("""
                    /your-thing
                    /your-thing/.gitignore
                    /your-thing/.idea
                    /your-thing/.idea/app.iml
                    /your-thing/.idea/bld.iml
                    /your-thing/.idea/libraries
                    /your-thing/.idea/libraries/bld.xml
                    /your-thing/.idea/libraries/compile.xml
                    /your-thing/.idea/libraries/runtime.xml
                    /your-thing/.idea/libraries/standalone.xml
                    /your-thing/.idea/libraries/test.xml
                    /your-thing/.idea/misc.xml
                    /your-thing/.idea/modules.xml
                    /your-thing/.idea/runConfigurations
                    /your-thing/.idea/runConfigurations/Run Main.xml
                    /your-thing/.idea/runConfigurations/Run Tests.xml
                    /your-thing/.vscode
                    /your-thing/.vscode/launch.json
                    /your-thing/.vscode/settings.json
                    /your-thing/bld
                    /your-thing/bld.bat
                    /your-thing/lib
                    /your-thing/lib/bld
                    /your-thing/lib/bld/bld-wrapper.jar
                    /your-thing/lib/bld/bld-wrapper.properties
                    /your-thing/lib/compile
                    /your-thing/lib/compile/modules
                    /your-thing/lib/provided
                    /your-thing/lib/provided/modules
                    /your-thing/lib/runtime
                    /your-thing/lib/runtime/modules
                    /your-thing/lib/standalone
                    /your-thing/lib/standalone/modules
                    /your-thing/lib/test
                    /your-thing/lib/test/modules
                    /your-thing/src
                    /your-thing/src/bld
                    /your-thing/src/bld/java
                    /your-thing/src/bld/java/org
                    /your-thing/src/bld/java/org/stuff
                    /your-thing/src/bld/java/org/stuff/YourThingBuild.java
                    /your-thing/src/bld/resources
                    /your-thing/src/main
                    /your-thing/src/main/java
                    /your-thing/src/main/java/org
                    /your-thing/src/main/java/org/stuff
                    /your-thing/src/main/java/org/stuff/YourThingSite.java
                    /your-thing/src/main/java/org/stuff/YourThingSiteUber.java
                    /your-thing/src/main/resources
                    /your-thing/src/main/resources/templates
                    /your-thing/src/main/resources/templates/hello.html
                    /your-thing/src/main/webapp
                    /your-thing/src/main/webapp/WEB-INF
                    /your-thing/src/main/webapp/WEB-INF/web.xml
                    /your-thing/src/main/webapp/css
                    /your-thing/src/main/webapp/css/style.css
                    /your-thing/src/test
                    /your-thing/src/test/java
                    /your-thing/src/test/java/org
                    /your-thing/src/test/java/org/stuff
                    /your-thing/src/test/java/org/stuff/YourThingTest.java
                    /your-thing/src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));

            var compile_operation = new CompileOperation() {
                public void executeProcessDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
                    // don't output errors
                }
            };
            compile_operation.fromProject(create_operation.project());
            assertThrows(ExitStatusException.class, compile_operation::execute);
            var diagnostics = compile_operation.diagnostics();
            assertFalse(diagnostics.isEmpty());
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
                .projectName("my-app")
                .baseName("MyApp")
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
                /my-app
                /my-app/\\.gitignore
                /my-app/\\.idea
                /my-app/\\.idea/app\\.iml
                /my-app/\\.idea/bld\\.iml
                /my-app/\\.idea/libraries
                /my-app/\\.idea/libraries/bld\\.xml
                /my-app/\\.idea/libraries/compile\\.xml
                /my-app/\\.idea/libraries/runtime\\.xml
                /my-app/\\.idea/libraries/standalone\\.xml
                /my-app/\\.idea/libraries/test\\.xml
                /my-app/\\.idea/misc\\.xml
                /my-app/\\.idea/modules\\.xml
                /my-app/\\.idea/runConfigurations
                /my-app/\\.idea/runConfigurations/Run Main\\.xml
                /my-app/\\.idea/runConfigurations/Run Tests\\.xml
                /my-app/\\.vscode
                /my-app/\\.vscode/launch\\.json
                /my-app/\\.vscode/settings\\.json
                /my-app/bld
                /my-app/bld\\.bat
                /my-app/build
                /my-app/build/main
                /my-app/build/main/com
                /my-app/build/main/com/example
                /my-app/build/main/com/example/MyAppSite\\.class
                /my-app/build/main/com/example/MyAppSiteUber\\.class
                /my-app/build/test
                /my-app/build/test/com
                /my-app/build/test/com/example
                /my-app/build/test/com/example/MyAppTest\\.class
                /my-app/lib
                /my-app/lib/bld
                /my-app/lib/bld/bld-wrapper\\.jar
                /my-app/lib/bld/bld-wrapper\\.properties
                /my-app/lib/compile
                /my-app/lib/compile/modules
                /my-app/lib/local
                /my-app/lib/local/apiguardian-api-1\\.1\\.2-sources\\.jar
                /my-app/lib/local/apiguardian-api-1\\.1\\.2\\.jar
                /my-app/lib/local/jakarta\\.servlet-api-6\\.0\\.0-sources\\.jar
                /my-app/lib/local/jakarta\\.servlet-api-6\\.0\\.0\\.jar
                /my-app/lib/local/jetty-ee10-servlet-12\\.0\\.12-sources\\.jar
                /my-app/lib/local/jetty-ee10-servlet-12\\.0\\.12\\.jar
                /my-app/lib/local/jetty-http-12\\.0\\.12-sources\\.jar
                /my-app/lib/local/jetty-http-12\\.0\\.12\\.jar
                /my-app/lib/local/jetty-io-12\\.0\\.12-sources\\.jar
                /my-app/lib/local/jetty-io-12\\.0\\.12\\.jar
                /my-app/lib/local/jetty-security-12\\.0\\.12-sources\\.jar
                /my-app/lib/local/jetty-security-12\\.0\\.12\\.jar
                /my-app/lib/local/jetty-server-12\\.0\\.12-sources\\.jar
                /my-app/lib/local/jetty-server-12\\.0\\.12\\.jar
                /my-app/lib/local/jetty-session-12\\.0\\.12-sources\\.jar
                /my-app/lib/local/jetty-session-12\\.0\\.12\\.jar
                /my-app/lib/local/jetty-util-12\\.0\\.12-sources\\.jar
                /my-app/lib/local/jetty-util-12\\.0\\.12\\.jar
                /my-app/lib/local/jsoup-1\\.18\\.1-sources\\.jar
                /my-app/lib/local/jsoup-1\\.18\\.1\\.jar
                /my-app/lib/local/junit-jupiter-5\\.11\\.0-sources\\.jar
                /my-app/lib/local/junit-jupiter-5\\.11\\.0\\.jar
                /my-app/lib/local/junit-jupiter-api-5\\.11\\.0-sources\\.jar
                /my-app/lib/local/junit-jupiter-api-5\\.11\\.0\\.jar
                /my-app/lib/local/junit-jupiter-engine-5\\.11\\.0-sources\\.jar
                /my-app/lib/local/junit-jupiter-engine-5\\.11\\.0\\.jar
                /my-app/lib/local/junit-jupiter-params-5\\.11\\.0-sources\\.jar
                /my-app/lib/local/junit-jupiter-params-5\\.11\\.0\\.jar
                /my-app/lib/local/junit-platform-commons-1\\.11\\.0-sources\\.jar
                /my-app/lib/local/junit-platform-commons-1\\.11\\.0\\.jar
                /my-app/lib/local/junit-platform-console-standalone-1\\.11\\.0-sources\\.jar
                /my-app/lib/local/junit-platform-console-standalone-1\\.11\\.0\\.jar
                /my-app/lib/local/junit-platform-engine-1\\.11\\.0-sources\\.jar
                /my-app/lib/local/junit-platform-engine-1\\.11\\.0\\.jar
                /my-app/lib/local/opentest4j-1\\.3\\.0-sources\\.jar
                /my-app/lib/local/opentest4j-1\\.3\\.0\\.jar
                /my-app/lib/local/rife2-.*-sources\\.jar
                /my-app/lib/local/rife2-.*\\.jar
                /my-app/lib/local/slf4j-api-2\\.0\\.13-sources\\.jar
                /my-app/lib/local/slf4j-api-2\\.0\\.13\\.jar
                /my-app/lib/local/slf4j-simple-2\\.0\\.13-sources\\.jar
                /my-app/lib/local/slf4j-simple-2\\.0\\.13\\.jar
                /my-app/lib/provided
                /my-app/lib/provided/modules
                /my-app/lib/runtime
                /my-app/lib/runtime/modules
                /my-app/lib/standalone
                /my-app/lib/standalone/modules
                /my-app/lib/test
                /my-app/lib/test/modules
                /my-app/src
                /my-app/src/bld
                /my-app/src/bld/java
                /my-app/src/bld/java/com
                /my-app/src/bld/java/com/example
                /my-app/src/bld/java/com/example/MyAppBuild\\.java
                /my-app/src/bld/resources
                /my-app/src/main
                /my-app/src/main/java
                /my-app/src/main/java/com
                /my-app/src/main/java/com/example
                /my-app/src/main/java/com/example/MyAppSite\\.java
                /my-app/src/main/java/com/example/MyAppSiteUber\\.java
                /my-app/src/main/resources
                /my-app/src/main/resources/templates
                /my-app/src/main/resources/templates/hello\\.html
                /my-app/src/main/webapp
                /my-app/src/main/webapp/WEB-INF
                /my-app/src/main/webapp/WEB-INF/web\\.xml
                /my-app/src/main/webapp/css
                /my-app/src/main/webapp/css/style\\.css
                /my-app/src/test
                /my-app/src/test/java
                /my-app/src/test/java/com
                /my-app/src/test/java/com/example
                /my-app/src/test/java/com/example/MyAppTest\\.java
                /my-app/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

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
            }, 2, TimeUnit.SECONDS);
            executor.schedule(() -> run_operation.process().destroy(), 4, TimeUnit.SECONDS);
            assertThrows(ExitStatusException.class, run_operation::execute);
            Thread.sleep(2000);

            assertTrue(check_result.toString().contains("<p>Hello World My-app</p>"), check_result.toString());
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
                .projectName("my-app")
                .baseName("MyApp")
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
                /my-app
                /my-app/\\.gitignore
                /my-app/\\.idea
                /my-app/\\.idea/app\\.iml
                /my-app/\\.idea/bld\\.iml
                /my-app/\\.idea/libraries
                /my-app/\\.idea/libraries/bld\\.xml
                /my-app/\\.idea/libraries/compile\\.xml
                /my-app/\\.idea/libraries/runtime\\.xml
                /my-app/\\.idea/libraries/standalone\\.xml
                /my-app/\\.idea/libraries/test\\.xml
                /my-app/\\.idea/misc\\.xml
                /my-app/\\.idea/modules\\.xml
                /my-app/\\.idea/runConfigurations
                /my-app/\\.idea/runConfigurations/Run Main\\.xml
                /my-app/\\.idea/runConfigurations/Run Tests\\.xml
                /my-app/\\.vscode
                /my-app/\\.vscode/launch\\.json
                /my-app/\\.vscode/settings\\.json
                /my-app/bld
                /my-app/bld\\.bat
                /my-app/build
                /my-app/build/main
                /my-app/build/main/com
                /my-app/build/main/com/example
                /my-app/build/main/com/example/MyAppSite\\.class
                /my-app/build/main/com/example/MyAppSiteUber\\.class
                /my-app/build/test
                /my-app/build/test/com
                /my-app/build/test/com/example
                /my-app/build/test/com/example/MyAppTest\\.class
                /my-app/lib
                /my-app/lib/bld
                /my-app/lib/bld/bld-wrapper\\.jar
                /my-app/lib/bld/bld-wrapper\\.properties
                /my-app/lib/compile
                /my-app/lib/compile/modules
                /my-app/lib/local_compile
                /my-app/lib/local_compile/rife2-.*-sources\\.jar
                /my-app/lib/local_compile/rife2-.*\\.jar
                /my-app/lib/local_standalone
                /my-app/lib/local_standalone/jakarta\\.servlet-api-6\\.0\\.0-sources\\.jar
                /my-app/lib/local_standalone/jakarta\\.servlet-api-6\\.0\\.0\\.jar
                /my-app/lib/local_standalone/jetty-ee10-servlet-12\\.0\\.12-sources\\.jar
                /my-app/lib/local_standalone/jetty-ee10-servlet-12\\.0\\.12\\.jar
                /my-app/lib/local_standalone/jetty-http-12\\.0\\.12-sources\\.jar
                /my-app/lib/local_standalone/jetty-http-12\\.0\\.12\\.jar
                /my-app/lib/local_standalone/jetty-io-12\\.0\\.12-sources\\.jar
                /my-app/lib/local_standalone/jetty-io-12\\.0\\.12\\.jar
                /my-app/lib/local_standalone/jetty-security-12\\.0\\.12-sources\\.jar
                /my-app/lib/local_standalone/jetty-security-12\\.0\\.12\\.jar
                /my-app/lib/local_standalone/jetty-server-12\\.0\\.12-sources\\.jar
                /my-app/lib/local_standalone/jetty-server-12\\.0\\.12\\.jar
                /my-app/lib/local_standalone/jetty-session-12\\.0\\.12-sources\\.jar
                /my-app/lib/local_standalone/jetty-session-12\\.0\\.12\\.jar
                /my-app/lib/local_standalone/jetty-util-12\\.0\\.12-sources\\.jar
                /my-app/lib/local_standalone/jetty-util-12\\.0\\.12\\.jar
                /my-app/lib/local_standalone/slf4j-api-2\\.0\\.13-sources\\.jar
                /my-app/lib/local_standalone/slf4j-api-2\\.0\\.13\\.jar
                /my-app/lib/local_standalone/slf4j-simple-2\\.0\\.13-sources\\.jar
                /my-app/lib/local_standalone/slf4j-simple-2\\.0\\.13\\.jar
                /my-app/lib/local_test
                /my-app/lib/local_test/apiguardian-api-1\\.1\\.2-sources\\.jar
                /my-app/lib/local_test/apiguardian-api-1\\.1\\.2\\.jar
                /my-app/lib/local_test/jsoup-1\\.18\\.1-sources\\.jar
                /my-app/lib/local_test/jsoup-1\\.18\\.1\\.jar
                /my-app/lib/local_test/junit-jupiter-5\\.11\\.0-sources\\.jar
                /my-app/lib/local_test/junit-jupiter-5\\.11\\.0\\.jar
                /my-app/lib/local_test/junit-jupiter-api-5\\.11\\.0-sources\\.jar
                /my-app/lib/local_test/junit-jupiter-api-5\\.11\\.0\\.jar
                /my-app/lib/local_test/junit-jupiter-engine-5\\.11\\.0-sources\\.jar
                /my-app/lib/local_test/junit-jupiter-engine-5\\.11\\.0\\.jar
                /my-app/lib/local_test/junit-jupiter-params-5\\.11\\.0-sources\\.jar
                /my-app/lib/local_test/junit-jupiter-params-5\\.11\\.0\\.jar
                /my-app/lib/local_test/junit-platform-commons-1\\.11\\.0-sources\\.jar
                /my-app/lib/local_test/junit-platform-commons-1\\.11\\.0\\.jar
                /my-app/lib/local_test/junit-platform-console-standalone-1\\.11\\.0-sources\\.jar
                /my-app/lib/local_test/junit-platform-console-standalone-1\\.11\\.0\\.jar
                /my-app/lib/local_test/junit-platform-engine-1\\.11\\.0-sources\\.jar
                /my-app/lib/local_test/junit-platform-engine-1\\.11\\.0\\.jar
                /my-app/lib/local_test/opentest4j-1\\.3\\.0-sources\\.jar
                /my-app/lib/local_test/opentest4j-1\\.3\\.0\\.jar
                /my-app/lib/provided
                /my-app/lib/provided/modules
                /my-app/lib/runtime
                /my-app/lib/runtime/modules
                /my-app/lib/standalone
                /my-app/lib/standalone/modules
                /my-app/lib/test
                /my-app/lib/test/modules
                /my-app/src
                /my-app/src/bld
                /my-app/src/bld/java
                /my-app/src/bld/java/com
                /my-app/src/bld/java/com/example
                /my-app/src/bld/java/com/example/MyAppBuild\\.java
                /my-app/src/bld/resources
                /my-app/src/main
                /my-app/src/main/java
                /my-app/src/main/java/com
                /my-app/src/main/java/com/example
                /my-app/src/main/java/com/example/MyAppSite\\.java
                /my-app/src/main/java/com/example/MyAppSiteUber\\.java
                /my-app/src/main/resources
                /my-app/src/main/resources/templates
                /my-app/src/main/resources/templates/hello\\.html
                /my-app/src/main/webapp
                /my-app/src/main/webapp/WEB-INF
                /my-app/src/main/webapp/WEB-INF/web\\.xml
                /my-app/src/main/webapp/css
                /my-app/src/main/webapp/css/style\\.css
                /my-app/src/test
                /my-app/src/test/java
                /my-app/src/test/java/com
                /my-app/src/test/java/com/example
                /my-app/src/test/java/com/example/MyAppTest\\.java
                /my-app/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

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
            }, 2, TimeUnit.SECONDS);
            executor.schedule(() -> run_operation.process().destroy(), 4, TimeUnit.SECONDS);
            assertThrows(ExitStatusException.class, run_operation::execute);
            Thread.sleep(2000);

            assertTrue(check_result.toString().contains("<p>Hello World My-app</p>"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
