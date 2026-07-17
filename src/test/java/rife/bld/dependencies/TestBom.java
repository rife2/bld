/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import rife.bld.BldCache;
import rife.bld.dependencies.exceptions.ArtifactNotFoundException;
import rife.ioc.HierarchicalProperties;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.RepositoryTestHelper.*;
import static rife.bld.dependencies.Scope.compile;

public class TestBom {
    @Test
    void testParse() {
        assertEquals(new Bom("com.example", "bom1"), Bom.parse("com.example:bom1"));
        assertEquals(new Bom("com.example", "bom1", new VersionNumber(1, 2, 3)), Bom.parse("com.example:bom1:1.2.3"));
        assertEquals(new Bom("com.example", "bom1", new VersionNumber(1, 2, 3)), Bom.parse("com.example:bom1:1.2.3@bom"));
        assertEquals(new Bom("com.example", "bom1", new VersionNumber(1, 2, 3)), Bom.parse("com.example:bom1:1.2.3@pom"));
        // BOMs can't have classifiers, POM imports don't support them
        assertNull(Bom.parse("com.example:bom1:1.2.3:classifier1"));
        assertNull(Bom.parse(null));
        assertNull(Bom.parse(""));
        assertNull(Bom.parse("not@a@bom"));
    }

    @Test
    void testBomFillsDeclaredVersion() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
            "a:1.4.0", pom("a", "1.4.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("1.4.0"), resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomPinsTransitiveDependency() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("b", "1.5.0")),
            "a:1.0.0", pom("a", "1.0.0", dependency("b", "2.0.0")),
            "b:1.5.0", pom("b", "1.5.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("1.5.0"), resolved.get(new Dependency("com.example", "b")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testExplicitDeclaredVersionWinsOverBom() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
            "a:3.0.0", pom("a", "3.0.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(3, 0, 0)));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("3.0.0"), resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testVersionOverridePropertyWinsOverBomForTransitive() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("b", "1.5.0")),
            "a:1.0.0", pom("a", "1.0.0", dependency("b", "2.0.0")),
            "b:2.5.0", pom("b", "2.5.0", "")));
        server.start();
        try {
            var properties = new HierarchicalProperties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:b:2.5.0");

            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            var resolved = scopes.resolveCompileDependencies(properties, ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("2.5.0"), resolved.get(new Dependency("com.example", "b")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testVersionOverridePropertyWinsOverBomForDeclared() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
            "a:2.5.0", pom("a", "2.5.0", "")));
        server.start();
        try {
            var properties = new HierarchicalProperties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:a:2.5.0");

            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var resolved = scopes.resolveCompileDependencies(properties, ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("2.5.0"), resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testVersionOverridePropertyAppliesToBomItself() throws Exception {
        // only version 2.0.0 of the BOM is served, resolution succeeds
        // solely when the override rewrites the declared BOM version
        var server = createArtifactServer(Map.of(
            "bom1:2.0.0", bomPom("bom1", "2.0.0", managed("a", "1.4.0")),
            "a:1.4.0", pom("a", "1.4.0", "")));
        server.start();
        try {
            var properties = new HierarchicalProperties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:bom1:2.0.0");

            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var resolved = scopes.resolveCompileDependencies(properties, ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("1.4.0"), resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testFirstBomWinsOnConflict() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("c", "1.0.0")),
            "bom2:1.0.0", bomPom("bom2", "1.0.0", managed("c", "2.0.0")),
            "c:1.0.0", pom("c", "1.0.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Bom("com.example", "bom2", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "c"));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("1.0.0"), resolved.get(new Dependency("com.example", "c")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testNestedBomImport() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0",
                managed("d", "1.0.0") +
                "<dependency><groupId>com.example</groupId><artifactId>bom2</artifactId><version>1.0.0</version><type>pom</type><scope>import</scope></dependency>"),
            "bom2:1.0.0", bomPom("bom2", "1.0.0", managed("d", "9.9.9") + managed("e", "2.0.0")),
            "d:1.0.0", pom("d", "1.0.0", ""),
            "e:2.0.0", pom("e", "2.0.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "d"))
                .include(new Dependency("com.example", "e"));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            // the importing BOM's own entry wins over the imported entry
            assertEquals(Version.parse("1.0.0"), resolved.get(new Dependency("com.example", "d")).version());
            // entries that are only imported apply as well
            assertEquals(Version.parse("2.0.0"), resolved.get(new Dependency("com.example", "e")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomParentPomInheritance() throws Exception {
        var server = createArtifactServer(Map.of(
            "bomparent:1.0.0", bomPom("bomparent", "1.0.0", managed("a", "1.4.0") + managed("b", "9.9.9")),
            "bom1:1.0.0", bomPomWithParent("bom1", "1.0.0", "bomparent", managed("b", "1.5.0")),
            "a:1.4.0", pom("a", "1.4.0", ""),
            "b:1.5.0", pom("b", "1.5.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"))
                .include(new Dependency("com.example", "b"));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            // entries inherited from the BOM's parent POM apply
            assertEquals(Version.parse("1.4.0"), resolved.get(new Dependency("com.example", "a")).version());
            // the BOM's own entry wins over its parent's entry
            assertEquals(Version.parse("1.5.0"), resolved.get(new Dependency("com.example", "b")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomPropertyPlaceholders() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bom1</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <properties>
                        <dep.version>1.4.0</dep.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>${dep.version}</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""",
            "a:1.4.0", pom("a", "1.4.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("1.4.0"), resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testUncoveredVersionlessStillResolvesLatest() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
                "a:1.4.0", pom("a", "1.4.0", ""),
                "f:2.2.0", pom("f", "2.2.0", "")),
            Map.of("f", metadata("f", "2.2.0", "1.0.0", "2.2.0")));
        server.start();
        try {
            var retriever = ArtifactRetriever.cachingInstance();
            var repositories = serverRepositories(server);
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"))
                .include(new Dependency("com.example", "f"));
            scopes.scope(Scope.standalone)
                // no BOMs apply to this scope : it's not reported on
                .include(new Dependency("com.example", "f"));

            // only the dependency that the BOM doesn't cover is reported
            assertEquals(List.of(new Dependency("com.example", "f")),
                scopes.versionlessDependenciesWithoutBom(new HierarchicalProperties(), retriever, repositories));

            // a bld.override that supplies the version prevents the report
            var override_properties = new HierarchicalProperties();
            override_properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:f:2.2.0");
            assertEquals(List.of(),
                scopes.versionlessDependenciesWithoutBom(override_properties, retriever, repositories));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), retriever, repositories);

            // covered by the BOM : the BOM version applies
            assertEquals(Version.parse("1.4.0"), resolved.get(new Dependency("com.example", "a")).version());
            // not covered by the BOM : the version stays unknown in the set,
            // exactly as it would without any BOM declared
            assertEquals(VersionNumber.UNKNOWN, resolved.get(new Dependency("com.example", "f")).version());
            // and it still resolves to the latest version from the metadata
            var resolution = new VersionResolution(new HierarchicalProperties())
                .withBoms(retriever, repositories, List.of(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0))));
            assertEquals(Version.parse("2.2.0"), new DependencyResolver(resolution, retriever, repositories, new Dependency("com.example", "f")).resolveVersion());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomScopeInheritance() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
                "a:1.4.0", pom("a", "1.4.0", ""),
                "a:2.2.0", pom("a", "2.2.0", "")),
            Map.of("a", metadata("a", "2.2.0", "1.4.0", "2.2.0")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            scopes.scope(Scope.provided)
                .include(new Dependency("com.example", "a"));
            scopes.scope(Scope.test)
                .include(new Dependency("com.example", "a"));
            scopes.scope(Scope.standalone)
                .include(new Dependency("com.example", "a"));

            var properties = new HierarchicalProperties();
            var retriever = ArtifactRetriever.cachingInstance();
            var repositories = serverRepositories(server);

            // the compile scope BOM applies to the compile scope resolution,
            // and follows the classpath composition into the provided and
            // test scope resolutions
            var compile_resolved = scopes.resolveCompileDependencies(properties, retriever, repositories);
            assertEquals(Version.parse("1.4.0"), compile_resolved.get(new Dependency("com.example", "a")).version());
            var provided_resolved = scopes.resolveProvidedDependencies(properties, retriever, repositories);
            assertEquals(Version.parse("1.4.0"), provided_resolved.get(new Dependency("com.example", "a")).version());
            var test_resolved = scopes.resolveTestDependencies(properties, retriever, repositories);
            assertEquals(Version.parse("1.4.0"), test_resolved.get(new Dependency("com.example", "a")).version());

            // the standalone scope is its own world, its version stays
            // unknown and falls back to the latest from the metadata
            var standalone_resolved = scopes.resolveStandaloneDependencies(properties, retriever, repositories);
            assertEquals(VersionNumber.UNKNOWN, standalone_resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomScopeInheritanceIsDirectional() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
                "a:1.4.0", pom("a", "1.4.0", ""),
                "a:2.2.0", pom("a", "2.2.0", "")),
            Map.of("a", metadata("a", "2.2.0", "1.4.0", "2.2.0")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(Scope.test)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            scopes.scope(compile)
                .include(new Dependency("com.example", "a"));

            var properties = new HierarchicalProperties();
            var retriever = ArtifactRetriever.cachingInstance();
            var repositories = serverRepositories(server);

            // a test scope BOM applies to the test scope resolution
            var test_resolved = scopes.resolveTestDependencies(properties, retriever, repositories);
            assertEquals(Version.parse("1.4.0"), test_resolved.get(new Dependency("com.example", "a")).version());

            // but never reaches back into the compile scope resolution
            var compile_resolved = scopes.resolveCompileDependencies(properties, retriever, repositories);
            assertEquals(VersionNumber.UNKNOWN, compile_resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomPrecedenceAcrossScopes() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
                "bom2:1.0.0", bomPom("bom2", "1.0.0", managed("a", "2.2.0")),
                "a:1.4.0", pom("a", "1.4.0", ""),
                "a:2.2.0", pom("a", "2.2.0", "")),
            Map.of("a", metadata("a", "2.2.0", "1.4.0", "2.2.0")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)));
            scopes.scope(Scope.test)
                .include(new Bom("com.example", "bom2", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            // a BOM declared in the dependency's own scope takes precedence
            // over a BOM inherited from a broader scope, so the test scope
            // bom2 wins over the compile scope bom1
            var test_resolved = scopes.resolveTestDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("2.2.0"), test_resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testUncoveredReportingFollowsScopeComposition() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0"))),
            Map.of());
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)));
            scopes.scope(Scope.test)
                // covered through scope composition by the compile scope BOM
                .include(new Dependency("com.example", "a"))
                // not covered by any applicable BOM
                .include(new Dependency("com.example", "g"));

            assertEquals(List.of(new Dependency("com.example", "g")),
                scopes.versionlessDependenciesWithoutBom(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server)));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomIdentityIncludesTypeAndClassifier() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0",
                    managedTyped("a", "1.5.0", "test-jar", null) +
                    managedTyped("b", "1.6.0", "jar", "linux-x86_64") +
                    managed("b", "1.4.0"))),
            Map.of());
        server.start();
        try {
            var resolution = new VersionResolution(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server),
                List.of(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0))));

            // a test-jar entry doesn't cover the regular jar dependency
            assertFalse(resolution.coversDependency(new Dependency("com.example", "a")));
            // entries with the same identifiers but different classifiers
            // are managed independently
            assertEquals(Version.parse("1.4.0"), resolution.overrideDeclaredDependency(new Dependency("com.example", "b")).version());
            assertEquals(Version.parse("1.6.0"), resolution.overrideDeclaredDependency(new Dependency("com.example", "b", null, "linux-x86_64")).version());
            // the modular and forced-classpath JAR types match the plain
            // jar entries of the BOM
            assertEquals(Version.parse("1.4.0"), resolution.overrideDeclaredDependency(new Dependency("com.example", "b", null, null, Dependency.TYPE_MODULAR_JAR)).version());
            assertEquals(Version.parse("1.4.0"), resolution.overrideDeclaredDependency(new Dependency("com.example", "b", null, null, Dependency.TYPE_CLASSPATH_JAR)).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomVersionConflictsAreDetected() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0") + managed("b", "3.0.0")),
                "bom2:1.0.0", bomPom("bom2", "1.0.0", managed("a", "2.2.0"))),
            Map.of());
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)));
            scopes.scope(Scope.test)
                .include(new Bom("com.example", "bom2", new VersionNumber(1, 0, 0)));

            var conflicts = scopes.bomVersionConflicts(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            // only 'a' is managed by both BOMs at different versions, 'b'
            // is managed by a single BOM so it isn't a conflict
            assertEquals(1, conflicts.size());
            var conflict = conflicts.get(0);
            assertEquals("com.example:a", conflict.dependency());

            var versions = conflict.bomVersions().entrySet().iterator();
            // the test scope bom2 wins and is reported first, the compile
            // scope bom1 is the alternative
            var used = versions.next();
            assertEquals("com.example:bom2", used.getKey());
            assertEquals(Version.parse("2.2.0"), used.getValue());
            var other = versions.next();
            assertEquals("com.example:bom1", other.getKey());
            assertEquals(Version.parse("1.4.0"), other.getValue());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testDeclaredVersionConflictsAreDetected() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0") + managed("b", "3.0.0"))),
            Map.of());
        server.start();
        try {
            var retriever = ArtifactRetriever.cachingInstance();
            var repositories = serverRepositories(server);
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                // declared at a different version than the BOM manages
                .include(new Dependency("com.example", "a", new VersionNumber(2, 2, 0)))
                // declared at the same version as the BOM manages
                .include(new Dependency("com.example", "b", new VersionNumber(3, 0, 0)));
            scopes.scope(Scope.test)
                // the same difference through scope composition is
                // reported once
                .include(new Dependency("com.example", "a", new VersionNumber(2, 2, 0)))
                // a version-less dependency takes the BOM version and
                // isn't a difference
                .include(new Dependency("com.example", "b"));

            var conflicts = scopes.declaredVersionConflicts(new HierarchicalProperties(), retriever, repositories);
            assertEquals(1, conflicts.size());
            var conflict = conflicts.get(0);
            assertEquals("com.example:a", conflict.dependency());
            assertEquals(Version.parse("2.2.0"), conflict.declaredVersion());
            assertEquals("com.example:bom1", conflict.bom());
            assertEquals(Version.parse("1.4.0"), conflict.bomVersion());

            // a bld.override that supplies the version prevents the report
            var override_properties = new HierarchicalProperties();
            override_properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:a:2.2.0");
            assertEquals(List.of(),
                scopes.declaredVersionConflicts(override_properties, retriever, repositories));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testEffectiveBomsComposition() {
        var compile_bom = new Bom("com.example", "compile-bom", new VersionNumber(1, 0, 0));
        var runtime_bom = new Bom("com.example", "runtime-bom", new VersionNumber(2, 0, 0));
        var provided_bom = new Bom("com.example", "provided-bom", new VersionNumber(3, 0, 0));
        var test_bom = new Bom("com.example", "test-bom", new VersionNumber(4, 0, 0));
        var standalone_bom = new Bom("com.example", "standalone-bom", new VersionNumber(5, 0, 0));

        var scopes = new DependencyScopes();
        scopes.scope(compile).include(compile_bom);
        scopes.scope(Scope.runtime).include(runtime_bom);
        scopes.scope(Scope.provided).include(provided_bom);
        scopes.scope(Scope.test).include(test_bom);
        scopes.scope(Scope.standalone).include(standalone_bom);

        // the scope's own BOMs come first, then the inherited ones from
        // the more specific to the more general scope
        assertEquals(List.of(compile_bom), scopes.effectiveBoms(compile));
        assertEquals(List.of(provided_bom, compile_bom), scopes.effectiveBoms(Scope.provided));
        assertEquals(List.of(runtime_bom, compile_bom), scopes.effectiveBoms(Scope.runtime));
        assertEquals(List.of(test_bom, compile_bom, provided_bom, runtime_bom), scopes.effectiveBoms(Scope.test));
        assertEquals(List.of(standalone_bom), scopes.effectiveBoms(Scope.standalone));
    }

    @Test
    void testMissingBomFailsLoudly() throws Exception {
        var server = createArtifactServer(Map.of());
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "missing", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            assertThrows(ArtifactNotFoundException.class,
                () -> scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server)));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testManagedEntryWithoutVersionIsIgnored() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0",
                "<dependency><groupId>com.example</groupId><artifactId>x</artifactId></dependency>" +
                managed("a", "1.4.0")),
            "a:1.4.0", pom("a", "1.4.0", "")));
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("1.4.0"), resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomIsNotTransferred() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
            "a:1.4.0", pom("a", "1.4.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("bomtransfers").toFile();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var resolution = new VersionResolution(new HierarchicalProperties());
            var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            resolved.transferIntoDirectory(resolution, ArtifactRetriever.cachingInstance(), serverRepositories(server), tmp, tmp);

            assertTrue(new File(tmp, "a-1.4.0.jar").exists());
            assertEquals(1, tmp.list().length, "expected only the dependency artifact, found: " + List.of(tmp.list()));
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testGenerateTransitiveDependencyTreeWithBom() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("b", "1.5.0")),
            "a:1.0.0", pom("a", "1.0.0", dependency("b", "2.0.0")),
            "b:1.5.0", pom("b", "1.5.0", "")));
        server.start();
        try {
            var dependencies = new DependencySet();
            dependencies.include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)));
            dependencies.include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            var tree = dependencies.generateTransitiveDependencyTree(new VersionResolution(null), ArtifactRetriever.cachingInstance(), serverRepositories(server), compile);
            assertTrue(tree.contains("com.example:a:1.0.0"), "tree was:\n" + tree);
            assertTrue(tree.contains("com.example:b:1.5.0"), "tree was:\n" + tree);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testVersionResolutionBomSemantics() throws Exception {
        var server = createArtifactServer(Map.of(
            "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0"))));
        server.start();
        try {
            var properties = new HierarchicalProperties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:o:5.0.0");
            properties.put(VersionResolution.PROPERTY_TRANSFER_PARALLELISM, "3");
            var base = new VersionResolution(properties);

            // no BOMs returns the same instance
            assertSame(base, base.withBoms(ArtifactRetriever.cachingInstance(), serverRepositories(server), List.of()));
            assertSame(base, base.withBoms(ArtifactRetriever.cachingInstance(), serverRepositories(server), null));

            var resolution = base.withBoms(ArtifactRetriever.cachingInstance(), serverRepositories(server),
                List.of(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0))));

            // the public constructor imports BOMs identically
            var constructed = new VersionResolution(properties, ArtifactRetriever.cachingInstance(), serverRepositories(server),
                List.of(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0))));
            assertEquals(resolution.bomVersions(), constructed.bomVersions());
            assertEquals(resolution.versionOverrides(), constructed.versionOverrides());
            assertEquals(resolution.transferParallelism(), constructed.transferParallelism());

            // the derived instance preserves the overrides and parallelism settings
            assertNotSame(base, resolution);
            assertEquals(base.versionOverrides(), resolution.versionOverrides());
            assertEquals(3, resolution.transferParallelism());
            assertEquals(Map.of("com.example:a:jar:", Version.parse("1.4.0")), resolution.bomVersions());

            // a declared dependency without version is filled in from the BOM
            assertEquals(Version.parse("1.4.0"), resolution.overrideDeclaredDependency(new Dependency("com.example", "a")).version());
            // a declared dependency with an explicit version is never rewritten by the BOM
            assertEquals(Version.parse("3.0.0"), resolution.overrideDeclaredDependency(new Dependency("com.example", "a", new VersionNumber(3, 0, 0))).version());
            // a transitive dependency is pinned by the BOM even when it has a version
            assertEquals(Version.parse("1.4.0"), resolution.overrideTransitiveDependency(new Dependency("com.example", "a", new VersionNumber(2, 0, 0))).version());
            // the version override property beats the BOM
            assertEquals(Version.parse("5.0.0"), resolution.overrideDeclaredDependency(new Dependency("com.example", "o")).version());
            // version resolution consults the BOM for unknown versions only
            assertEquals(Version.parse("1.4.0"), resolution.overrideVersion(new Dependency("com.example", "a")));
            assertEquals(Version.parse("2.0.0"), resolution.overrideVersion(new Dependency("com.example", "a", new VersionNumber(2, 0, 0))));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testBomChangesInvalidateDependenciesCache() throws Exception {
        var tmp = Files.createTempDirectory("bomcache").toFile();
        try {
            var repositories = List.of(new Repository("http://localhost/"));

            var scopes_without_bom = new DependencyScopes();
            scopes_without_bom.scope(compile).include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            var scopes_with_bom = new DependencyScopes();
            scopes_with_bom.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            var cache = new BldCache(tmp, VersionResolution.dummy());
            cache.cacheDependenciesHash(repositories, scopes_without_bom);
            cache.writeCache();

            // the same dependencies without BOM validate against the cache
            var cache_same = new BldCache(tmp, VersionResolution.dummy());
            cache_same.cacheDependenciesHash(repositories, scopes_without_bom);
            assertTrue(cache_same.isDependenciesHashValid());

            // adding a BOM invalidates the cache
            var cache_bom = new BldCache(tmp, VersionResolution.dummy());
            cache_bom.cacheDependenciesHash(repositories, scopes_with_bom);
            assertFalse(cache_bom.isDependenciesHashValid());

            // a different BOM version invalidates the cache too
            cache_bom.writeCache();
            var scopes_with_other_bom = new DependencyScopes();
            scopes_with_other_bom.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(2, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));
            var cache_other_bom = new BldCache(tmp, VersionResolution.dummy());
            cache_other_bom.cacheDependenciesHash(repositories, scopes_with_other_bom);
            assertFalse(cache_other_bom.isDependenciesHashValid());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testBomSurvivesOperationDependenciesCopy() throws Exception {
        // regression test : operations copy the project's dependency scopes
        // through DependencyScopes.include, which used to lose the BOMs so
        // that version-less dependencies resolved to their latest versions
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
                "a:1.4.0", pom("a", "1.4.0", ""),
                "a:2.2.0", pom("a", "2.2.0", "")),
            Map.of("a", metadata("a", "2.2.0", "1.4.0", "2.2.0")));
        server.start();
        var tmp = Files.createTempDirectory("bomcopy").toFile();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var operation = new rife.bld.operations.DownloadOperation()
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .repositories(serverRepositories(server))
                .dependencies(scopes)
                .libCompileDirectory(tmp).libCompileModulesDirectory(tmp)
                .libProvidedDirectory(tmp).libProvidedModulesDirectory(tmp)
                .libRuntimeDirectory(tmp).libRuntimeModulesDirectory(tmp)
                .libStandaloneDirectory(tmp).libStandaloneModulesDirectory(tmp)
                .libTestDirectory(tmp).libTestModulesDirectory(tmp)
                .silent(true);
            operation.execute();

            // the BOM version is downloaded, not the latest version
            assertTrue(new File(tmp, "a-1.4.0.jar").exists());
            assertFalse(new File(tmp, "a-2.2.0.jar").exists());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMissingBomFailsLoudlyThroughOperation() throws Exception {
        // regression test : a missing BOM also has to fail when the
        // dependency scopes were copied into an operation, the BOM is
        // deliberately the only artifact that is missing
        var server = createArtifactServer(Map.of(
            "a:1.0.0", pom("a", "1.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("bommissing").toFile();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(compile)
                .include(new Bom("com.example", "missing", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            var operation = new rife.bld.operations.DownloadOperation()
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .repositories(serverRepositories(server))
                .dependencies(scopes)
                .libCompileDirectory(tmp).libCompileModulesDirectory(tmp)
                .libProvidedDirectory(tmp).libProvidedModulesDirectory(tmp)
                .libRuntimeDirectory(tmp).libRuntimeModulesDirectory(tmp)
                .libStandaloneDirectory(tmp).libStandaloneModulesDirectory(tmp)
                .libTestDirectory(tmp).libTestModulesDirectory(tmp)
                .silent(true);

            assertThrows(ArtifactNotFoundException.class, operation::execute);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testProjectUpdatesWithBomAndOverride() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0"))),
            Map.of("bom1", metadata("bom1", "1.0.0", "1.0.0", "1.0.0"),
                   "a", metadata("a", "9.9.9", "1.4.0", "9.9.9")));
        server.start();
        var tmp = Files.createTempDirectory("bomupdatesoverride").toFile();
        try {
            var project = new BomProject(tmp, transferRepository(server));
            project.properties().put(VersionResolution.PROPERTY_OVERRIDE_PREFIX, "com.example:a:1.4.0");
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var original_out = System.out;
            var captured = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(captured, true));
            int status;
            try {
                status = project.execute(new String[]{"updates"});
            } finally {
                System.setOut(original_out);
            }
            var output = captured.toString();

            assertEquals(0, status);
            // the override takes the version control away from the BOM,
            // the dependency is checked individually against the override
            assertTrue(output.contains("com.example:a:9.9.9"), output);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    static class BomProject extends rife.bld.Project {
        BomProject(File tmp, Repository repository) {
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "bom_project";
            version = new VersionNumber(0, 0, 1);
            repositories = List.of(repository);
        }

        void enableAutoDownloadPurge() {
            autoDownloadPurge = true;
        }
    }

    @Test
    void testProjectDownloadWithBoms() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0") + managed("b", "2.1.0")),
                "a:1.4.0", pom("a", "1.4.0", ""),
                "a:2.2.0", pom("a", "2.2.0", ""),
                "b:2.1.0", pom("b", "2.1.0", "")),
            Map.of("a", metadata("a", "2.2.0", "1.4.0", "2.2.0"),
                   "b", metadata("b", "2.1.0", "2.1.0")));
        server.start();
        var tmp = Files.createTempDirectory("bomproject").toFile();
        try {
            var project = new BomProject(tmp, transferRepository(server));
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            project.dependencies().scope(Scope.test)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"))
                .include(new Dependency("com.example", "b"));

            assertEquals(0, project.execute(new String[]{"download"}));

            // the BOM versions are downloaded in every scope,
            // never the latest versions
            assertTrue(new File(tmp, "lib/compile/a-1.4.0.jar").exists());
            assertFalse(new File(tmp, "lib/compile/a-2.2.0.jar").exists());
            assertTrue(new File(tmp, "lib/test/a-1.4.0.jar").exists());
            assertFalse(new File(tmp, "lib/test/a-2.2.0.jar").exists());
            assertTrue(new File(tmp, "lib/test/b-2.1.0.jar").exists());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testProjectAutoDownloadPurgeBomUpgrade() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
                "bom1:2.0.0", bomPom("bom1", "2.0.0", managed("a", "2.2.0")),
                "a:1.4.0", pom("a", "1.4.0", ""),
                "a:2.2.0", pom("a", "2.2.0", "")),
            Map.of("a", metadata("a", "2.2.0", "1.4.0", "2.2.0")));
        server.start();
        var tmp = Files.createTempDirectory("bomupgrade").toFile();
        try {
            var project = new BomProject(tmp, transferRepository(server));
            project.enableAutoDownloadPurge();
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            assertEquals(0, project.execute(new String[]{"version"}));
            assertTrue(new File(tmp, "lib/compile/a-1.4.0.jar").exists());

            // running again with an identical BOM declaration
            // doesn't invalidate the dependencies cache
            project = new BomProject(tmp, transferRepository(server));
            project.enableAutoDownloadPurge();
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            assertEquals(0, project.execute(new String[]{"version"}));
            assertTrue(new File(tmp, "lib/compile/a-1.4.0.jar").exists());

            // upgrading the BOM version invalidates the cache,
            // downloads the new version and purges the old one
            project = new BomProject(tmp, transferRepository(server));
            project.enableAutoDownloadPurge();
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(2, 0, 0)))
                .include(new Dependency("com.example", "a"));
            assertEquals(0, project.execute(new String[]{"version"}));
            assertTrue(new File(tmp, "lib/compile/a-2.2.0.jar").exists());
            assertFalse(new File(tmp, "lib/compile/a-1.4.0.jar").exists());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testProjectUpdatesWithBom() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0"))),
            Map.of("bom1", metadata("bom1", "2.0.0", "1.0.0", "2.0.0"),
                   "a", metadata("a", "9.9.9", "1.4.0", "9.9.9")));
        server.start();
        var tmp = Files.createTempDirectory("bomupdates").toFile();
        try {
            var project = new BomProject(tmp, transferRepository(server));
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            project.dependencies().scope(Scope.test)
                // covered through scope composition by the compile scope BOM,
                // must not be reported individually either
                .include(new Dependency("com.example", "a"));

            var original_out = System.out;
            var captured = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(captured, true));
            int status;
            try {
                status = project.execute(new String[]{"updates"});
            } finally {
                System.setOut(original_out);
            }
            var output = captured.toString();

            assertEquals(0, status);
            // the newer BOM version is reported with its type suffix
            assertTrue(output.contains("com.example:bom1:2.0.0@bom"), output);
            // the version-less dependency covered by the BOM
            // is not reported individually
            assertFalse(output.contains("com.example:a"), output);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testProjectDependencyTreeWithBom() throws Exception {
        var server = createArtifactServer(Map.of(
                "bom1:1.0.0", bomPom("bom1", "1.0.0", managed("a", "1.4.0")),
                "a:1.4.0", pom("a", "1.4.0", "")),
            Map.of("a", metadata("a", "2.2.0", "1.4.0", "2.2.0")));
        server.start();
        var tmp = Files.createTempDirectory("bomtree").toFile();
        try {
            var project = new BomProject(tmp, transferRepository(server));
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            project.dependencies().scope(Scope.test)
                // covered through scope composition by the compile scope BOM
                .include(new Dependency("com.example", "a"));

            var original_out = System.out;
            var captured = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(captured, true));
            int status;
            try {
                status = project.execute(new String[]{"dependency-tree"});
            } finally {
                System.setOut(original_out);
            }
            var output = captured.toString();

            assertEquals(0, status);
            // the trees reflect the BOM version, not the latest version,
            // in the compile scope as well as in the test scope that the
            // compile scope BOM also applies to
            assertTrue(output.contains("com.example:a:1.4.0"), output);
            assertFalse(output.contains("com.example:a:2.2.0"), output);
            var test_tree = output.substring(output.indexOf("test:"));
            assertTrue(test_tree.contains("com.example:a:1.4.0"), output);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testProjectMissingBomFails() throws Exception {
        // the BOM is deliberately the only artifact that is missing
        var server = createArtifactServer(Map.of(
            "a:1.0.0", pom("a", "1.0.0", "")));
        server.start();
        var tmp = Files.createTempDirectory("bommissingproject").toFile();
        try {
            var project = new BomProject(tmp, transferRepository(server));
            project.dependencies().scope(compile)
                .include(new Bom("com.example", "missing", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a", new VersionNumber(1, 0, 0)));

            assertNotEquals(0, project.execute(new String[]{"download"}));
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    private static Repository transferRepository(HttpServer server) {
        return new Repository("http://localhost:" + server.getAddress().getPort() + "/");
    }

    @Test
    void testProjectDownloadVertxBom() throws Exception {
        // real-world reproduction of the scenario in
        // https://github.com/rife2/bld/issues/59 through a full project
        var tmp = Files.createTempDirectory("bomvertx").toFile();
        try {
            var project = new BomProject(tmp, getNextRepository());
            project.dependencies().scope(compile)
                .include(new Bom("io.vertx", "vertx-stack-depchain", new VersionNumber(4, 5, 12)))
                .include(new Dependency("io.vertx", "vertx-core"))
                .include(new Dependency("com.fasterxml.jackson.core", "jackson-databind"));

            assertEquals(0, project.execute(new String[]{"download"}));

            var jars = List.of(java.util.Objects.requireNonNull(new File(tmp, "lib/compile").list()));
            assertTrue(jars.contains("vertx-core-4.5.12.jar"), jars.toString());
            assertTrue(jars.contains("jackson-databind-2.16.1.jar"), jars.toString());
            assertTrue(jars.contains("jackson-core-2.16.1.jar"), jars.toString());
            // no jackson artifact resolved to anything but the BOM version
            assertEquals(0, jars.stream().filter(jar -> jar.startsWith("jackson-") && !jar.contains("2.16.1")).count(), jars.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testProjectDownloadJUnitBomTestScope() throws Exception {
        // real-world reproduction of the scenario reported for the
        // BOMs getting lost when operations copy the project scopes
        var tmp = Files.createTempDirectory("bomjunit").toFile();
        try {
            var project = new BomProject(tmp, getNextRepository());
            project.dependencies().scope(Scope.test)
                .include(new Bom("org.junit", "junit-bom", new VersionNumber(6, 1, 1)))
                .include(new Dependency("org.junit.jupiter", "junit-jupiter"))
                .include(new Dependency("org.junit.platform", "junit-platform-console-standalone"));

            assertEquals(0, project.execute(new String[]{"download"}));

            var jars = List.of(java.util.Objects.requireNonNull(new File(tmp, "lib/test").list()));
            assertTrue(jars.contains("junit-jupiter-6.1.1.jar"), jars.toString());
            // every artifact that junit-bom manages is pinned to the BOM version
            assertEquals(0, jars.stream().filter(jar -> jar.startsWith("junit-") &&
                                                        !jar.startsWith("junit-platform-console-standalone-") &&
                                                        !jar.contains("6.1.1")).count(), jars.toString());
            // junit-bom deliberately doesn't manage the console-standalone
            // distribution artifact, so it resolves to its latest version
            assertTrue(jars.stream().anyMatch(jar -> jar.startsWith("junit-platform-console-standalone-")), jars.toString());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testProjectUpdatesRealBom() throws Exception {
        // a real-world BOM with known newer releases reports its own
        // update while its covered dependencies stay silent
        var tmp = Files.createTempDirectory("bomupdatesreal").toFile();
        try {
            var project = new BomProject(tmp, getNextRepository());
            project.dependencies().scope(Scope.test)
                .include(new Bom("org.junit", "junit-bom", new VersionNumber(6, 1, 1)))
                .include(new Dependency("org.junit.jupiter", "junit-jupiter"));

            var original_out = System.out;
            var captured = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(captured, true));
            int status;
            try {
                status = project.execute(new String[]{"updates"});
            } finally {
                System.setOut(original_out);
            }
            var output = captured.toString();

            assertEquals(0, status);
            assertTrue(output.contains("org.junit:junit-bom:"), output);
            assertTrue(output.contains("@bom"), output);
            assertFalse(output.contains("org.junit.jupiter:junit-jupiter:"), output);
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testBomVertxAcceptance() {
        // reproduction of the scenario in https://github.com/rife2/bld/issues/59
        var scopes = new DependencyScopes();
        scopes.scope(compile)
            .include(new Bom("io.vertx", "vertx-stack-depchain", new VersionNumber(4, 5, 12)))
            .include(new Dependency("io.vertx", "vertx-core"))
            .include(new Dependency("com.fasterxml.jackson.core", "jackson-databind"));

        var resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), getNextRepositories());
        assertEquals(Version.parse("4.5.12"), resolved.get(new Dependency("io.vertx", "vertx-core")).version());
        assertEquals(Version.parse("2.16.1"), resolved.get(new Dependency("com.fasterxml.jackson.core", "jackson-databind")).version());
        assertEquals(Version.parse("2.16.1"), resolved.get(new Dependency("com.fasterxml.jackson.core", "jackson-core")).version());
    }

    private static List<Repository> serverRepositories(HttpServer server) {
        return List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/"));
    }

    private static HttpServer createArtifactServer(Map<String, String> poms)
    throws IOException {
        return createArtifactServer(poms, Map.of());
    }

    private static HttpServer createArtifactServer(Map<String, String> poms, Map<String, String> metadata)
    throws IOException {
        var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            byte[] body = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                var content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
                if (content != null) {
                    body = content.getBytes();
                }
            } else if (filename.endsWith(".jar") && segments.length >= 3) {
                if (poms.containsKey(segments[segments.length - 3] + ":" + segments[segments.length - 2])) {
                    body = "jar".getBytes();
                }
            } else if (filename.equals("maven-metadata.xml") && segments.length >= 2) {
                var content = metadata.get(segments[segments.length - 2]);
                if (content != null) {
                    body = content.getBytes();
                }
            }
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    private static String dependency(String artifact, String version) {
        return "<dependency><groupId>com.example</groupId><artifactId>" + artifact + "</artifactId><version>" + version + "</version></dependency>";
    }

    private static String managed(String artifact, String version) {
        return dependency(artifact, version);
    }

    private static String managedTyped(String artifact, String version, String type, String classifier) {
        return "<dependency><groupId>com.example</groupId><artifactId>" + artifact + "</artifactId><version>" + version + "</version>" +
               "<type>" + type + "</type>" +
               (classifier == null ? "" : "<classifier>" + classifier + "</classifier>") +
               "</dependency>";
    }

    private static String pom(String artifact, String version, String dependencies) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <dependencies>%s</dependencies>
            </project>""".formatted(artifact, version, dependencies);
    }

    private static String bomPom(String artifact, String version, String managedDependencies) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                    <dependencies>%s</dependencies>
                </dependencyManagement>
            </project>""".formatted(artifact, version, managedDependencies);
    }

    private static String bomPomWithParent(String artifact, String version, String parentArtifact, String managedDependencies) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0</version>
                </parent>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                    <dependencies>%s</dependencies>
                </dependencyManagement>
            </project>""".formatted(parentArtifact, artifact, version, managedDependencies);
    }

    private static String metadata(String artifact, String latest, String... versions) {
        var versions_xml = new StringBuilder();
        for (var version : versions) {
            versions_xml.append("<version>").append(version).append("</version>");
        }
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
                <groupId>com.example</groupId>
                <artifactId>%s</artifactId>
                <versioning>
                    <latest>%s</latest>
                    <release>%s</release>
                    <versions>%s</versions>
                </versioning>
            </metadata>""".formatted(artifact, latest, latest, versions_xml);
    }
}
