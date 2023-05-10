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

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TestCreateBlankOperation {
    @Test
    void testInstantiation() {
        var operation = new CreateBlankOperation();
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

            var operation = new CreateBlankOperation();
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
            var create_operation = new CreateBlankOperation()
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
                    /myapp/lib/runtime
                    /myapp/lib/test
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2\\.jar
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
                    /myapp/src/main/java/com/example/MyappMain\\.java
                    /myapp/src/main/resources
                    /myapp/src/main/resources/templates
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
                    /myapp/build/main/com/example/MyappMain\\.class
                    /myapp/build/test
                    /myapp/build/test/com
                    /myapp/build/test/com/example
                    /myapp/build/test/com/example/MyappTest\\.class
                    /myapp/lib
                    /myapp/lib/bld
                    /myapp/lib/bld/bld-wrapper\\.jar
                    /myapp/lib/bld/bld-wrapper\\.properties
                    /myapp/lib/compile
                    /myapp/lib/runtime
                    /myapp/lib/test
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                    /myapp/lib/test/apiguardian-api-1\\.1\\.2\\.jar
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
                    /myapp/src/main/java/com/example/MyappMain\\.java
                    /myapp/src/main/resources
                    /myapp/src/main/resources/templates
                    /myapp/src/test
                    /myapp/src/test/java
                    /myapp/src/test/java/com
                    /myapp/src/test/java/com/example
                    /myapp/src/test/java/com/example/MyappTest\\.java
                    /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var check_result = new StringBuilder();
            new RunOperation()
                .fromProject(create_operation.project())
                .outputProcessor(s -> {
                    check_result.append(s);
                    return true;
                })
                .execute();
            assertEquals("Hello World!", check_result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteNoDownload()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateBlankOperation()
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
                    /yourthing/src/main/java/org/stuff/YourthingMain.java
                    /yourthing/src/main/resources
                    /yourthing/src/main/resources/templates
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
            assertEquals(4, diagnostics.size());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteLocalDependencies()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateBlankOperation()
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
            for (var lib : FileUtils.getFileList(project.libTestDirectory())) {
                if (!lib.endsWith("-sources.jar")) {
                    project.dependencies().scope(Scope.test).include(new LocalDependency(Path.of("lib", "local", lib).toString()));
                }
                new File(project.libTestDirectory(), lib).renameTo(new File(lib_local, lib));
            }

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
            System.out.println(FileUtils.generateDirectoryListing(tmp));
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
                /myapp/build/main/com/example/MyappMain\\.class
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
                /myapp/lib/runtime
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
                /myapp/src/main/java/com/example/MyappMain\\.java
                /myapp/src/main/resources
                /myapp/src/main/resources/templates
                /myapp/src/test
                /myapp/src/test/java
                /myapp/src/test/java/com
                /myapp/src/test/java/com/example
                /myapp/src/test/java/com/example/MyappTest\\.java
                /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var check_result = new StringBuilder();
            new RunOperation()
                .fromProject(create_operation.project())
                .outputProcessor(s -> {
                    check_result.append(s);
                    return true;
                })
                .execute();
            assertEquals("Hello World!", check_result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteLocalDependenciesFolders()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateBlankOperation()
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
                /myapp/build/main/com/example/MyappMain\\.class
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
                /myapp/lib/local_test
                /myapp/lib/local_test/apiguardian-api-1\\.1\\.2-sources\\.jar
                /myapp/lib/local_test/apiguardian-api-1\\.1\\.2\\.jar
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
                /myapp/src/main/java/com/example/MyappMain\\.java
                /myapp/src/main/resources
                /myapp/src/main/resources/templates
                /myapp/src/test
                /myapp/src/test/java
                /myapp/src/test/java/com
                /myapp/src/test/java/com/example
                /myapp/src/test/java/com/example/MyappTest\\.java
                /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var check_result = new StringBuilder();
            new RunOperation()
                .fromProject(create_operation.project())
                .outputProcessor(s -> {
                    check_result.append(s);
                    return true;
                })
                .execute();
            assertEquals("Hello World!", check_result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
