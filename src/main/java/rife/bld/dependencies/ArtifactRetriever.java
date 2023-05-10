/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.security.MessageDigest;
import java.util.*;

import static rife.tools.HttpUtils.HEADER_AUTHORIZATION;
import static rife.tools.HttpUtils.basicAuthorizationHeader;
import static rife.tools.StringUtils.encodeHexLower;

/**
 * Retrieves artifact data.
 * <p>
 * To instantiate, use either {@link #instance()} for direct retrieval of
 * each request, or {@link #cachingInstance()} where previous retrievals
 * of remote string content will be cached for faster future retrieval.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.18
 */
public abstract class ArtifactRetriever {
    private final static ArtifactRetriever UNCACHED = new ArtifactRetriever() {
        String getCached(RepositoryArtifact artifact) {
            return null;
        }

        void cache(RepositoryArtifact artifact, String content) {
        }
    };

    /**
     * Gets an artifact retriever that does direct retrieval of each request.
     *
     * @return the direct retrieval instance
     * @since 1.5.18
     */
    public static ArtifactRetriever instance() {
        return UNCACHED;
    }

    /**
     * Creates a caching artifact retriever where previous retrievals
     * of remote string content will be cached for faster future retrieval.
     *
     * @return a caching instance
     * @since 1.5.18
     */
    public static ArtifactRetriever cachingInstance() {
        return new ArtifactRetriever() {
            private final Map<RepositoryArtifact, String> artifactCache = new HashMap<>();

            String getCached(RepositoryArtifact artifact) {
                return artifactCache.get(artifact);
            }

            void cache(RepositoryArtifact artifact, String content) {
                artifactCache.put(artifact, content);
            }

        };
    }

    private ArtifactRetriever() {
    }

    abstract String getCached(RepositoryArtifact artifact);

    abstract void cache(RepositoryArtifact artifact, String content);

    /**
     * Reads the contents of an artifact as a string.
     *
     * @param artifact the artifact who's content to retrieve
     * @return the string content of the artifact
     * @throws FileUtilsErrorException when an error occurred when reading the contents
     * @since 1.5.18
     */
    public String readString(RepositoryArtifact artifact)
    throws FileUtilsErrorException {
        if (artifact.repository().isLocal()) {
            return FileUtils.readString(new File(artifact.location()));
        } else {
            var cached = getCached(artifact);
            if (cached != null) {
                return cached;
            }

            try {
                var connection = new URL(artifact.location()).openConnection();
                connection.setUseCaches(false);
                if (artifact.repository().username() != null && artifact.repository().password() != null) {
                    connection.setRequestProperty(
                        HEADER_AUTHORIZATION,
                        basicAuthorizationHeader(artifact.repository().username(), artifact.repository().password()));
                }
                try (var input_stream = connection.getInputStream()) {
                    var result = FileUtils.readString(input_stream);
                    cache(artifact, result);
                    return result;
                }
            } catch (IOException e) {
                throw new FileUtilsErrorException("Error while reading URL '" + artifact.location() + ".", e);
            }
        }
    }

    /**
     * Transfers artifact into the provided directory.
     * <p>
     * The destination directory must exist and be writable.
     *
     * @param artifact  the artifact to transfer
     * @param directory the directory to transfer the artifact into
     * @return {@code true} when the artifact is present in the directory (it could already have been
     * there and be validated as correct); or {@code false} when the artifact couldn't be transferred
     * @throws IOException             when an error occurred during the transfer
     * @throws FileUtilsErrorException when an error occurred during the transfer
     * @since 1.5.18
     */
    public boolean transferIntoDirectory(RepositoryArtifact artifact, File directory)
    throws IOException, FileUtilsErrorException {
        if (directory == null) throw new IllegalArgumentException("directory can't be null");
        if (!directory.exists()) throw new IllegalArgumentException("directory '" + directory + "' doesn't exit");
        if (!directory.canWrite()) throw new IllegalArgumentException("directory '" + directory + "' can't be written to");
        if (!directory.isDirectory()) throw new IllegalArgumentException("directory '" + directory + "' is not a directory");

        var download_filename = artifact.location().substring(artifact.location().lastIndexOf("/") + 1);
        var download_file = new File(directory, download_filename);
        System.out.print("Downloading: " + artifact.location() + " ... ");
        System.out.flush();
        try {
            if (artifact.repository().isLocal()) {
                var source = new File(artifact.location());
                if (source.exists()) {
                    FileUtils.copy(source, download_file);
                    System.out.print("done");
                    return true;
                } else {
                    System.out.print("not found");
                    return false;
                }
            } else {
                try {
                    if (download_file.exists() && download_file.canRead()) {
                        if (checkHash(artifact, download_file, ".sha256", "SHA-256") ||
                            checkHash(artifact, download_file, ".md5", "MD5")) {
                            System.out.print("exists");
                            return true;
                        }
                    }

                    var connection = new URL(artifact.location()).openConnection();
                    connection.setUseCaches(false);
                    if (artifact.repository().username() != null && artifact.repository().password() != null) {
                        connection.setRequestProperty(
                            HEADER_AUTHORIZATION,
                            basicAuthorizationHeader(artifact.repository().username(), artifact.repository().password()));
                    }
                    try (var input_stream = connection.getInputStream()) {
                        var readableByteChannel = Channels.newChannel(input_stream);
                        try (var fileOutputStream = new FileOutputStream(download_file)) {
                            var fileChannel = fileOutputStream.getChannel();
                            fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                            System.out.print("done");
                            return true;
                        }
                    }
                } catch (FileNotFoundException e) {
                    System.out.print("not found");
                    return false;
                }
            }
        } finally {
            System.out.println();
        }
    }

    private boolean checkHash(RepositoryArtifact artifact, File downloadFile, String extension, String algorithm) {
        try {
            var hash_sum = readString(artifact.appendPath(extension));
            var digest = MessageDigest.getInstance(algorithm);
            digest.update(FileUtils.readBytes(downloadFile));
            return hash_sum.equals(encodeHexLower(digest.digest()));
        } catch (Exception e) {
            // no-op, the hash file couldn't be found or calculated, so it couldn't be checked
        }
        return false;
    }
}
