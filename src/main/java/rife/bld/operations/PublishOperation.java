/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.BldVersion;
import rife.bld.dependencies.*;
import rife.bld.dependencies.exceptions.DependencyException;
import rife.bld.operations.exceptions.OperationOptionException;
import rife.bld.operations.exceptions.SignException;
import rife.bld.operations.exceptions.UploadException;
import rife.bld.publish.*;
import rife.ioc.HierarchicalProperties;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static rife.bld.dependencies.Dependency.*;
import static rife.bld.publish.MetadataBuilder.SNAPSHOT_TIMESTAMP_FORMATTER;
import static rife.tools.HttpUtils.*;
import static rife.tools.StringUtils.encodeHexLower;

/**
 * Published artifacts to a Maven repository.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.7
 */
public class PublishOperation extends AbstractOperation<PublishOperation> {
    private boolean offline_ = false;
    private HierarchicalProperties properties_ = null;
    private ArtifactRetriever retriever_ = null;
    private final HttpClient client_ = HttpClient.newHttpClient();

    private ZonedDateTime moment_ = null;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencyScopes dependencies_ = new DependencyScopes();
    private PublishInfo info_ = new PublishInfo();
    private PublishProperties publishProperties_ = new PublishProperties();
    private final List<PublishArtifact> artifacts_ = new ArrayList<>();

    /**
     * Performs the publish operation.
     *
     * @since 1.5.7
     */
    public void execute() {
        if (offline_) {
            System.out.println("Offline mode: publish is disabled");
            return;
        }

        if (repositories().isEmpty()) {
            throw new OperationOptionException("ERROR: the publication repositories should be specified");
        }

        var moment = moment_;
        if (moment == null) {
            moment = ZonedDateTime.now();
        }

        executeValidateArtifacts();

        var actual_version = info().version();

        for (var repository : repositories()) {
            System.out.println("Publishing to '" + repository.location() + "'");

            // treat a snapshot version differently
            if (info().version().isSnapshot()) {
                actual_version = executePublishSnapshotMetadata(repository, moment);
            }

            executePublishArtifacts(repository, actual_version);
            executePublishPom(repository, actual_version);
            executePublishMetadata(repository, moment);
        }
        if (!silent()) {
            System.out.println("Publishing finished successfully.");
        }
    }

    /**
     * Part of the {@link #execute} operation, validates the publishing artifacts.
     *
     * @since 1.5.10
     */
    protected void executeValidateArtifacts() {
        artifacts().removeIf(artifact -> {
            if (!artifact.file().exists()) {
                System.out.println("WARNING: Missing artifact file '" + artifact.file() + "', skipping.");
                return true;
            }
            return false;
        });
    }

    /**
     * Part of the {@link #execute} operation, publishes snapshot metadata if this
     * is a snapshot version.
     *
     * @param repository the repository to publish to
     * @param moment the timestamp at which the operation started executing
     * @return the adapted version with the snapshot timestamp and build number
     * @since 1.5.10
     */
    protected Version executePublishSnapshotMetadata(Repository repository, ZonedDateTime moment) {
        var metadata = new MetadataBuilder();

        Version actual_version;
        if (repository.isLocal()) {
            actual_version = info().version();
            metadata.snapshotLocal();
        } else {
            var snapshot_timestamp = SNAPSHOT_TIMESTAMP_FORMATTER.format(moment.withZoneSameInstant(ZoneId.of("UTC")));

            // determine which build number to use
            var snapshot_build_number = 1;
            try {
                var resolution = new VersionResolution(properties());
                var resolver = new DependencyResolver(resolution, artifactRetriever(), List.of(repository), new Dependency(info().groupId(), info().artifactId(), info().version()));
                var snapshot_meta = resolver.getSnapshotMavenMetadata();
                var build_number_meta = snapshot_meta.getSnapshotBuildNumber();
                if (build_number_meta == null) {
                    throw new DependencyException("Snapshot metadata build number doesn't exist.");
                }
                snapshot_build_number = build_number_meta + 1;
            } catch (DependencyException e) {
                // start the build number from the beginning
                System.out.println("Unable to retrieve previous snapshot metadata, using first build number.");
                System.out.println("This is expected for a first publication or for publication to a staging repository.");
            }

            // adapt the actual version used by the artifacts
            var snapshot_qualifier = snapshot_timestamp + "-" + snapshot_build_number;
            actual_version = info().version().withQualifier(snapshot_qualifier);

            // record the snapshot information in the metadata
            metadata.snapshot(moment, snapshot_build_number);
        }

        // include version information about each artifact in this snapshot
        for (var artifact : artifacts()) {
            metadata.snapshotVersions().add(new SnapshotVersion(artifact.classifier(), artifact.type(), actual_version.toString(), moment));
        }
        metadata.snapshotVersions().add(new SnapshotVersion(null, "pom", actual_version.toString(), moment));

        // publish snapshot metadata
        executePublishStringArtifact(
            repository,
            metadata
                .info(info())
                .updated(moment)
                .build(),
            info().version() + "/" + repository.getMetadataName(), false);
        return actual_version;
    }

