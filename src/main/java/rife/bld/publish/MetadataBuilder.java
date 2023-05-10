/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import rife.bld.dependencies.VersionNumber;
import rife.template.TemplateFactory;
import rife.tools.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Provides the functionalities to build a Maven metadata xml file.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.7
 */
public class MetadataBuilder {
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter SNAPSHOT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss");

    private PublishInfo info_ = null;
    private ZonedDateTime timestamp_ = null;
    private final Set<VersionNumber> otherVersions_ = new HashSet<>();
    private ZonedDateTime snapshotTimestamp_ = null;
    private Integer snapshotBuildNumber_ = null;
    private boolean snapshotLocal_ = false;
    private final List<SnapshotVersion> snapshotVersions_ = new ArrayList<>();

    /**
     * Provides the publishing info to build the metadata for.
     *
     * @param info the publishing info to use
     * @return this {@code MetadataBuilder} instance
     * @since 1.5.7
     */
    public MetadataBuilder info(PublishInfo info) {
        info_ = info;
        return this;
    }

    /**
     * Retrieves the publishing info to build the metadata for.
     *
     * @return the publishing info
     * @since 1.5.7
     */
    public PublishInfo info() {
        return info_;
    }

    /**
     * Provides the updated timestamp for the metadata.
     *
     * @param timestamp the publishing updated timestamp
     * @return this {@code MetadataBuilder} instance
     * @since 1.5.7
     */
    public MetadataBuilder updated(ZonedDateTime timestamp) {
        timestamp_ = timestamp;
        return this;
    }

    /**
     * Retrieves the updated timestamp for the metadata.
     *
     * @return the publishing updated timestamp
     * @since 1.5.7
     */
    public ZonedDateTime updated() {
        return timestamp_;
    }

    /**
     * Provides the other versions for the metadata.
     *
     * @param otherVersions the other versions to include
     * @return this {@code MetadataBuilder} instance
     * @since 1.5.8
     */
    public MetadataBuilder otherVersions(Collection<VersionNumber> otherVersions) {
        otherVersions_.addAll(otherVersions);
        return this;
    }

    /**
     * Retrieves the other versions for the metadata.
     * <p>
     * This is a modifiable set that can be retrieved and changed.
     *
     * @return the other versions
     * @since 1.5.8
     */
    public Set<VersionNumber> otherVersions() {
        return otherVersions_;
    }

    /**
     * Provides the snapshot details for this metadata, this will switch it to
     * snapshot formatting, which is different from main artifact metadata.
     *
     * @param timestamp   the snapshot timestamp
     * @param buildNumber the snapshot build number
     * @return this {@code MetadataBuilder} instance
     * @since 1.5.8
     */
    public MetadataBuilder snapshot(ZonedDateTime timestamp, Integer buildNumber) {
        snapshotTimestamp_ = timestamp;
        snapshotBuildNumber_ = buildNumber;
        snapshotLocal_ = false;
        return this;
    }

    /**
     * Retrieves the snapshot timestamp.
     *
     * @return the snapshot timestamp; or
     * {@code null} if this is not snapshot metadata
     * @since 1.5.8
     */
    public ZonedDateTime snapshotTimestamp() {
        return snapshotTimestamp_;
    }

    /**
     * Retrieves the snapshot build number.
     *
     * @return the snapshot build number; or
     * {@code null} if this is not snapshot metadata
     * @since 1.5.8
     */
    public Integer snapshotBuildNumber() {
        return snapshotBuildNumber_;
    }

    /**
     * Indicates this is local snapshot metadata, this will switch it to
     * snapshot formatting, which is different from main artifact metadata.
     *
     * @return this {@code MetadataBuilder} instance
     * @since 1.5.8
     */
    public MetadataBuilder snapshotLocal() {
        snapshotTimestamp_ = null;
        snapshotBuildNumber_ = null;
        snapshotLocal_ = true;
        return this;
    }

