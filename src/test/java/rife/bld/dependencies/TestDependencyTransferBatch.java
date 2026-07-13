/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.exceptions.DependencyTransferException;
import rife.tools.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.TransferTestHelper.*;

public class TestDependencyTransferBatch {
    @Test
    void testTransferAcrossSets() throws Exception {
        var max_concurrent_transfers = new AtomicInteger();
        var server = createTransferServer(max_concurrent_transfers);
        server.start();
        var tmp1 = Files.createTempDirectory("transfers1").toFile();
        var tmp2 = Files.createTempDirectory("transfers2").toFile();
        try {
            var set1 = createTransferDependencies(1, 3);
            var set2 = createTransferDependencies(4, 6);
            var repositories = List.of(transferRepository(server));

            var batch = new DependencyTransferBatch();
            batch.add(set1, tmp1, tmp1)
                 .add(set2, tmp2, tmp2)
                 // identical transfers into the same directory are only performed once
                 .add(set1, tmp1, tmp1);
            var artifacts = batch.transfer(new VersionResolution(null), ArtifactRetriever.instance(), repositories);

            assertEquals(6, artifacts.size());
            assertTransferredArtifacts(set1, artifacts.subList(0, 3), tmp1);
            assertTransferredArtifacts(set2, artifacts.subList(3, 6), tmp2);
            assertTrue(max_concurrent_transfers.get() > 1, "expected concurrent transfers, max was " + max_concurrent_transfers.get());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp1);
            FileUtils.deleteDirectory(tmp2);
        }
    }

    @Test
    void testTransferEmptiesTheBatch() throws Exception {
        var server = createTransferServer(new AtomicInteger());
        server.start();
        var tmp = Files.createTempDirectory("transfers").toFile();
        try {
            var repositories = List.of(transferRepository(server));
            var resolution = new VersionResolution(null);
            var batch = new DependencyTransferBatch();

            var set1 = createTransferDependencies(1, 3);
            batch.add(set1, tmp, tmp);
            assertTransferredArtifacts(set1, batch.transfer(resolution, ArtifactRetriever.instance(), repositories), tmp);

            // the batch was emptied, nothing is transferred again
            assertTrue(batch.transfer(resolution, ArtifactRetriever.instance(), repositories).isEmpty());

            // the same instance can collect and transfer again,
            // including targets that were transferred before
            var set2 = createTransferDependencies(3, 5);
            batch.add(set2, tmp, tmp);
            assertTransferredArtifacts(set2, batch.transfer(resolution, ArtifactRetriever.instance(), repositories), tmp);
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testTransferModularJarsIntoModulesDirectory() throws Exception {
        var server = createTransferServer(new AtomicInteger());
        server.start();
        var artifacts_dir = Files.createTempDirectory("artifacts").toFile();
        var modules_dir = Files.createTempDirectory("modules").toFile();
        try {
            var dependencies = new DependencySet()
                .include(new Dependency("com.example", "artifact1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "module1", new VersionNumber(1, 0, 0), "", Dependency.TYPE_MODULAR_JAR));

            var artifacts = new DependencyTransferBatch()
                .add(dependencies, artifacts_dir, modules_dir)
                .transfer(new VersionResolution(null), ArtifactRetriever.instance(), List.of(transferRepository(server)));

            assertEquals(2, artifacts.size());
            assertTrue(new File(artifacts_dir, "artifact1-1.0.0.jar").exists());
            assertTrue(new File(modules_dir, "module1-1.0.0.jar").exists());
            assertFalse(new File(artifacts_dir, "module1-1.0.0.jar").exists());
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(artifacts_dir);
            FileUtils.deleteDirectory(modules_dir);
        }
    }

    @Test
    void testTransferClassifiers() throws Exception {
        var server = createTransferServer(new AtomicInteger());
        server.start();
        var tmp = Files.createTempDirectory("transfers").toFile();
        try {
            var dependencies = createTransferDependencies(1, 2);
            var artifacts = new DependencyTransferBatch()
                .add(dependencies, tmp, tmp, "sources")
                .transfer(new VersionResolution(null), ArtifactRetriever.instance(), List.of(transferRepository(server)));

            assertEquals(4, artifacts.size());
            for (var i = 1; i <= 2; i++) {
                assertTrue(new File(tmp, "artifact" + i + "-1.0.0.jar").exists());
                assertTrue(new File(tmp, "artifact" + i + "-1.0.0-sources.jar").exists());
            }
        } finally {
            server.stop(0);
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMissingDirectories() {
        var regular = new DependencySet()
            .include(new Dependency("com.example", "artifact1", new VersionNumber(1, 0, 0)));
        var modular = new DependencySet()
            .include(new Dependency("com.example", "module1", new VersionNumber(1, 0, 0), "", Dependency.TYPE_MODULAR_JAR));

        assertThrows(DependencyTransferException.class, () -> new DependencyTransferBatch().add(regular, null, null));
        assertThrows(DependencyTransferException.class, () -> new DependencyTransferBatch().add(modular, Files.createTempDirectory("artifacts").toFile(), null));
    }

    @Test
    void testEmptyBatch() {
        var artifacts = new DependencyTransferBatch()
            .transfer(new VersionResolution(null), ArtifactRetriever.instance(), List.of());
        assertTrue(artifacts.isEmpty());
    }
}
