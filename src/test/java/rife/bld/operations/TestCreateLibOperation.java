/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.LocalDependency;
import rife.bld.dependencies.Scope;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TestCreateLibOperation {
    @Test
    void testInstantiation() {
        var operation = new CreateLibOperation();
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

            var operation = new CreateLibOperation();
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
            var create_operation = new CreateLibOperation()
                .workDirectory(tmp)
                .packageName("com.example")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation.execute();

            assertEquals("""
                /myapp
                /myapp/.gitignore
                /myapp/.idea
                /myapp/.idea/app.iml
                /myapp/.idea/bld.iml
                /myapp/.idea/libraries
                /myapp/.idea/libraries/bld.xml
                /myapp/.idea/libraries/compile.xml
                /myapp/.idea/libraries/runtime.xml
                /myapp/.idea/libraries/test.xml
                /myapp/.idea/misc.xml
                /myapp/.idea/modules.xml
                /myapp/.idea/runConfigurations
                /myapp/.idea/runConfigurations/Run Tests.xml
                /myapp/.vscode
                /myapp/.vscode/launch.json
                /myapp/.vscode/settings.json
                /myapp/bld
                /myapp/bld.bat
                /myapp/lib
                /myapp/lib/bld
                /myapp/lib/bld/bld-wrapper.jar
                /myapp/lib/bld/bld-wrapper.properties
                /myapp/lib/compile
                /myapp/lib/runtime
                /myapp/lib/test
                /myapp/src
                /myapp/src/bld
                /myapp/src/bld/java
                /myapp/src/bld/java/com
                /myapp/src/bld/java/com/example
                /myapp/src/bld/java/com/example/MyappBuild.java
                /myapp/src/bld/resources
                /myapp/src/main
                /myapp/src/main/java
                /myapp/src/main/java/com
                /myapp/src/main/java/com/example
                /myapp/src/main/java/com/example/MyappLib.java
                /myapp/src/main/resources
                /myapp/src/main/resources/templates
                /myapp/src/test
                /myapp/src/test/java
                /myapp/src/test/java/com
                /myapp/src/test/java/com/example
                /myapp/src/test/java/com/example/MyappTest.java
                /myapp/src/test/resources""", FileUtils.generateDirectoryListing(tmp));

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
            assertEquals("""
                /myapp
                /myapp/.gitignore
                /myapp/.idea
                /myapp/.idea/app.iml
                /myapp/.idea/bld.iml
                /myapp/.idea/libraries
                /myapp/.idea/libraries/bld.xml
                /myapp/.idea/libraries/compile.xml
                /myapp/.idea/libraries/runtime.xml
                /myapp/.idea/libraries/test.xml
                /myapp/.idea/misc.xml
                /myapp/.idea/modules.xml
                /myapp/.idea/runConfigurations
                /myapp/.idea/runConfigurations/Run Tests.xml
                /myapp/.vscode
                /myapp/.vscode/launch.json
                /myapp/.vscode/settings.json
                /myapp/bld
                /myapp/bld.bat
                /myapp/build
                /myapp/build/main
                /myapp/build/main/com
                /myapp/build/main/com/example
                /myapp/build/main/com/example/MyappLib.class
                /myapp/build/test
                /myapp/build/test/com
                /myapp/build/test/com/example
                /myapp/build/test/com/example/MyappTest.class
                /myapp/lib
                /myapp/lib/bld
                /myapp/lib/bld/bld-wrapper.jar
                /myapp/lib/bld/bld-wrapper.properties
                /myapp/lib/compile
                /myapp/lib/runtime
                /myapp/lib/test
                /myapp/src
                /myapp/src/bld
                /myapp/src/bld/java
                /myapp/src/bld/java/com
                /myapp/src/bld/java/com/example
                /myapp/src/bld/java/com/example/MyappBuild.java
                /myapp/src/bld/resources
                /myapp/src/main
                /myapp/src/main/java
                /myapp/src/main/java/com
                /myapp/src/main/java/com/example
                /myapp/src/main/java/com/example/MyappLib.java
                /myapp/src/main/resources
                /myapp/src/main/resources/templates
                /myapp/src/test
                /myapp/src/test/java
                /myapp/src/test/java/com
                /myapp/src/test/java/com/example
                /myapp/src/test/java/com/example/MyappTest.java
                /myapp/src/test/resources""", FileUtils.generateDirectoryListing(tmp));

            var check_result = new StringBuilder();
            new TestOperation<>()
                .fromProject(create_operation.project())
                .outputProcessor(s -> {
                    check_result.append(s);
                    return true;
                })
                .mainClass("com.example.MyappTest")
                .execute();
            assertEquals("Succeeded", check_result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExecuteNoDownload()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var create_operation = new CreateLibOperation()
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
                    /yourthing/src/main/java/org/stuff/YourthingLib.java
                    /yourthing/src/main/resources
                    /yourthing/src/main/resources/templates
                    /yourthing/src/test
                    /yourthing/src/test/java
                    /yourthing/src/test/java/org
                    /yourthing/src/test/java/org/stuff
                    /yourthing/src/test/java/org/stuff/YourthingTest.java
                    /yourthing/src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
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
                    /yourthing/.idea/runConfigurations/Run Tests.xml
                    /yourthing/.vscode
                    /yourthing/.vscode/launch.json
                    /yourthing/.vscode/settings.json
                    /yourthing/bld
                    /yourthing/bld.bat
                    /yourthing/build
                    /yourthing/build/main
                    /yourthing/build/main/org
                    /yourthing/build/main/org/stuff
                    /yourthing/build/main/org/stuff/YourthingLib.class
                    /yourthing/build/test
                    /yourthing/build/test/org
                    /yourthing/build/test/org/stuff
                    /yourthing/build/test/org/stuff/YourthingTest.class
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
                    /yourthing/src/main/java/org/stuff/YourthingLib.java
                    /yourthing/src/main/resources
                    /yourthing/src/main/resources/templates
                    /yourthing/src/test
                    /yourthing/src/test/java
                    /yourthing/src/test/java/org
                    /yourthing/src/test/java/org/stuff
                    /yourthing/src/test/java/org/stuff/YourthingTest.java
                    /yourthing/src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));

            var check_result = new StringBuilder();
            new TestOperation<>()
                .fromProject(create_operation.project())
                .outputProcessor(s -> {
                    check_result.append(s);
                    return true;
                })
                .mainClass("org.stuff.YourthingTest")
                .execute();
            assertEquals("Succeeded", check_result.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
