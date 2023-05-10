/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import java.time.ZonedDateTime;

/**
 * Contains the information to describe a particular artifact in a snapshot version.
 *
 * @param classifier the classifier of the artifact
 * @param extension  the file extension of the artifact
 * @param value      the version value
 * @param updated    the timestamp of publication
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.8
 */
public record SnapshotVersion(String classifier, String extension, String value, ZonedDateTime updated) {
    public SnapshotVersion(String classifier, String extension, String value, ZonedDateTime updated) {
        this.classifier = (classifier == null ? "" : classifier);
        this.extension = (extension == null ? "jar" : extension);
        this.value = value;
        this.updated = updated;
    }
}