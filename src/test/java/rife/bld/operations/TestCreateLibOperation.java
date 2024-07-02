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
                /myapp/lib/provided
                /myapp/lib/runtime
                /myapp/lib/test
                /myapp/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                /myapp/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                /myapp/lib/test/junit-jupiter-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-jupiter-api-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-api-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-jupiter-engine-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-engine-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-jupiter-params-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-params-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-platform-commons-1\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-platform-commons-1\\.10\\.3\\.jar
                /myapp/lib/test/junit-platform-console-standalone-1\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-platform-console-standalone-1\\.10\\.3\\.jar
                /myapp/lib/test/junit-platform-engine-1\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-platform-engine-1\\.10\\.3\\.jar
                /myapp/lib/test/opentest4j-1\\.3\\.0-sources\\.jar
                /myapp/lib/test/opentest4j-1\\.3\\.0\\.jar
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
                /myapp/src/main/java/com/example/MyappLib\\.java
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
                /myapp/build/main/com/example/MyappLib\\.class
                /myapp/build/test
                /myapp/build/test/com
                /myapp/build/test/com/example
                /myapp/build/test/com/example/MyappTest\\.class
                /myapp/lib
                /myapp/lib/bld
                /myapp/lib/bld/bld-wrapper\\.jar
                /myapp/lib/bld/bld-wrapper\\.properties
                /myapp/lib/compile
                /myapp/lib/provided
                /myapp/lib/runtime
                /myapp/lib/test
                /myapp/lib/test/apiguardian-api-1\\.1\\.2-sources\\.jar
                /myapp/lib/test/apiguardian-api-1\\.1\\.2\\.jar
                /myapp/lib/test/junit-jupiter-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-jupiter-api-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-api-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-jupiter-engine-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-engine-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-jupiter-params-5\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-jupiter-params-5\\.10\\.3\\.jar
                /myapp/lib/test/junit-platform-commons-1\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-platform-commons-1\\.10\\.3\\.jar
                /myapp/lib/test/junit-platform-console-standalone-1\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-platform-console-standalone-1\\.10\\.3\\.jar
                /myapp/lib/test/junit-platform-engine-1\\.10\\.3-sources\\.jar
                /myapp/lib/test/junit-platform-engine-1\\.10\\.3\\.jar
                /myapp/lib/test/opentest4j-1\\.3\\.0-sources\\.jar
                /myapp/lib/test/opentest4j-1\\.3\\.0\\.jar
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
                /myapp/src/main/java/com/example/MyappLib\\.java
                /myapp/src/main/resources
                /myapp/src/main/resources/templates
                /myapp/src/test
                /myapp/src/test/java
                /myapp/src/test/java/com
                /myapp/src/test/java/com/example
                /myapp/src/test/java/com/example/MyappTest\\.java
                /myapp/src/test/resources""").matcher(FileUtils.generateDirectoryListing(tmp)).matches());

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
