/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.bld.BldVersion;
import rife.tools.FileUtils;
import rife.tools.HttpUtils;
import rife.tools.Product;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final int TRANSFER_CHUNK_SIZE = 128 * 1024;

    private final static ArtifactRetriever UNCACHED = new ArtifactRetriever() {
        String getCached(RepositoryArtifact artifact) {
            return null;
        }

        void cache(RepositoryArtifact artifact, String content) {
        }

        boolean isCaching() {
            return false;
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
            private final Map<RepositoryArtifact, String> artifactCache = new ConcurrentHashMap<>();

            String getCached(RepositoryArtifact artifact) {
                return artifactCache.get(artifact);
            }

            void cache(RepositoryArtifact artifact, String content) {
                artifactCache.put(artifact, content);
            }

            boolean isCaching() {
                return true;
            }
        };
    }

    private ArtifactRetriever() {
    }

    abstract String getCached(RepositoryArtifact artifact);

    abstract void cache(RepositoryArtifact artifact, String content);

    abstract boolean isCaching();

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
                var connection = connectArtifact(artifact);
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
        var transfer = TransferOutput.instance().start(artifact.location());
        var status = "";
        try {
            if (artifact.repository().isLocal()) {
                var source = new File(artifact.location());
                if (source.exists()) {
                    FileUtils.copy(source, download_file);
                    status = "done";
                    return true;
                } else {
                    status = "not found";
                    return false;
                }
            } else {
                try {
                    if (download_file.exists() && download_file.canRead()) {
                        if (checkHash(artifact, download_file, ".sha256", "SHA-256") ||
                            checkHash(artifact, download_file, ".md5", "MD5")) {
                            status = "exists";
                            return true;
                        }
                    }

                    var connection = connectArtifact(artifact);
                    var content_length = connection.getContentLengthLong();
                    try (var input_stream = connection.getInputStream()) {
                        var readableByteChannel = Channels.newChannel(input_stream);
                        try (var fileOutputStream = new FileOutputStream(download_file)) {
                            var fileChannel = fileOutputStream.getChannel();
                            var position = 0L;
                            long transferred;
                            while ((transferred = fileChannel.transferFrom(readableByteChannel, position, TRANSFER_CHUNK_SIZE)) > 0) {
                                position += transferred;
                                transfer.progress(position, content_length);
                            }

                            status = "done";
                            return true;
                        }
                    }
                } catch (FileNotFoundException e) {
                    status = "not found";
                    return false;
                }
            }
        } finally {
            transfer.finish(status);
        }
    }

    static final int RETRIEVAL_ATTEMPTS = 3;
    static final long RETRIEVAL_RETRY_DELAY_MS = 1000L;
    static final long RATE_LIMIT_RETRY_DELAY_MS = 5000L;

    private static URLConnection connectArtifact(RepositoryArtifact artifact)
    throws IOException {
        return connectArtifact(artifact, RETRIEVAL_ATTEMPTS, RETRIEVAL_RETRY_DELAY_MS, RATE_LIMIT_RETRY_DELAY_MS);
    }

    /**
     * Opens a connection to a remote artifact and retries transient
     * failures: connection issues, server errors and rate limiting.
     * The connection is returned with its input stream already opened.
     */
    static URLConnection connectArtifact(RepositoryArtifact artifact, int attempts, long delayMs, long rateLimitDelayMs)
    throws IOException {
        for (var attempt = 1; ; ++attempt) {
            var connection = openUrlConnection(artifact);
            try {
                connection.getInputStream();
                return connection;
            } catch (FileNotFoundException e) {
                // an artifact that isn't there will not appear by retrying,
                // and resolution legitimately probes repositories without it
                throw e;
            } catch (IOException e) {
                var code = -1;
                if (connection instanceof HttpURLConnection http) {
                    try {
                        code = http.getResponseCode();
                    } catch (IOException unavailable) {
                        // the failure happened before a status line arrived
                    }
                }
                // other client errors, like failing authentication, will
                // fail the same way again
                if (attempt >= attempts ||
                    (code >= 400 && code < 500 && code != 408 && code != 429)) {
                    throw e;
                }
                System.err.println("Artifact retrieval issue (" + e.getMessage() + "), retrying ...");
                try {
                    // a 429 is the server asking to slow down, wait longer
                    Thread.sleep((code == 429 ? rateLimitDelayMs : delayMs) * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    static final int CONNECT_TIMEOUT_MS = 10_000;
    static final int READ_TIMEOUT_MS = 60_000;

    private static URLConnection openUrlConnection(RepositoryArtifact artifact) throws IOException {
        var connection = new URL(artifact.location()).openConnection();
        connection.setUseCaches(false);
        // without these a host that accepts packets but never answers
        // stalls resolution for the platform default, which can be minutes
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty(HttpUtils.HEADER_USER_AGENT, Product.BLD.toUserAgent(BldVersion.getVersion()));
        if (artifact.repository().username() != null && artifact.repository().password() != null) {
            connection.setRequestProperty(
                HEADER_AUTHORIZATION,
                basicAuthorizationHeader(artifact.repository().username(), artifact.repository().password()));
        }
        return connection;
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
