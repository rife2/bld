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
                /my-app/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                /my-app/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                /my-app/lib/test/junit-jupiter-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-jupiter-api-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-api-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-jupiter-engine-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-engine-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-jupiter-params-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-params-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-platform-commons-1\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-platform-commons-1\\.11\\.4\\.jar
                /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.4\\.jar
                /my-app/lib/test/junit-platform-engine-1\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-platform-engine-1\\.11\\.4\\.jar
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
                /my-app/src/main/java/com/example/MyApp\\.java
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
                /my-app/build/main/com/example/MyApp\\.class
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
                /my-app/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                /my-app/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                /my-app/lib/test/junit-jupiter-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-jupiter-api-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-api-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-jupiter-engine-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-engine-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-jupiter-params-5\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-jupiter-params-5\\.11\\.4\\.jar
                /my-app/lib/test/junit-platform-commons-1\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-platform-commons-1\\.11\\.4\\.jar
                /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-platform-console-standalone-1\\.11\\.4\\.jar
                /my-app/lib/test/junit-platform-engine-1\\.11\\.4-sources\\.jar
                /my-app/lib/test/junit-platform-engine-1\\.11\\.4\\.jar
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
                /my-app/src/main/java/com/example/MyApp\\.java
                /my-app/src/main/resources
                /my-app/src/main/resources/templates
                /my-app/src/test
                /my-app/src/test/java
                /my-app/src/test/java/com
                /my-app/src/test/java/com/example
                /my-app/src/test/java/com/example/MyAppTest\\.java
                /my-app/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

            var check_result = new StringBuilder();
            new JUnitOperation()
                .fromProject(create_operation.project())
                .outputProcessor(s -> {
                    check_result.append(s);
                    return true;
                })
                .testToolOptions("--details=summary")
                .execute();
            assertTrue(check_result.toString().contains("1 tests successful"));
            assertTrue(check_result.toString().contains("0 tests failed"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