    /**
     * Part of the {@link #execute} operation, publishes all the artifacts
     * in this operation.
     *
     * @param repository the repository to publish to
     * @param actualVersion the version that was potentially adapted if this is a snapshot
     * @since 1.5.10
     */
    protected void executePublishArtifacts(Repository repository, Version actualVersion) {
        // upload artifacts
        for (var artifact : artifacts()) {
            var artifact_name = new StringBuilder(info().artifactId()).append('-').append(actualVersion);
            if (!artifact.classifier().isEmpty()) {
                artifact_name.append('-').append(artifact.classifier());
            }
            var type = artifact.type();
            if (TYPE_JAR.equals(type) || TYPE_MODULAR_JAR.equals(type) || TYPE_CLASSPATH_JAR.equals(type)) {
                type = TYPE_JAR;
            }
            artifact_name.append('.').append(type);

            executePublishFileArtifact(repository, artifact.file(), info().version() + "/" + artifact_name);
        }
    }

    /**
     * Part of the {@link #execute} operation, publishes the Maven POM.
     *
     * @param repository the repository to publish to
     * @param actualVersion the version that was potentially adapted if this is a snapshot
     * @since 1.5.10
     */
    protected void executePublishPom(Repository repository, Version actualVersion) {
        // generate and upload pom
        executePublishStringArtifact(
            repository,
            new PomBuilder().properties(publishProperties()).info(info()).dependencies(dependencies()).build(),
            info().version() + "/" + info().artifactId() + "-" + actualVersion + ".pom", true);
    }

    /**
     * Part of the {@link #execute} operation, publishes the artifact metadata.
     *
     * @param repository the repository to publish to
     * @param moment the timestamp at which the operation started executing
     * @since 1.5.8
     */
    protected void executePublishMetadata(Repository repository, ZonedDateTime moment) {
        var current_versions = new ArrayList<Version>();
        var resolution = new VersionResolution(properties());
        var resolver = new DependencyResolver(resolution, artifactRetriever(), List.of(repository), new Dependency(info().groupId(), info().artifactId(), info().version()));
        try {
            current_versions.addAll(resolver.getMavenMetadata().getVersions());
        } catch (DependencyException e) {
            // no existing versions could be found
            System.out.println("Unable to retrieve previous artifact metadata, proceeding with empty version list.");
            System.out.println("This is expected for a first publication or for publication to a staging repository.");
        }

        // upload metadata
        executePublishStringArtifact(
            repository,
            new MetadataBuilder()
                .info(info())
                .updated(moment)
                .otherVersions(current_versions)
                .build(),
            repository.getMetadataName(), false);
    }

