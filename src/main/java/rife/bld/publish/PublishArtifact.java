/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import java.io.File;

/**
 * Contains the information about an artifact that will be published.
 *
 * @param file       the file that will be published
 * @param classifier the classifier of the artifact
 * @param type       the type of the artifact
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.8
 */
public record PublishArtifact(File file, String classifier, String type) {
    public PublishArtifact(File file, String classifier, String type) {
        this.file = file;
        this.classifier = (classifier == null ? "" : classifier);
        this.type = (type == null ? "jar" : type);
    }
}
