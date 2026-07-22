/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.json.Json;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import rife.bld.Project;
import rife.bld.WebProject;
import rife.bld.blueprints.AppProjectBlueprint;
import rife.bld.dependencies.*;
import rife.bld.publish.PublishArtifact;
import rife.bld.publish.PublishInfo;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestPublishOperation {
    static File reposiliteJar_ = null;

    @BeforeAll
    static void downloadReposilite()
    throws Exception {
        reposiliteJar_ = File.createTempFile("reposilite", "jar");
        reposiliteJar_.deleteOnExit();
        System.out.print("Downloading: https://maven.reposilite.com/releases/com/reposilite/reposilite/3.4.0/reposilite-3.4.0-all.jar ...");
        System.out.flush();
        FileUtils.copy(new URL("https://maven.reposilite.com/releases/com/reposilite/reposilite/3.4.0/reposilite-3.4.0-all.jar").openStream(), reposiliteJar_);
        System.out.println("done");
    }

    @AfterAll
    static void deleteReposilite()
    throws Exception {
        if (reposiliteJar_ != null) {
            reposiliteJar_.delete();
            reposiliteJar_ = null;
        }
    }

    static class PublishProject extends AppProjectBlueprint {
        public PublishProject(File work, String packageName, String projectName, String baseName, VersionNumber versionNumber) {
            super(work, packageName, projectName, baseName, versionNumber);
            javaRelease = 17;
        }
    }

    @Test
    void testInstantiation() {
        var operation = new PublishOperation();
        assertFalse(operation.offline());
        assertTrue(operation.repositories().isEmpty());
        assertNull(operation.moment());
        assertTrue(operation.dependencies().isEmpty());
        assertTrue(operation.publishProperties().isEmpty());
        assertNotNull(operation.info());
        assertNull(operation.info().groupId());
        assertNull(operation.info().artifactId());
        assertNull(operation.info().version());
        assertNull(operation.info().name());
        assertNull(operation.info().description());
        assertNull(operation.info().url());
        assertTrue(operation.info().licenses().isEmpty());
        assertTrue(operation.info().developers().isEmpty());
        assertNull(operation.info().scm());
        assertTrue(operation.artifacts().isEmpty());
    }

    @Test
    void testPopulation() {
        var repository1 = new Repository("repository1");
        var repository2 = new Repository("repository2");
        var moment = ZonedDateTime.now();

        var artifact1 = new PublishArtifact(new File("file1"), "classifier1", "type1");
        var artifact2 = new PublishArtifact(new File("file2"), "classifier2", "type2");

        var operation1 = new PublishOperation()
            .repositories(repository1, repository2)
            .moment(moment)
            .artifacts(List.of(artifact1, artifact2));
        assertTrue(operation1.repositories().contains(repository1));
        assertTrue(operation1.repositories().contains(repository2));
        assertEquals(moment, operation1.moment());
        assertTrue(operation1.artifacts().contains(artifact1));
        assertTrue(operation1.artifacts().contains(artifact2));

        var operation2 = new PublishOperation()
            .moment(moment);
        operation2.repositories().add(repository1);
        operation2.repositories().add(repository2);
        operation2.artifacts().add(artifact1);
        operation2.artifacts().add(artifact2);
        assertTrue(operation2.repositories().contains(repository1));
        assertTrue(operation2.repositories().contains(repository2));
        assertEquals(moment, operation2.moment());
        assertTrue(operation2.artifacts().contains(artifact1));
        assertTrue(operation2.artifacts().contains(artifact2));

        var operation3 = new PublishOperation()
            .offline(true)
            .repository(repository1)
            .repository(repository2)
            .moment(moment)
            .artifacts(List.of(artifact1, artifact2));
        assertTrue(operation3.offline());
        operation3.publishProperties().mavenCompilerSource(17).mavenCompilerTarget(19);
        assertTrue(operation3.repositories().contains(repository1));
        assertTrue(operation3.repositories().contains(repository2));
        assertEquals(moment, operation3.moment());
        assertTrue(operation3.artifacts().contains(artifact1));
        assertTrue(operation3.artifacts().contains(artifact2));
        assertEquals(17, operation3.publishProperties().mavenCompilerSource());
        assertEquals(19, operation3.publishProperties().mavenCompilerTarget());
    }

    @Test
    @DisabledOnOs({OS.WINDOWS})
    void testPublishRelease()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        var tmp_reposilite = Files.createTempDirectory("test").toFile();
        try {
            var process_builder = new ProcessBuilder(
                "java", "-jar", reposiliteJar_.getAbsolutePath(),
                "-wd", tmp_reposilite.getAbsolutePath(),
                "-p", "8081",
                "--token", "manager:passwd");
            process_builder.directory(tmp_reposilite);
            var process = process_builder.start();

            // wait for full startup
            Thread.sleep(6000);

            // verify the version doesn't exist
            assertThrows(FileUtilsErrorException.class, () -> FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp")));
            assertThrows(FileUtilsErrorException.class, () -> FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp/0.0.1")));

            // create a first publication
            var create_operation1 = new CreateAppOperation()
                .workDirectory(tmp1)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation1.execute();

            new CompileOperation()
                .fromProject(create_operation1.project())
                .execute();

            var jar_operation1 = new JarOperation()
                .fromProject(create_operation1.project());
            jar_operation1.execute();

            var publish_operation1 = new PublishOperation()
                .fromProject(create_operation1.project())
                .repositories(new Repository("http://localhost:8081/releases", "manager", "passwd"));
            publish_operation1.execute();

            var dir_json1 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp")));
            assertEquals("myapp", dir_json1.get("name"));
            var dir_files_json1 = dir_json1.getArray("files");
            assertEquals(6, dir_files_json1.size());
            assertEquals("0.0.1", dir_files_json1.getObject(0).get("name"));
            assertEquals("maven-metadata.xml.md5", dir_files_json1.getObject(1).get("name"));
            assertEquals("maven-metadata.xml.sha1", dir_files_json1.getObject(2).get("name"));
            assertEquals("maven-metadata.xml.sha256", dir_files_json1.getObject(3).get("name"));
            assertEquals("maven-metadata.xml.sha512", dir_files_json1.getObject(4).get("name"));
            assertEquals("maven-metadata.xml", dir_files_json1.getObject(5).get("name"));

            var maven_metadata1 = new Xml2MavenMetadata();
            maven_metadata1.processXml(FileUtils.readString(new URL("http://localhost:8081/releases/test/pkg/myapp/maven-metadata.xml")));
            assertEquals(create_operation1.project().version(), maven_metadata1.getLatest());
            assertEquals(create_operation1.project().version(), maven_metadata1.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata1.getSnapshot());
            assertNull(maven_metadata1.getSnapshotTimestamp());
            assertNull(maven_metadata1.getSnapshotBuildNumber());
            assertEquals(1, maven_metadata1.getVersions().size());
            assertTrue(maven_metadata1.getVersions().contains(create_operation1.project().version()));

            var version_json1 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp/0.0.1")));
            assertEquals("0.0.1", version_json1.get("name"));
            var version_files_json1 = version_json1.getArray("files");
            assertEquals(10, version_files_json1.size());
            assertEquals("myapp-0.0.1.jar.md5", version_files_json1.getObject(0).get("name"));
            assertEquals("myapp-0.0.1.jar.sha1", version_files_json1.getObject(1).get("name"));
            assertEquals("myapp-0.0.1.jar.sha256", version_files_json1.getObject(2).get("name"));
            assertEquals("myapp-0.0.1.jar.sha512", version_files_json1.getObject(3).get("name"));
            assertEquals("myapp-0.0.1.jar", version_files_json1.getObject(4).get("name"));
            assertEquals("myapp-0.0.1.pom.md5", version_files_json1.getObject(5).get("name"));
            assertEquals("myapp-0.0.1.pom.sha1", version_files_json1.getObject(6).get("name"));
            assertEquals("myapp-0.0.1.pom.sha256", version_files_json1.getObject(7).get("name"));
            assertEquals("myapp-0.0.1.pom.sha512", version_files_json1.getObject(8).get("name"));
            assertEquals("myapp-0.0.1.pom", version_files_json1.getObject(9).get("name"));

            // created an updated publication
            var create_operation2 = new CreateAppOperation() {
                protected Project createProjectBlueprint() {
                    return new PublishProject(new File(workDirectory(), projectName()), packageName(), projectName(), baseName(), new VersionNumber(1, 0, 0));
                }
            }
                .workDirectory(tmp2)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation2.execute();

            new CompileOperation()
                .fromProject(create_operation2.project())
                .execute();

            var jar_operation2 = new JarOperation()
                .fromProject(create_operation2.project());
            jar_operation2.execute();

            var publish_operation2 = new PublishOperation()
                .fromProject(create_operation2.project())
                .repositories(new Repository("http://localhost:8081/releases", "manager", "passwd"));
            publish_operation2.execute();

            var dir_json2 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp")));
            assertEquals("myapp", dir_json2.get("name"));
            var dir_files_json2 = dir_json2.getArray("files");
            assertEquals(7, dir_files_json2.size());
            assertEquals("0.0.1", dir_files_json2.getObject(0).get("name"));
            assertEquals("1.0.0", dir_files_json2.getObject(1).get("name"));
            assertEquals("maven-metadata.xml.md5", dir_files_json2.getObject(2).get("name"));
            assertEquals("maven-metadata.xml.sha1", dir_files_json2.getObject(3).get("name"));
            assertEquals("maven-metadata.xml.sha256", dir_files_json2.getObject(4).get("name"));
            assertEquals("maven-metadata.xml.sha512", dir_files_json2.getObject(5).get("name"));
            assertEquals("maven-metadata.xml", dir_files_json2.getObject(6).get("name"));

            var maven_metadata2 = new Xml2MavenMetadata();
            maven_metadata2.processXml(FileUtils.readString(new URL("http://localhost:8081/releases/test/pkg/myapp/maven-metadata.xml")));
            assertEquals(create_operation2.project().version(), maven_metadata2.getLatest());
            assertEquals(create_operation2.project().version(), maven_metadata2.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata2.getSnapshot());
            assertNull(maven_metadata2.getSnapshotTimestamp());
            assertNull(maven_metadata2.getSnapshotBuildNumber());
            assertEquals(2, maven_metadata2.getVersions().size());
            assertTrue(maven_metadata2.getVersions().contains(create_operation1.project().version()));
            assertTrue(maven_metadata2.getVersions().contains(create_operation2.project().version()));

            var version_json1b = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp/0.0.1")));
            assertEquals("0.0.1", version_json1b.get("name"));
            var version_files_json1b = version_json1b.getArray("files");
            assertEquals(10, version_files_json1b.size());
            assertEquals("myapp-0.0.1.jar.md5", version_files_json1b.getObject(0).get("name"));
            assertEquals("myapp-0.0.1.jar.sha1", version_files_json1b.getObject(1).get("name"));
            assertEquals("myapp-0.0.1.jar.sha256", version_files_json1b.getObject(2).get("name"));
            assertEquals("myapp-0.0.1.jar.sha512", version_files_json1b.getObject(3).get("name"));
            assertEquals("myapp-0.0.1.jar", version_files_json1b.getObject(4).get("name"));
            assertEquals("myapp-0.0.1.pom.md5", version_files_json1b.getObject(5).get("name"));
            assertEquals("myapp-0.0.1.pom.sha1", version_files_json1b.getObject(6).get("name"));
            assertEquals("myapp-0.0.1.pom.sha256", version_files_json1b.getObject(7).get("name"));
            assertEquals("myapp-0.0.1.pom.sha512", version_files_json1b.getObject(8).get("name"));
            assertEquals("myapp-0.0.1.pom", version_files_json1b.getObject(9).get("name"));

            var version_json2 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp/1.0.0")));
            assertEquals("1.0.0", version_json2.get("name"));
            var version_files_json2 = version_json2.getArray("files");
            assertEquals(10, version_files_json2.size());
            assertEquals("myapp-1.0.0.jar.md5", version_files_json2.getObject(0).get("name"));
            assertEquals("myapp-1.0.0.jar.sha1", version_files_json2.getObject(1).get("name"));
            assertEquals("myapp-1.0.0.jar.sha256", version_files_json2.getObject(2).get("name"));
            assertEquals("myapp-1.0.0.jar.sha512", version_files_json2.getObject(3).get("name"));
            assertEquals("myapp-1.0.0.jar", version_files_json2.getObject(4).get("name"));
            assertEquals("myapp-1.0.0.pom.md5", version_files_json2.getObject(5).get("name"));
            assertEquals("myapp-1.0.0.pom.sha1", version_files_json2.getObject(6).get("name"));
            assertEquals("myapp-1.0.0.pom.sha256", version_files_json2.getObject(7).get("name"));
            assertEquals("myapp-1.0.0.pom.sha512", version_files_json2.getObject(8).get("name"));
            assertEquals("myapp-1.0.0.pom", version_files_json2.getObject(9).get("name"));

            process.destroy();
        } finally {
            FileUtils.deleteDirectory(tmp_reposilite);
            FileUtils.deleteDirectory(tmp2);
            FileUtils.deleteDirectory(tmp1);
        }
    }

    @Test
    void testPublishReleaseLocal()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        var tmp_local = Files.createTempDirectory("test").toFile();
        try {
            // create a first publication
            var create_operation1 = new CreateAppOperation()
                .workDirectory(tmp1)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation1.execute();

            new CompileOperation()
                .fromProject(create_operation1.project())
                .execute();

            var jar_operation1 = new JarOperation()
                .fromProject(create_operation1.project());
            jar_operation1.execute();

            var publish_operation1 = new PublishOperation()
                .fromProject(create_operation1.project())
                .repositories(new Repository(tmp_local.getAbsolutePath()));
            publish_operation1.execute();

            assertEquals("""
                /test
                /test/pkg
                /test/pkg/myapp
                /test/pkg/myapp/0.0.1
                /test/pkg/myapp/0.0.1/myapp-0.0.1.jar
                /test/pkg/myapp/0.0.1/myapp-0.0.1.pom
                /test/pkg/myapp/maven-metadata-local.xml""", FileUtils.generateDirectoryListing(tmp_local));

            var maven_metadata1 = new Xml2MavenMetadata();
            maven_metadata1.processXml(FileUtils.readString(Path.of(tmp_local.getAbsolutePath(), "test", "pkg", "myapp", "maven-metadata-local.xml").toFile()));
            assertEquals(create_operation1.project().version(), maven_metadata1.getLatest());
            assertEquals(create_operation1.project().version(), maven_metadata1.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata1.getSnapshot());
            assertNull(maven_metadata1.getSnapshotTimestamp());
            assertNull(maven_metadata1.getSnapshotBuildNumber());
            assertEquals(1, maven_metadata1.getVersions().size());
            assertTrue(maven_metadata1.getVersions().contains(create_operation1.project().version()));

            // created an updated publication
            var create_operation2 = new CreateAppOperation() {
                protected Project createProjectBlueprint() {
                    return new PublishProject(new File(workDirectory(), projectName()), packageName(), projectName(), baseName(), new VersionNumber(1, 0, 0));
                }
            }
                .workDirectory(tmp2)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation2.execute();

            new CompileOperation()
                .fromProject(create_operation2.project())
                .execute();

            var jar_operation2 = new JarOperation()
                .fromProject(create_operation2.project());
            jar_operation2.execute();

            var publish_operation2 = new PublishOperation()
                .fromProject(create_operation2.project())
                .repositories(new Repository(tmp_local.getAbsolutePath()));
            publish_operation2.execute();

            assertEquals("""
                /test
                /test/pkg
                /test/pkg/myapp
                /test/pkg/myapp/0.0.1
                /test/pkg/myapp/0.0.1/myapp-0.0.1.jar
                /test/pkg/myapp/0.0.1/myapp-0.0.1.pom
                /test/pkg/myapp/1.0.0
                /test/pkg/myapp/1.0.0/myapp-1.0.0.jar
                /test/pkg/myapp/1.0.0/myapp-1.0.0.pom
                /test/pkg/myapp/maven-metadata-local.xml""", FileUtils.generateDirectoryListing(tmp_local));

            var maven_metadata2 = new Xml2MavenMetadata();
            maven_metadata2.processXml(FileUtils.readString(Path.of(tmp_local.getAbsolutePath(), "test", "pkg", "myapp", "maven-metadata-local.xml").toFile()));
            assertEquals(create_operation2.project().version(), maven_metadata2.getLatest());
            assertEquals(create_operation2.project().version(), maven_metadata2.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata2.getSnapshot());
            assertNull(maven_metadata2.getSnapshotTimestamp());
            assertNull(maven_metadata2.getSnapshotBuildNumber());
            assertEquals(2, maven_metadata2.getVersions().size());
            assertTrue(maven_metadata2.getVersions().contains(create_operation1.project().version()));
            assertTrue(maven_metadata2.getVersions().contains(create_operation2.project().version()));
        } finally {
            FileUtils.deleteDirectory(tmp_local);
            FileUtils.deleteDirectory(tmp2);
            FileUtils.deleteDirectory(tmp1);
        }
    }

    @Test
    @DisabledOnOs({OS.WINDOWS})
    void testPublishSnapshot()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        var tmp_reposilite = Files.createTempDirectory("test").toFile();
        try {
            var process_builder = new ProcessBuilder(
                "java", "-jar", reposiliteJar_.getAbsolutePath(),
                "-wd", tmp_reposilite.getAbsolutePath(),
                "-p", "8081",
                "--token", "manager:passwd");
            process_builder.directory(tmp_reposilite);
            var process = process_builder.start();

            // wait for full startup
            Thread.sleep(6000);

            // verify the version doesn't exist
            assertThrows(FileUtilsErrorException.class, () -> FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp")));
            assertThrows(FileUtilsErrorException.class, () -> FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp/1.2.3-SNAPSHOT")));

            // create a first publication
            var create_operation1 = new CreateAppOperation() {
                protected Project createProjectBlueprint() {
                    return new PublishProject(new File(workDirectory(), projectName()), packageName(), projectName(), baseName(), new VersionNumber(1, 2, 3, "SNAPSHOT"));
                }
            }
                .workDirectory(tmp1)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation1.execute();

            new CompileOperation()
                .fromProject(create_operation1.project())
                .execute();

            var jar_operation1 = new JarOperation()
                .fromProject(create_operation1.project());
            jar_operation1.execute();

            var publish_operation1 = new PublishOperation()
                .fromProject(create_operation1.project())
                .moment(ZonedDateTime.of(2023, 3, 29, 18, 54, 32, 909, ZoneId.of("America/New_York")))
                .repositories(new Repository("http://localhost:8081/releases", "manager", "passwd"));
            publish_operation1.execute();

            var dir_json1 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp")));
            assertEquals("myapp", dir_json1.get("name"));
            var dir_files_json1 = dir_json1.getArray("files");
            assertEquals(6, dir_files_json1.size());
            assertEquals("1.2.3-SNAPSHOT", dir_files_json1.getObject(0).get("name"));
            assertEquals("maven-metadata.xml.md5", dir_files_json1.getObject(1).get("name"));
            assertEquals("maven-metadata.xml.sha1", dir_files_json1.getObject(2).get("name"));
            assertEquals("maven-metadata.xml.sha256", dir_files_json1.getObject(3).get("name"));
            assertEquals("maven-metadata.xml.sha512", dir_files_json1.getObject(4).get("name"));
            assertEquals("maven-metadata.xml", dir_files_json1.getObject(5).get("name"));

            var maven_metadata1 = new Xml2MavenMetadata();
            maven_metadata1.processXml(FileUtils.readString(new URL("http://localhost:8081/releases/test/pkg/myapp/maven-metadata.xml")));
            assertEquals(create_operation1.project().version(), maven_metadata1.getLatest());
            assertEquals(create_operation1.project().version(), maven_metadata1.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata1.getSnapshot());
            assertNull(maven_metadata1.getSnapshotTimestamp());
            assertNull(maven_metadata1.getSnapshotBuildNumber());
            assertEquals(1, maven_metadata1.getVersions().size());
            assertTrue(maven_metadata1.getVersions().contains(create_operation1.project().version()));

            var version_json1 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp/1.2.3-SNAPSHOT")));
            assertEquals("1.2.3-SNAPSHOT", version_json1.get("name"));
            var version_files_json1 = version_json1.getArray("files");
            assertEquals(15, version_files_json1.size());
            assertEquals("maven-metadata.xml.md5", version_files_json1.getObject(0).get("name"));
            assertEquals("maven-metadata.xml.sha1", version_files_json1.getObject(1).get("name"));
            assertEquals("maven-metadata.xml.sha256", version_files_json1.getObject(2).get("name"));
            assertEquals("maven-metadata.xml.sha512", version_files_json1.getObject(3).get("name"));
            assertEquals("maven-metadata.xml", version_files_json1.getObject(4).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.jar.md5", version_files_json1.getObject(5).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.jar.sha1", version_files_json1.getObject(6).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.jar.sha256", version_files_json1.getObject(7).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.jar.sha512", version_files_json1.getObject(8).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.jar", version_files_json1.getObject(9).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.pom.md5", version_files_json1.getObject(10).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.pom.sha1", version_files_json1.getObject(11).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.pom.sha256", version_files_json1.getObject(12).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.pom.sha512", version_files_json1.getObject(13).get("name"));
            assertEquals("myapp-1.2.3-20230329.225432-1.pom", version_files_json1.getObject(14).get("name"));

            var maven_snapshot_metadata1 = new Xml2MavenMetadata();
            maven_snapshot_metadata1.processXml(FileUtils.readString(new URL("http://localhost:8081/releases/test/pkg/myapp/1.2.3-SNAPSHOT/maven-metadata.xml")));
            assertEquals(create_operation1.project().version(), maven_snapshot_metadata1.getLatest());
            assertEquals(VersionNumber.UNKNOWN, maven_snapshot_metadata1.getRelease());
            assertEquals(new VersionNumber(1, 2, 3, "20230329.225432-1"), maven_snapshot_metadata1.getSnapshot());
            assertEquals("20230329.225432", maven_snapshot_metadata1.getSnapshotTimestamp());
            assertEquals(1, maven_snapshot_metadata1.getSnapshotBuildNumber());
            assertEquals(1, maven_snapshot_metadata1.getVersions().size());
            assertTrue(maven_snapshot_metadata1.getVersions().contains(create_operation1.project().version()));

            // created an updated publication
            var create_operation2 = new CreateAppOperation() {
                protected Project createProjectBlueprint() {
                    return new PublishProject(new File(workDirectory(), projectName()), packageName(), projectName(), baseName(), new VersionNumber(1, 2, 3, "SNAPSHOT"));
                }
            }
                .workDirectory(tmp2)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation2.execute();

            new CompileOperation()
                .fromProject(create_operation2.project())
                .execute();

            var jar_operation2 = new JarOperation()
                .fromProject(create_operation2.project());
            jar_operation2.execute();

            var publish_operation2 = new PublishOperation()
                .fromProject(create_operation2.project())
                .moment(ZonedDateTime.of(2023, 3, 30, 13, 17, 29, 89, ZoneId.of("America/New_York")))
                .repositories(new Repository("http://localhost:8081/releases", "manager", "passwd"));
            publish_operation2.execute();

            var dir_json2 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp")));
            assertEquals("myapp", dir_json2.get("name"));
            var dir_files_json2 = dir_json2.getArray("files");
            assertEquals(6, dir_files_json2.size());
            assertEquals("1.2.3-SNAPSHOT", dir_files_json2.getObject(0).get("name"));
            assertEquals("maven-metadata.xml.md5", dir_files_json2.getObject(1).get("name"));
            assertEquals("maven-metadata.xml.sha1", dir_files_json2.getObject(2).get("name"));
            assertEquals("maven-metadata.xml.sha256", dir_files_json2.getObject(3).get("name"));
            assertEquals("maven-metadata.xml.sha512", dir_files_json2.getObject(4).get("name"));
            assertEquals("maven-metadata.xml", dir_files_json2.getObject(5).get("name"));

            var maven_metadata2 = new Xml2MavenMetadata();
            maven_metadata2.processXml(FileUtils.readString(new URL("http://localhost:8081/releases/test/pkg/myapp/maven-metadata.xml")));
            assertEquals(create_operation2.project().version(), maven_metadata2.getLatest());
            assertEquals(create_operation2.project().version(), maven_metadata2.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata2.getSnapshot());
            assertNull(maven_metadata2.getSnapshotTimestamp());
            assertNull(maven_metadata2.getSnapshotBuildNumber());
            assertEquals(1, maven_metadata2.getVersions().size());
            assertTrue(maven_metadata2.getVersions().contains(create_operation1.project().version()));
            assertTrue(maven_metadata2.getVersions().contains(create_operation2.project().version()));

            var version_json2 = Json.parseObject(FileUtils.readString(new URL("http://localhost:8081/api/maven/details/releases/test/pkg/myapp/1.2.3-SNAPSHOT")));
            assertEquals("1.2.3-SNAPSHOT", version_json2.get("name"));
            var version_files_json2 = version_json2.getArray("files");
            assertEquals(15, version_files_json2.size());
            assertEquals("maven-metadata.xml.md5", version_files_json2.getObject(0).get("name"));
            assertEquals("maven-metadata.xml.sha1", version_files_json2.getObject(1).get("name"));
            assertEquals("maven-metadata.xml.sha256", version_files_json2.getObject(2).get("name"));
            assertEquals("maven-metadata.xml.sha512", version_files_json2.getObject(3).get("name"));
            assertEquals("maven-metadata.xml", version_files_json2.getObject(4).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.jar.md5", version_files_json2.getObject(5).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.jar.sha1", version_files_json2.getObject(6).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.jar.sha256", version_files_json2.getObject(7).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.jar.sha512", version_files_json2.getObject(8).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.jar", version_files_json2.getObject(9).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.pom.md5", version_files_json2.getObject(10).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.pom.sha1", version_files_json2.getObject(11).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.pom.sha256", version_files_json2.getObject(12).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.pom.sha512", version_files_json2.getObject(13).get("name"));
            assertEquals("myapp-1.2.3-20230330.171729-2.pom", version_files_json2.getObject(14).get("name"));

            var maven_snapshot_metadata2 = new Xml2MavenMetadata();
            maven_snapshot_metadata2.processXml(FileUtils.readString(new URL("http://localhost:8081/releases/test/pkg/myapp/1.2.3-SNAPSHOT/maven-metadata.xml")));
            assertEquals(create_operation2.project().version(), maven_snapshot_metadata2.getLatest());
            assertEquals(VersionNumber.UNKNOWN, maven_snapshot_metadata2.getRelease());
            assertEquals(new VersionNumber(1, 2, 3, "20230330.171729-2"), maven_snapshot_metadata2.getSnapshot());
            assertEquals("20230330.171729", maven_snapshot_metadata2.getSnapshotTimestamp());
            assertEquals(2, maven_snapshot_metadata2.getSnapshotBuildNumber());
            assertEquals(1, maven_snapshot_metadata2.getVersions().size());
            assertTrue(maven_snapshot_metadata2.getVersions().contains(create_operation2.project().version()));

            process.destroy();
        } finally {
            FileUtils.deleteDirectory(tmp_reposilite);
            FileUtils.deleteDirectory(tmp2);
            FileUtils.deleteDirectory(tmp1);
        }
    }

    @Test
    void testPublishSnapshotLocal()
    throws Exception {
        var tmp1 = Files.createTempDirectory("test1").toFile();
        var tmp2 = Files.createTempDirectory("test2").toFile();
        var tmp_local = Files.createTempDirectory("test").toFile();
        try {
            // create a first publication
            var create_operation1 = new CreateAppOperation() {
                protected Project createProjectBlueprint() {
                    return new PublishProject(new File(workDirectory(), projectName()), packageName(), projectName(), baseName(), new VersionNumber(1, 2, 3, "SNAPSHOT"));
                }
            }
                .workDirectory(tmp1)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation1.execute();

            new CompileOperation()
                .fromProject(create_operation1.project())
                .execute();

            var jar_operation1 = new JarOperation()
                .fromProject(create_operation1.project());
            jar_operation1.execute();

            var publish_operation1 = new PublishOperation()
                .fromProject(create_operation1.project())
                .moment(ZonedDateTime.of(2023, 3, 29, 18, 54, 32, 909, ZoneId.of("America/New_York")))
                .repositories(new Repository(tmp_local.getAbsolutePath()));
            publish_operation1.execute();

            assertEquals("""
                /test
                /test/pkg
                /test/pkg/myapp
                /test/pkg/myapp/1.2.3-SNAPSHOT
                /test/pkg/myapp/1.2.3-SNAPSHOT/maven-metadata-local.xml
                /test/pkg/myapp/1.2.3-SNAPSHOT/myapp-1.2.3-SNAPSHOT.jar
                /test/pkg/myapp/1.2.3-SNAPSHOT/myapp-1.2.3-SNAPSHOT.pom
                /test/pkg/myapp/maven-metadata-local.xml""", FileUtils.generateDirectoryListing(tmp_local));

            var maven_metadata1 = new Xml2MavenMetadata();
            maven_metadata1.processXml(FileUtils.readString(Path.of(tmp_local.getAbsolutePath(), "test", "pkg", "myapp", "maven-metadata-local.xml").toFile()));
            assertEquals(create_operation1.project().version(), maven_metadata1.getLatest());
            assertEquals(create_operation1.project().version(), maven_metadata1.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata1.getSnapshot());
            assertNull(maven_metadata1.getSnapshotTimestamp());
            assertNull(maven_metadata1.getSnapshotBuildNumber());
            assertEquals(1, maven_metadata1.getVersions().size());
            assertTrue(maven_metadata1.getVersions().contains(create_operation1.project().version()));

            var maven_snapshot_metadata1 = new Xml2MavenMetadata();
            maven_snapshot_metadata1.processXml(FileUtils.readString(Path.of(tmp_local.getAbsolutePath(), "test", "pkg", "myapp", "1.2.3-SNAPSHOT", "maven-metadata-local.xml").toFile()));
            assertEquals(create_operation1.project().version(), maven_snapshot_metadata1.getLatest());
            assertEquals(VersionNumber.UNKNOWN, maven_snapshot_metadata1.getRelease());
            assertEquals(new VersionNumber(1, 2, 3, "SNAPSHOT"), maven_snapshot_metadata1.getSnapshot());
            assertNull(maven_snapshot_metadata1.getSnapshotTimestamp());
            assertNull(maven_snapshot_metadata1.getSnapshotBuildNumber());
            assertEquals(1, maven_snapshot_metadata1.getVersions().size());
            assertTrue(maven_snapshot_metadata1.getVersions().contains(create_operation1.project().version()));

            // created an updated publication
            var create_operation2 = new CreateAppOperation() {
                protected Project createProjectBlueprint() {
                    return new PublishProject(new File(workDirectory(), projectName()), packageName(), projectName(), baseName(), new VersionNumber(1, 2, 3, "SNAPSHOT"));
                }
            }
                .workDirectory(tmp2)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation2.execute();

            new CompileOperation()
                .fromProject(create_operation2.project())
                .execute();

            var jar_operation2 = new JarOperation()
                .fromProject(create_operation2.project());
            jar_operation2.execute();

            var publish_operation2 = new PublishOperation()
                .fromProject(create_operation2.project())
                .moment(ZonedDateTime.of(2023, 3, 30, 13, 17, 29, 89, ZoneId.of("America/New_York")))
                .repositories(new Repository(tmp_local.getAbsolutePath()));
            publish_operation2.execute();

            assertEquals("""
                /test
                /test/pkg
                /test/pkg/myapp
                /test/pkg/myapp/1.2.3-SNAPSHOT
                /test/pkg/myapp/1.2.3-SNAPSHOT/maven-metadata-local.xml
                /test/pkg/myapp/1.2.3-SNAPSHOT/myapp-1.2.3-SNAPSHOT.jar
                /test/pkg/myapp/1.2.3-SNAPSHOT/myapp-1.2.3-SNAPSHOT.pom
                /test/pkg/myapp/maven-metadata-local.xml""", FileUtils.generateDirectoryListing(tmp_local));

            var maven_metadata2 = new Xml2MavenMetadata();
            maven_metadata2.processXml(FileUtils.readString(Path.of(tmp_local.getAbsolutePath(), "test", "pkg", "myapp", "maven-metadata-local.xml").toFile()));
            assertEquals(create_operation2.project().version(), maven_metadata2.getLatest());
            assertEquals(create_operation2.project().version(), maven_metadata2.getRelease());
            assertEquals(VersionNumber.UNKNOWN, maven_metadata2.getSnapshot());
            assertNull(maven_metadata2.getSnapshotTimestamp());
            assertNull(maven_metadata2.getSnapshotBuildNumber());
            assertEquals(1, maven_metadata2.getVersions().size());
            assertTrue(maven_metadata2.getVersions().contains(create_operation1.project().version()));
            assertTrue(maven_metadata2.getVersions().contains(create_operation2.project().version()));

            var maven_snapshot_metadata2 = new Xml2MavenMetadata();
            maven_snapshot_metadata2.processXml(FileUtils.readString(Path.of(tmp_local.getAbsolutePath(), "test", "pkg", "myapp", "1.2.3-SNAPSHOT", "maven-metadata-local.xml").toFile()));
            assertEquals(create_operation2.project().version(), maven_snapshot_metadata2.getLatest());
            assertEquals(VersionNumber.UNKNOWN, maven_snapshot_metadata2.getRelease());
            assertEquals(new VersionNumber(1, 2, 3, "SNAPSHOT"), maven_snapshot_metadata2.getSnapshot());
            assertNull(maven_snapshot_metadata2.getSnapshotTimestamp());
            assertNull(maven_snapshot_metadata2.getSnapshotBuildNumber());
            assertEquals(1, maven_snapshot_metadata2.getVersions().size());
            assertTrue(maven_snapshot_metadata2.getVersions().contains(create_operation2.project().version()));
        } finally {
            FileUtils.deleteDirectory(tmp_local);
            FileUtils.deleteDirectory(tmp2);
            FileUtils.deleteDirectory(tmp1);
        }
    }

    @Test
    void testPublishLocalWithBom()
    throws Exception {
        var poms = java.util.Map.of(
            "bom1:1.0.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bom1</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>1.4.0</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""");
        var metadata = java.util.Map.of(
            "b", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                    <groupId>com.example</groupId>
                    <artifactId>b</artifactId>
                    <versioning>
                        <latest>2.2.0</latest>
                        <release>2.2.0</release>
                        <versions><version>2.2.0</version></versions>
                    </versioning>
                </metadata>""");
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            String content = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
            } else if (filename.equals("maven-metadata.xml") && segments.length >= 2) {
                content = metadata.get(segments[segments.length - 2]);
            }
            if (content == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                var body = content.getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        var tmp_local = Files.createTempDirectory("bomlocal").toFile();
        var artifact_file = File.createTempFile("myapp", ".jar");
        try {
            var operation = new PublishOperation()
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .repositories(new Repository(tmp_local.getAbsolutePath()))
                .dependencyRepositories(List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/")))
                .info(new PublishInfo()
                    .groupId("test.pkg")
                    .artifactId("myapp")
                    .version(new VersionNumber(3, 3, 3)))
                .artifacts(List.of(new PublishArtifact(artifact_file, "", "jar")));
            operation.dependencies().scope(Scope.compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"))
                .include(new Dependency("com.example", "b"));
            operation.execute();

            var pom = FileUtils.readString(Path.of(tmp_local.getAbsolutePath(), "test", "pkg", "myapp", "3.3.3", "myapp-3.3.3.pom").toFile());

            // the BOM is imported through dependency management
            assertTrue(pom.contains("<dependencyManagement>"), pom);
            assertTrue(pom.contains("<artifactId>bom1</artifactId>"), pom);
            assertTrue(pom.contains("<version>1.0.0</version>"), pom);
            assertTrue(pom.contains("<type>pom</type>"), pom);
            assertTrue(pom.contains("<scope>import</scope>"), pom);

            // the covered dependency stays version-less,
            // the BOM import provides its version downstream
            assertTrue(pom.contains("<artifactId>a</artifactId>"), pom);
            assertFalse(java.util.regex.Pattern.compile("<artifactId>a</artifactId>\\s*<version>").matcher(pom).find(), pom);

            // the uncovered dependency is frozen to its resolved version
            assertTrue(java.util.regex.Pattern.compile("<artifactId>b</artifactId>\\s*<version>2\\.2\\.0</version>").matcher(pom).find(), pom);
        } finally {
            server.stop(0);
            artifact_file.delete();
            FileUtils.deleteDirectory(tmp_local);
        }
    }

    @Test
    void testResolveVersionlessDependenciesWithOverrides()
    throws Exception {
        var poms = java.util.Map.of(
            "bom1:1.0.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bom1</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>1.4.0</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""",
            "bom2:2.5.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bom2</artifactId>
                    <version>2.5.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>d</artifactId><version>4.0.0</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""");
        var metadata = java.util.Map.of(
            "b", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                    <groupId>com.example</groupId>
                    <artifactId>b</artifactId>
                    <versioning>
                        <latest>2.2.0</latest>
                        <release>2.2.0</release>
                        <versions><version>2.2.0</version></versions>
                    </versioning>
                </metadata>""");
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            String content = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
            } else if (filename.equals("maven-metadata.xml") && segments.length >= 2) {
                content = metadata.get(segments[segments.length - 2]);
            }
            if (content == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                var body = content.getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        try {
            var properties = new rife.ioc.HierarchicalProperties();
            properties.put(VersionResolution.PROPERTY_OVERRIDE_PREFIX,
                "com.example:a:1.9.9,com.example:b:1.1.1,com.example:bom2:2.5.0");

            var scopes = new DependencyScopes();
            scopes.scope(Scope.compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"))
                .include(new Dependency("com.example", "b"));
            scopes.scope(Scope.runtime)
                .include(new Bom("com.example", "bom2"));

            var operation = new PublishOperation()
                .properties(properties)
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .dependencyRepositories(List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/")))
                .dependencies(scopes);
            operation.executeResolveVersionlessDependencies();

            var compile_scope = operation.dependencies().scope(Scope.compile);
            // the override is what the build resolves, it's frozen into the
            // POM even though the BOM covers the dependency
            assertEquals(new VersionNumber(1, 9, 9), compile_scope.get(new Dependency("com.example", "a")).version());
            // an uncovered dependency freezes to its override instead of
            // the latest version
            assertEquals(new VersionNumber(1, 1, 1), compile_scope.get(new Dependency("com.example", "b")).version());
            // a version-less BOM freezes to its override
            var runtime_boms = operation.dependencies().scope(Scope.runtime).boms();
            assertEquals(new Bom("com.example", "bom2", new VersionNumber(2, 5, 0)), runtime_boms.iterator().next());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testResolveVersionlessDependenciesFreezesFlattenedConflicts()
    throws Exception {
        var poms = java.util.Map.of(
            "bomr:1.0.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bomr</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>1.0.0</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""",
            "bomp:1.0.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bomp</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>2.0.0</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""");
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            String content = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
            }
            if (content == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                var body = content.getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(Scope.runtime)
                .include(new Bom("com.example", "bomr", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            scopes.scope(Scope.provided)
                .include(new Bom("com.example", "bomp", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));

            var operation = new PublishOperation()
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .dependencyRepositories(List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/")))
                .dependencies(scopes);
            operation.executeResolveVersionlessDependencies();

            // the flattened dependency management of the POM imports the
            // runtime BOM first and would supply 1.0.0, the runtime scope
            // agrees with that so its dependency stays version-less
            assertEquals(VersionNumber.UNKNOWN, operation.dependencies().scope(Scope.runtime).get(new Dependency("com.example", "a")).version());
            // the provided scope resolves 2.0.0 through its own BOM, that
            // conflicts with the flattened list so the version is frozen
            assertEquals(new VersionNumber(2, 0, 0), operation.dependencies().scope(Scope.provided).get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testResolveVersionlessDependenciesUntouchedScopes() {
        // no network access is needed when nothing has to be resolved :
        // explicitly versioned dependencies are untouched and the test
        // scope isn't part of the publication
        var scopes = new DependencyScopes();
        scopes.scope(Scope.compile)
            .include(new Dependency("com.example", "c", new VersionNumber(5, 0, 0)));
        scopes.scope(Scope.test)
            .include(new Dependency("com.example", "t"));

        var operation = new PublishOperation()
            .dependencies(scopes);
        operation.executeResolveVersionlessDependencies();

        assertEquals(new VersionNumber(5, 0, 0), operation.dependencies().scope(Scope.compile).get(new Dependency("com.example", "c")).version());
        assertEquals(VersionNumber.UNKNOWN, operation.dependencies().scope(Scope.test).get(new Dependency("com.example", "t")).version());
    }

    static class BomPublishProject extends WebProject {
        BomPublishProject(File tmp) {
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "myapp";
            version = new VersionNumber(0, 0, 1);
            repositories = List.of(new Repository("https://example.com/resolve"));
        }
    }

    @Test
    void testFromProjectDependencyRepositories()
    throws Exception {
        var tmp = Files.createTempDirectory("test").toFile();
        try {
            var project = new BomPublishProject(tmp);
            var operation = new PublishOperation()
                .fromProject(project);
            assertEquals(project.repositories(), operation.dependencyRepositories());
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testResolveVersionlessDependencies()
    throws Exception {
        var poms = java.util.Map.of(
            "bom1:1.0.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bom1</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>a</artifactId><version>1.4.0</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""",
            "bom2:3.0.0", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>bom2</artifactId>
                    <version>3.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency><groupId>com.example</groupId><artifactId>d</artifactId><version>4.0.0</version></dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>""");
        var metadata = java.util.Map.of(
            "b", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                    <groupId>com.example</groupId>
                    <artifactId>b</artifactId>
                    <versioning>
                        <latest>2.2.0</latest>
                        <release>2.2.0</release>
                        <versions><version>2.2.0</version></versions>
                    </versioning>
                </metadata>""",
            "bom2", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                    <groupId>com.example</groupId>
                    <artifactId>bom2</artifactId>
                    <versioning>
                        <latest>3.0.0</latest>
                        <release>3.0.0</release>
                        <versions><version>3.0.0</version></versions>
                    </versioning>
                </metadata>""");
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            var segments = exchange.getRequestURI().getPath().split("/");
            var filename = segments[segments.length - 1];
            String content = null;
            if (filename.endsWith(".pom") && segments.length >= 3) {
                content = poms.get(segments[segments.length - 3] + ":" + segments[segments.length - 2]);
            } else if (filename.equals("maven-metadata.xml") && segments.length >= 2) {
                content = metadata.get(segments[segments.length - 2]);
            }
            if (content == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                var body = content.getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        try {
            var scopes = new DependencyScopes();
            scopes.scope(Scope.compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"))
                .include(new Dependency("com.example", "b"))
                .include(new Dependency("com.example", "c", new VersionNumber(5, 0, 0)));
            scopes.scope(Scope.runtime)
                .include(new Bom("com.example", "bom2"));
            scopes.scope(Scope.provided)
                // covered by the compile scope BOM through scope inheritance
                .include(new Dependency("com.example", "a"));

            var operation = new PublishOperation()
                .artifactRetriever(ArtifactRetriever.cachingInstance())
                .dependencyRepositories(List.of(new Repository("http://localhost:" + server.getAddress().getPort() + "/")))
                .dependencies(scopes);
            operation.executeResolveVersionlessDependencies();

            var compile_scope = operation.dependencies().scope(Scope.compile);
            // covered by the BOM : stays version-less for the POM,
            // the dependency management import provides the version
            assertEquals(VersionNumber.UNKNOWN, compile_scope.get(new Dependency("com.example", "a")).version());
            // not covered : resolved to its latest version
            assertEquals(new VersionNumber(2, 2, 0), compile_scope.get(new Dependency("com.example", "b")).version());
            // explicitly versioned : untouched
            assertEquals(new VersionNumber(5, 0, 0), compile_scope.get(new Dependency("com.example", "c")).version());
            // a version-less BOM is resolved to its latest version
            var runtime_boms = operation.dependencies().scope(Scope.runtime).boms();
            assertEquals(new Bom("com.example", "bom2", new VersionNumber(3, 0, 0)), runtime_boms.iterator().next());
            // the compile scope BOM also covers the provided scope,
            // its version-less dependency stays version-less
            assertEquals(VersionNumber.UNKNOWN, operation.dependencies().scope(Scope.provided).get(new Dependency("com.example", "a")).version());
        } finally {
            server.stop(0);
        }
    }
}