    /**
     * Part of the {@link #execute} operation, publishes a single artifact with
     * hashes and a potential signature.
     *
     * @param repository the repository to publish to
     * @param content    the content of the file that needs to be published
     * @param path       the path of the artifact within the artifact folder
     * @param sign       indicates whether the artifact should be signed
     * @since 1.5.18
     */
    protected void executePublishStringArtifact(Repository repository, String content, String path, boolean sign)
    throws UploadException {
        try {
            if (sign && info().signKey() != null) {
                var tmp_file = File.createTempFile(path, "gpg");
                FileUtils.writeString(content, tmp_file);
                try {
                    executeTransferArtifact(repository, executeSignFile(tmp_file), path + ".asc");
                } finally {
                    tmp_file.delete();
                }
            }

            if (!repository.isLocal()) {
                executeTransferArtifact(repository, generateHash(content, "MD5"), path + ".md5");
                executeTransferArtifact(repository, generateHash(content, "SHA-1"), path + ".sha1");
                executeTransferArtifact(repository, generateHash(content, "SHA-256"), path + ".sha256");
                executeTransferArtifact(repository, generateHash(content, "SHA-512"), path + ".sha512");
            }

            executeTransferArtifact(repository, content, path);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new UploadException(path, e);
        }
    }

    /**
     * Generates the hash for a particular string and algorithm.
     *
     * @param content   the string to generate the hash for
     * @param algorithm the hashing algorithm to use
     * @return the generates hash, encoded in lowercase hex
     * @throws NoSuchAlgorithmException when the hashing algorithm couldn't be found
     * @since 1.5.18
     */
    protected String generateHash(String content, String algorithm)
    throws NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance(algorithm);
        digest.update(content.getBytes(StandardCharsets.UTF_8));
        return encodeHexLower(digest.digest());
    }

    /**
     * Part of the {@link #execute} operation, publishes a single artifact with
     * hashes and a potential signature.
     *
     * @param repository the repository to publish to
     * @param file       the file that needs to be published
     * @param path       the path of the artifact within the artifact folder
     * @since 1.5.8
     */
    protected void executePublishFileArtifact(Repository repository, File file, String path)
    throws UploadException {
        try {
            var digest_md5 = MessageDigest.getInstance("MD5");
            var digest_sha1 = MessageDigest.getInstance("SHA-1");
            var digest_sha256 = MessageDigest.getInstance("SHA-256");
            var digest_sha512 = MessageDigest.getInstance("SHA-512");

            try (var is = Files.newInputStream(file.toPath())) {
                var buffer = new byte[1024];
                var return_value = -1;
                while (-1 != (return_value = is.read(buffer))) {
                    digest_md5.update(buffer, 0, return_value);
                    digest_sha1.update(buffer, 0, return_value);
                    digest_sha256.update(buffer, 0, return_value);
                    digest_sha512.update(buffer, 0, return_value);
                }

                if (info().signKey() != null) {
                    executeTransferArtifact(repository, executeSignFile(file), path + ".asc");
                }

                if (!repository.isLocal()) {
                    executeTransferArtifact(repository, encodeHexLower(digest_md5.digest()), path + ".md5");
                    executeTransferArtifact(repository, encodeHexLower(digest_sha1.digest()), path + ".sha1");
                    executeTransferArtifact(repository, encodeHexLower(digest_sha256.digest()), path + ".sha256");
                    executeTransferArtifact(repository, encodeHexLower(digest_sha512.digest()), path + ".sha512");
                }

                executeTransferArtifact(repository, file, path);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new UploadException(path, e);
        }
    }

    /**
     * Part of the {@link #execute} operation, generates the signature of a file.
     *
     * @param file the file whose signature will be generated
     * @since 1.5.8
     */
    protected String executeSignFile(File file)
    throws IOException, FileUtilsErrorException {
        var gpg_path = info().signGpgPath();
        if (gpg_path == null) {
            gpg_path = "gpg";
        }
        var gpg_arguments = new ArrayList<>(List.of(
            gpg_path,
            "--pinentry-mode=loopback",
            "--no-tty", "--batch", "--detach-sign", "--armor", "-o-",
            "--local-user", info().signKey()));
        if (info().signPassphrase() != null) {
            gpg_arguments.addAll(List.of("--passphrase", info().signPassphrase()));
        }
        gpg_arguments.add(file.getAbsolutePath());
        var builder = new ProcessBuilder(gpg_arguments);
        builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        builder.redirectError(ProcessBuilder.Redirect.PIPE);
        var process = builder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new SignException(file, e.getMessage());
        }

        if (process.exitValue() != 0) {
            var error = FileUtils.readString(process.getErrorStream());
            throw new SignException(file, error);
        }
        return FileUtils.readString(process.getInputStream());
    }

    /**
     * Part of the {@link #execute} operation, transfers an artifact.
     *
     * @param repository the repository to transfer to
     * @param file       the file to transfer
     * @param path       the path of the file within the artifact folder
     * @since 1.5.18
     */
    protected void executeTransferArtifact(Repository repository, File file, String path)
    throws IOException {
        if (repository.isLocal()) {
            executeStoreArtifact(repository, file, path);
        } else {
            executeUploadArtifact(repository, BodyPublishers.ofFile(file.toPath()), path);
        }
    }

    /**
     * Part of the {@link #execute} operation, transfers an artifact.
     *
     * @param repository the repository to transfer to
     * @param content    the content to transfer
     * @param path       the path of the file within the artifact folder
     * @since 1.5.18
     */
    protected void executeTransferArtifact(Repository repository, String content, String path)
    throws IOException {
        if (repository.isLocal()) {
            executeStoreArtifact(repository, content, path);
        } else {
            executeUploadArtifact(repository, BodyPublishers.ofString(content), path);
        }
    }

    /**
     * Part of the {@link #execute} operation, stores an artifact.
     *
     * @param repository the repository to store in
     * @param file       the file to store
     * @param path       the path of the file within the artifact folder
     * @since 1.5.18
     */
    protected void executeStoreArtifact(Repository repository, File file, String path)
    throws FileUtilsErrorException {
        var location = repository.getArtifactLocation(info().groupId(), info().artifactId()) + path;
        System.out.print("Storing: " + location + " ... ");
        try {
            var target = new File(location);
            target.getParentFile().mkdirs();
            FileUtils.copy(file, target);
            System.out.print("done");
        } catch (FileUtilsErrorException e) {
            System.out.print("error");
            throw e;
        } finally {
            System.out.println();
        }
    }

    /**
     * Part of the {@link #execute} operation, stores an artifact.
     *
     * @param repository the repository to store in
     * @param content    the content to store
     * @param path       the path of the file within the artifact folder
     * @since 1.5.18
     */
    protected void executeStoreArtifact(Repository repository, String content, String path)
    throws FileUtilsErrorException {
        var location = repository.getArtifactLocation(info().groupId(), info().artifactId()) + path;
        System.out.print("Storing: " + location + " ... ");
        try {
            var target = new File(location);
            target.getParentFile().mkdirs();
            FileUtils.writeString(content, target);
            System.out.print("done");
        } catch (FileUtilsErrorException e) {
            System.out.print("error");
            throw e;
        } finally {
            System.out.println();
        }
    }

    /**
     * Part of the {@link #execute} operation, uploads an artifact.
     *
     * @param repository the repository to upload to
     * @param body       the body of the file to upload
     * @param path       the path of the file within the artifact folder
     * @since 1.5.18
     */
    protected void executeUploadArtifact(Repository repository, HttpRequest.BodyPublisher body, String path) {
        var url = repository.getArtifactLocation(info().groupId(), info().artifactId()) + path;
        System.out.print("Uploading: " + url + " ... ");
        System.out.flush();
        try {
            var builder = HttpRequest.newBuilder()
                .PUT(body)
                .uri(URI.create(url))
                .header(HEADER_USER_AGENT, "bld/" + BldVersion.getVersion() +
                    " (" + System.getProperty("os.name") + "; " + System.getProperty("os.version") + "; " + System.getProperty("os.arch") + ") " +
                    "(" + System.getProperty("java.vendor") + " " + System.getProperty("java.vm.name") + "; " + System.getProperty("java.version") + "; " + System.getProperty("java.vm.version") + ")");
            if (repository.username() != null && repository.password() != null) {
                builder.header(HEADER_AUTHORIZATION, basicAuthorizationHeader(repository.username(), repository.password()));
            }
            var request = builder.build();

            HttpResponse<String> response;
            try {
                response = client_.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                System.out.print("I/O error");
                throw new UploadException(url, e);
            } catch (InterruptedException e) {
                System.out.print("interrupted");
                throw new UploadException(url, e);
            }

            if (response.statusCode() >= 200 &&
                response.statusCode() < 300) {
                System.out.print("done");
            } else {
                System.out.print("failed");
                throw new UploadException(url, response.statusCode());
            }
        } finally {
            System.out.println();
        }
    }

    /**
     * Configures a publish operation from a {@link BaseProject}.
     *
     * @param project the project to configure the publish operation from
     * @since 1.5.7
     */
    public PublishOperation fromProject(BaseProject project) {
        if (project.javaRelease() != null) {
            publishProperties()
                .mavenCompilerSource(project.javaRelease())
                .mavenCompilerTarget(project.javaRelease());
        }
        offline(project.offline());
        properties(project.properties());
        artifactRetriever(project.artifactRetriever());
        dependencies().include(project.dependencies());
        artifacts(List.of(
            new PublishArtifact(new File(project.buildDistDirectory(), project.jarFileName()), "", TYPE_JAR),
            new PublishArtifact(new File(project.buildDistDirectory(), project.sourcesJarFileName()), CLASSIFIER_SOURCES, TYPE_JAR),
            new PublishArtifact(new File(project.buildDistDirectory(), project.javadocJarFileName()), CLASSIFIER_JAVADOC, TYPE_JAR)));
        if (info().groupId() == null) {
            info().groupId(project.pkg());
        }
        if (info().artifactId() == null) {
            info().artifactId(project.name().toLowerCase());
        }
        if (info().version() == null) {
            info().version(project.version());
        }
        if (info().name() == null) {
            info().name(project.name());
        }
        return this;
    }

    /**
     * Indicates whether the operation has to run offline.
     *
     * @param flag {@code true} if the operation runs offline; or
     *             {@code false} otherwise
     * @return this operation instance
     * @since 2.0
     */
    public PublishOperation offline(boolean flag) {
        offline_ = flag;
        return this;
    }

    /**
     * Returns whether the operation has to run offline.
     *
     * @return {@code true} if the operation runs offline; or
     *         {@code false} otherwise
     * @since 2.0
     */
    public boolean offline() {
        return offline_;
    }

    /**
     * Provides the moment of publication.
     * <p>
     * If this is not provided, the publication will use the current data and time.
     *
     * @param moment the publication moment
     * @return this operation instance
     * @since 1.5.8
     */
    public PublishOperation moment(ZonedDateTime moment) {
        moment_ = moment;
        return this;
    }

    /**
     * Retrieves the moment of publication.
     *
     * @return the moment of publication; or
     * {@code null} if it wasn't provided
     * @since 1.5.8
     */
    public ZonedDateTime moment() {
        return moment_;
    }

    /**
     * Provides a repository to publish to, can be called multiple times to
     * add more repositories.
     *
     * @param repository a repository that the artifacts will be published to
     * @return this operation instance
     * @since 1.5.7
     */
    public PublishOperation repository(Repository repository) {
        repositories_.add(repository);
        return this;
    }

    /**
     * Provides repositories to publish to.
     *
     * @param repositories repositories where the artifacts will be published
     * @return this operation instance
     * @since 1.5.18
     */
    public PublishOperation repositories(Repository... repositories) {
        repositories_.addAll(List.of(repositories));
        return this;
    }

    /**
     * Provides a list of repositories to publish to.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param repositories a list of repositories where the artifacts will be published
     * @return this operation instance
     * @since 1.5
     */
    public PublishOperation repositories(List<Repository> repositories) {
        repositories_.addAll(repositories);
        return this;
    }

    /**
     * Provides scoped dependencies to reference in the publication.
     *
     * @param dependencies the dependencies that will be references in the publication
     * @return this operation instance
     * @since 1.5.7
     */
    public PublishOperation dependencies(DependencyScopes dependencies) {
        dependencies_.include(dependencies);
        return this;
    }

    /**
     * Provides the publication properties.
     *
     * @param properties the publication properties
     * @return this operation instance
     * @since 2.0
     */
    public PublishOperation publishProperties(PublishProperties properties) {
        publishProperties_ = properties;
        return this;
    }

    /**
     * Provides the publication info structure.
     *
     * @param info the publication info
     * @return this operation instance
     * @since 1.5.18
     */
    public PublishOperation info(PublishInfo info) {
        info_ = info;
        return this;
    }

    /**
     * Provides artifacts that will be published.
     *
     * @param artifacts artifacts to publish
     * @return this operation instance
     * @since 1.5.18
     */
    public PublishOperation artifacts(PublishArtifact... artifacts) {
        artifacts_.addAll(List.of(artifacts));
        return this;
    }

    /**
     * Provides a list of artifacts that will be published.
     * <p>
     * A copy will be created to allow this list to be independently modifiable.
     *
     * @param artifacts a list of artifacts to publish
     * @return this operation instance
     * @since 1.5.7
     */
    public PublishOperation artifacts(List<PublishArtifact> artifacts) {
        artifacts_.addAll(artifacts);
        return this;
    }

    /**
     * Provides the artifact retriever to use.
     *
     * @param retriever the artifact retriever
     * @return this operation instance
     * @since 1.5.18
     */
    public PublishOperation artifactRetriever(ArtifactRetriever retriever) {
        retriever_ = retriever;
        return this;
    }

    /**
     * Provides the hierarchical properties to use.
     *
     * @param properties the hierarchical properties
     * @return this operation instance
     * @since 2.0
     */
    public PublishOperation properties(HierarchicalProperties properties) {
        properties_ = properties;
        return this;
    }

    /**
     * Retrieves the repositories to which will be published.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the repositories where the artifacts will be published
     * @since 1.51.8
     */
    public List<Repository> repositories() {
        return repositories_;
    }

    /**
     * Retrieves the scoped dependencies to reference in the publication.
     * <p>
     * This is a modifiable structure that can be retrieved and changed.
     *
     * @return the scoped dependencies
     * @since 1.5.7
     */
    public DependencyScopes dependencies() {
        return dependencies_;
    }

    /**
     * Retrieves the publication properties.
     * <p>
     * This is a modifiable structure that can be retrieved and changed.
     *
     * @return the publication properties
     * @since 2.0
     */
    public PublishProperties publishProperties() {
        return publishProperties_;
    }

    /**
     * Retrieves the publication info structure.
     * <p>
     * This is a modifiable structure that can be retrieved and changed.
     *
     * @return the publication info
     * @since 1.5.7
     */
    public PublishInfo info() {
        return info_;
    }

    /**
     * Retrieves the list of artifacts that will be published.
     * <p>
     * This is a modifiable list that can be retrieved and changed.
     *
     * @return the list of artifacts to publish
     * @since 1.5.7
     */
    public List<PublishArtifact> artifacts() {
        return artifacts_;
    }

    /**
     * Returns the artifact retriever that is used.
     *
     * @return the artifact retriever
     * @since 1.5.18
     */
    public ArtifactRetriever artifactRetriever() {
        if (retriever_ == null) {
            return ArtifactRetriever.instance();
        }
        return retriever_;
    }

    /**
     * Returns the hierarchical properties that are used.
     *
     * @return the hierarchical properties
     * @since 2.0
     */
    public HierarchicalProperties properties() {
        if (properties_ == null) {
            properties_ = new HierarchicalProperties();
        }
        return properties_;
    }
}
