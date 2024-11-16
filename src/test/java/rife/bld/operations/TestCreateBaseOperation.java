/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.tools.FileUtils;

import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TestCreateBaseOperation {
    @Test
    void testInstantiation() {
        var operation = new CreateBaseOperation();
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

            var operation = new CreateBaseOperation();
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
            var create_operation = new CreateBaseOperation()
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
                    /my-app/lib/provided
                    /my-app/lib/provided/modules
                    /my-app/lib/runtime
                    /my-app/lib/runtime/modules
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
                    /my-app/src/main/java/com/example/MyAppMain\\.java
                    /my-app/src/main/resources
                    /my-app/src/main/resources/templates
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
                    /my-app/build/main/com/example/MyAppMain\\.class
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
                    /my-app/lib/provided
                    /my-app/lib/provided/modules
                    /my-app/lib/runtime
                    /my-app/lib/runtime/modules
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
                    /my-app/src/main/java/com/example/MyAppMain\\.java
                    /my-app/src/main/resources
                    /my-app/src/main/resources/templates
                    /my-app/src/test
                    /my-app/src/test/java
                    /my-app/src/test/java/com
                    /my-app/src/test/java/com/example
                    /my-app/src/test/java/com/example/MyAppTest\\.java
                    /my-app/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

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
            var create_operation = new CreateBaseOperation()
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
                    /your-thing/src/main/java/org/stuff/YourThingMain.java
                    /your-thing/src/main/resources
                    /your-thing/src/main/resources/templates
                    /your-thing/src/test
                    /your-thing/src/test/java
                    /your-thing/src/test/java/org
                    /your-thing/src/test/java/org/stuff
                    /your-thing/src/test/java/org/stuff/YourThingTest.java
                    /your-thing/src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));

            var compile_operation = new CompileOperation().fromProject(create_operation.project());
            compile_operation.execute();
            assertTrue(compile_operation.diagnostics().isEmpty());
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
                    /your-thing/build
                    /your-thing/build/main
                    /your-thing/build/main/org
                    /your-thing/build/main/org/stuff
                    /your-thing/build/main/org/stuff/YourThingMain.class
                    /your-thing/build/test
                    /your-thing/build/test/org
                    /your-thing/build/test/org/stuff
                    /your-thing/build/test/org/stuff/YourThingTest.class
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
                    /your-thing/src/main/java/org/stuff/YourThingMain.java
                    /your-thing/src/main/resources
                    /your-thing/src/main/resources/templates
                    /your-thing/src/test
                    /your-thing/src/test/java
                    /your-thing/src/test/java/org
                    /your-thing/src/test/java/org/stuff
                    /your-thing/src/test/java/org/stuff/YourThingTest.java
                    /your-thing/src/test/resources""",
                FileUtils.generateDirectoryListing(tmp));

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
