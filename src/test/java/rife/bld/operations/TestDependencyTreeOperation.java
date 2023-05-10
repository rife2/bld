/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.WebProject;
import rife.bld.dependencies.*;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDependencyTreeOperation {
    @Test
    void testInstantiation() {
        var operation = new DependencyTreeOperation();
        assertTrue(operation.dependencies().isEmpty());
        assertTrue(operation.repositories().isEmpty());
    }

    @Test
    void testPopulation() {
        var repository1 = new Repository("repository1");
        var repository2 = new Repository("repository2");
        var dependency1 = new Dependency("group1", "artifact1");
        var dependency2 = new Dependency("group2", "artifact2");

        var operation1 = new DependencyTreeOperation()
            .repositories(List.of(repository1, repository2));
        var dependency_scopes = new DependencyScopes();
        dependency_scopes.scope(Scope.compile).include(dependency1).include(dependency2);
        operation1.dependencies(dependency_scopes);
        assertTrue(operation1.repositories().contains(repository1));
        assertTrue(operation1.repositories().contains(repository2));
        assertTrue(operation1.dependencies().scope(Scope.compile).contains(dependency1));
        assertTrue(operation1.dependencies().scope(Scope.compile).contains(dependency2));

        var operation2 = new DependencyTreeOperation();
        operation2.repositories().add(repository1);
        operation2.repositories().add(repository2);
        operation2.dependencies().scope(Scope.compile).include(dependency1).include(dependency2);
        operation2.dependencies(dependency_scopes);
        assertTrue(operation2.repositories().contains(repository1));
        assertTrue(operation2.repositories().contains(repository2));
        assertTrue(operation2.dependencies().scope(Scope.compile).contains(dependency1));
        assertTrue(operation2.dependencies().scope(Scope.compile).contains(dependency2));

        var operation3 = new DependencyTreeOperation()
            .repositories(repository1, repository2);
        assertTrue(operation3.repositories().contains(repository1));
        assertTrue(operation3.repositories().contains(repository2));
    }

    @Test
    void testExecution()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var operation = new DependencyTreeOperation()
                .repositories(List.of(Repository.MAVEN_CENTRAL));
            operation.dependencies().scope(Scope.compile)
                .include(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,5,20)))
                .include(new Dependency("com.stripe", "stripe-java", new VersionNumber(20,136,0)))
                .include(new Dependency("org.json", "json", new VersionNumber(20230227)))
                .include(new Dependency("com.itextpdf", "itext7-core", new VersionNumber(7,2,5)))
                .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(2,0,7)))
                .include(new Dependency("org.apache.thrift", "libthrift", new VersionNumber(0,17,0)))
                .include(new Dependency("commons-codec", "commons-codec", new VersionNumber(1,15)))
                .include(new Dependency("org.apache.httpcomponents", "httpcore", new VersionNumber(4,4,16)))
                .include(new Dependency("com.google.zxing", "javase", new VersionNumber(3,5,1)));
            operation.dependencies().scope(Scope.runtime)
                .include(new Dependency("org.postgresql", "postgresql", new VersionNumber(42,6,0)));

            operation.execute();

            var tree = operation.dependencyTree();

            assertEquals("""
                compile:
                ├─ com.uwyn.rife2:rife2:1.5.20
                ├─ com.stripe:stripe-java:20.136.0
                ├─ org.json:json:20230227
                ├─ com.itextpdf:itext7-core:7.2.5
                │  ├─ com.itextpdf:barcodes:7.2.5
                │  ├─ com.itextpdf:font-asian:7.2.5
                │  ├─ com.itextpdf:forms:7.2.5
                │  ├─ com.itextpdf:hyph:7.2.5
                │  ├─ com.itextpdf:io:7.2.5
                │  │  └─ com.itextpdf:commons:7.2.5
                │  ├─ com.itextpdf:kernel:7.2.5
                │  │  ├─ org.bouncycastle:bcpkix-jdk15on:1.70
                │  │  │  └─ org.bouncycastle:bcutil-jdk15on:1.70
                │  │  └─ org.bouncycastle:bcprov-jdk15on:1.70
                │  ├─ com.itextpdf:layout:7.2.5
                │  ├─ com.itextpdf:pdfa:7.2.5
                │  ├─ com.itextpdf:sign:7.2.5
                │  ├─ com.itextpdf:styled-xml-parser:7.2.5
                │  └─ com.itextpdf:svg:7.2.5
                ├─ org.slf4j:slf4j-simple:2.0.7
                │  └─ org.slf4j:slf4j-api:2.0.7
                ├─ org.apache.thrift:libthrift:0.17.0
                ├─ commons-codec:commons-codec:1.15
                ├─ org.apache.httpcomponents:httpcore:4.4.16
                └─ com.google.zxing:javase:3.5.1
                   ├─ com.google.zxing:core:3.5.1
                   └─ com.beust:jcommander:1.82
                                
                runtime:
                └─ org.postgresql:postgresql:42.6.0
                   └─ org.checkerframework:checker-qual:3.31.0
                                
                """, tree);
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class TestProject extends WebProject {
        public TestProject(File tmp) {
            workDirectory = tmp;
            pkg = "test.pkg";
        }
    }

    @Test
    void testFromProject()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var project = new TestProject(tmp);
            project.createProjectStructure();
            project.repositories().add(Repository.MAVEN_CENTRAL);
            project.dependencies().scope(Scope.compile)
                .include(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,5,20)))
                .include(new Dependency("com.stripe", "stripe-java", new VersionNumber(20,136,0)))
                .include(new Dependency("org.json", "json", new VersionNumber(20230227)))
                .include(new Dependency("com.itextpdf", "itext7-core", new VersionNumber(7,2,5)))
                .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(2,0,7)))
                .include(new Dependency("org.apache.thrift", "libthrift", new VersionNumber(0,17,0)))
                .include(new Dependency("commons-codec", "commons-codec", new VersionNumber(1,15)))
                .include(new Dependency("org.apache.httpcomponents", "httpcore", new VersionNumber(4,4,16)))
                .include(new Dependency("com.google.zxing", "javase", new VersionNumber(3,5,1)));
            project.dependencies().scope(Scope.runtime)
                .include(new Dependency("org.postgresql", "postgresql", new VersionNumber(42,6,0)));

            var operation = new DependencyTreeOperation()
                .fromProject(project);

            operation.execute();

            var tree = operation.dependencyTree();

            assertEquals("""
                compile:
                ├─ com.uwyn.rife2:rife2:1.5.20
                ├─ com.stripe:stripe-java:20.136.0
                ├─ org.json:json:20230227
                ├─ com.itextpdf:itext7-core:7.2.5
                │  ├─ com.itextpdf:barcodes:7.2.5
                │  ├─ com.itextpdf:font-asian:7.2.5
                │  ├─ com.itextpdf:forms:7.2.5
                │  ├─ com.itextpdf:hyph:7.2.5
                │  ├─ com.itextpdf:io:7.2.5
                │  │  └─ com.itextpdf:commons:7.2.5
                │  ├─ com.itextpdf:kernel:7.2.5
                │  │  ├─ org.bouncycastle:bcpkix-jdk15on:1.70
                │  │  │  └─ org.bouncycastle:bcutil-jdk15on:1.70
                │  │  └─ org.bouncycastle:bcprov-jdk15on:1.70
                │  ├─ com.itextpdf:layout:7.2.5
                │  ├─ com.itextpdf:pdfa:7.2.5
                │  ├─ com.itextpdf:sign:7.2.5
                │  ├─ com.itextpdf:styled-xml-parser:7.2.5
                │  └─ com.itextpdf:svg:7.2.5
                ├─ org.slf4j:slf4j-simple:2.0.7
                │  └─ org.slf4j:slf4j-api:2.0.7
                ├─ org.apache.thrift:libthrift:0.17.0
                ├─ commons-codec:commons-codec:1.15
                ├─ org.apache.httpcomponents:httpcore:4.4.16
                └─ com.google.zxing:javase:3.5.1
                   ├─ com.google.zxing:core:3.5.1
                   └─ com.beust:jcommander:1.82
                                
                runtime:
                └─ org.postgresql:postgresql:42.6.0
                   └─ org.checkerframework:checker-qual:3.31.0
                                
                """, tree);
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }
}
