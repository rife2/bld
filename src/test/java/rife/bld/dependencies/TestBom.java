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
import static rife.bld.dependencies.RepositoryTestHelper.getNextRepositories;
import static rife.bld.dependencies.Scope.compile;

public class TestBom {
    @Test
    void testParse() {
        assertEquals(new Bom("com.example", "bom1"), Bom.parse("com.example:bom1"));
        assertEquals(new Bom("com.example", "bom1", new VersionNumber(1, 2, 3)), Bom.parse("com.example:bom1:1.2.3"));
        assertEquals(new Bom("com.example", "bom1", new VersionNumber(1, 2, 3)), Bom.parse("com.example:bom1:1.2.3@bom"));
        assertEquals(new Bom("com.example", "bom1", new VersionNumber(1, 2, 3)), Bom.parse("com.example:bom1:1.2.3@pom"));
        assertEquals(new Bom("com.example", "bom1", new VersionNumber(1, 2, 3), "classifier1"), Bom.parse("com.example:bom1:1.2.3:classifier1"));
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
    void testBomScopeIsolation() throws Exception {
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
            scopes.scope(Scope.test)
                .include(new Dependency("com.example", "a"));

            // the compile scope BOM applies to the compile scope resolution
            var compile_resolved = scopes.resolveCompileDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(Version.parse("1.4.0"), compile_resolved.get(new Dependency("com.example", "a")).version());

            // and doesn't leak into the test scope resolution, whose version
            // stays unknown and falls back to the latest from the metadata
            var test_resolved = scopes.resolveTestDependencies(new HierarchicalProperties(), ArtifactRetriever.cachingInstance(), serverRepositories(server));
            assertEquals(VersionNumber.UNKNOWN, test_resolved.get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
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
            assertEquals(Map.of("com.example:a", Version.parse("1.4.0")), resolution.bomVersions());

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