    /**
     * Indicates whether this is a local snapshot.
     *
     * @return {@code true} if this is a local snapshot; or
     * {@code false} otherwise
     * @since 1.5.10
     */
    public boolean isSnapshotLocal() {
        return snapshotLocal_;
    }

    /**
     * Provides the snapshot versions for the metadata.
     *
     * @param snapshotVersions the snapshot versions to include
     * @return this {@code MetadataBuilder} instance
     * @since 1.5.8
     */
    public MetadataBuilder snapshotVersions(Collection<SnapshotVersion> snapshotVersions) {
        snapshotVersions_.addAll(snapshotVersions);
        return this;
    }

    /**
     * Retrieves the snapshot versions for the metadata.
     * <p>
     * This is a modifiable set that can be retrieved and changed.
     *
     * @return the snapshot versions
     * @since 1.5.8
     */
    public Collection<SnapshotVersion> snapshotVersions() {
        return snapshotVersions_;
    }

    /**
     * Builds the Maven metadata xml file.
     *
     * @return the generated Maven metadata xml file as a string
     * @since 1.5.7
     */
    public String build() {
        var t = TemplateFactory.XML.get("bld.maven_metadata_blueprint");

        var info = info();
        if (info != null) {
            t.setValueEncoded("groupId", Objects.requireNonNullElse(info.groupId(), ""));
            t.setValueEncoded("artifactId", Objects.requireNonNullElse(info.artifactId(), ""));
        }
        if (snapshotLocal_ || (snapshotTimestamp() != null && snapshotBuildNumber() != null)) {
            t.setValueEncoded("mainVersion", Objects.requireNonNullElse(info.version(), ""));
            t.setBlock("mainVersion-tag");

            if (snapshotLocal_) {
                t.setBlock("snapshot-tag", "snapshot-local-tag");
            } else {
                t.setValueEncoded("snapshot-timestamp", SNAPSHOT_TIMESTAMP_FORMATTER.format(snapshotTimestamp().withZoneSameInstant(ZoneId.of("UTC"))));
                t.setValueEncoded("snapshot-buildNumber", snapshotBuildNumber());
                t.setBlock("snapshot-tag");
            }

            if (!snapshotVersions().isEmpty()) {
                for (var snapshot_version : snapshotVersions()) {
                    if (snapshot_version.classifier().isEmpty()) {
                        t.blankValue("snapshotVersionClassifier-tag");
                    } else {
                        t.setValueEncoded("snapshotVersion-classifier", snapshot_version.classifier());
                        t.setBlock("snapshotVersionClassifier-tag");
                    }
                    t.setValueEncoded("snapshotVersion-extension", Objects.requireNonNullElse(snapshot_version.extension(), ""));
                    t.setValueEncoded("snapshotVersion-value", Objects.requireNonNullElse(snapshot_version.value(), ""));
                    t.setValueEncoded("snapshotVersion-updated", Objects.requireNonNullElse(TIMESTAMP_FORMATTER.format(snapshot_version.updated().withZoneSameInstant(ZoneId.of("UTC"))), ""));

                    t.appendBlock("snapshotVersions", "snapshotVersion");
                }
                t.setBlock("snapshotVersions-tag");
            }
        } else {
            var versions = new TreeSet<>(otherVersions());
            if (info != null && info.version() != null) {
                t.setValueEncoded("latestVersion", Objects.requireNonNullElse(info.version(), ""));
                t.setValueEncoded("releaseVersion", Objects.requireNonNullElse(info.version(), ""));
                t.setBlock("versionLatest-tag");

                versions.add(info.version());
            }
            for (var version : versions) {
                t.setValueEncoded("version", version);
                t.appendBlock("versions", "version");
            }
            if (t.isValueSet("versions")) {
                t.setBlock("versions-tag");
            }
        }
        if (updated() != null) {
            t.setValueEncoded("lastUpdated", Objects.requireNonNullElse(TIMESTAMP_FORMATTER.format(updated().withZoneSameInstant(ZoneId.of("UTC"))), ""));
            t.setBlock("lastUpdated-tag");
        }

        return StringUtils.stripBlankLines(t.getContent());
    }
}
