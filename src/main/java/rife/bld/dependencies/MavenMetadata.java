/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.List;

/**
 * Provides Maven metadata information
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.8
 */
public interface MavenMetadata {
    /**
     * Returns latest version in the metadata.
     *
     * @return the latest version
     * @since 1.5.8
     */
    Version getLatest();

    /**
     * Returns release version in the metadata.
     *
     * @return the release version
     * @since 1.5.8
     */
    Version getRelease();

    /**
     * Returns snapshot version in the metadata.
     *
     * @return the snapshot version
     * @since 1.5.8
     */
    Version getSnapshot();

    /**
     * Returns snapshot timestamp in the metadata.
     *
     * @return the snapshot timestamp
     * @since 1.5.8
     */
    String getSnapshotTimestamp();

    /**
     * Returns snapshot build number in the metadata.
     *
     * @return the snapshot build number
     * @since 1.5.8
     */
    Integer getSnapshotBuildNumber();

    /**
     * Returns all the release or snapshot versions in the metadata.
     *
     * @return the version list
     * @since 1.5.8
     */
    List<Version> getVersions();
}
