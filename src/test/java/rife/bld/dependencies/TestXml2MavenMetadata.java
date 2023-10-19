/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Test;
import rife.resources.ResourceFinderClasspath;
import rife.resources.exceptions.ResourceFinderErrorException;

import static org.junit.jupiter.api.Assertions.*;

public class TestXml2MavenMetadata {
    @Test
    void testInstantiation() {
        var metadata = new Xml2MavenMetadata();
        assertNotNull(metadata);
    }

    @Test
    void testParse1()
    throws ResourceFinderErrorException {
        var resource_finder = ResourceFinderClasspath.instance();
        var metadata = new Xml2MavenMetadata();
        assertTrue(metadata.processXml(resource_finder.getContent("maven-metadata1.txt")));
        assertEquals(metadata.getLatest(), new VersionNumber(1, 1, 1, "SNAPSHOT"));
        assertEquals(metadata.getRelease(), VersionNumber.UNKNOWN);
        assertEquals(metadata.getSnapshot(), new VersionNumber(1, 1, 1, "SNAPSHOT"));
    }

    @Test
    void testParse2()
    throws ResourceFinderErrorException {
        var resource_finder = ResourceFinderClasspath.instance();
        var metadata = new Xml2MavenMetadata();
        assertTrue(metadata.processXml(resource_finder.getContent("maven-metadata2.txt")));
        assertEquals(metadata.getLatest(), new VersionNumber(1, 1, 1, "SNAPSHOT"));
        assertEquals(metadata.getRelease(), VersionNumber.UNKNOWN);
        assertEquals(metadata.getSnapshot(), new VersionNumber(1, 1, 1, "SNAPSHOT"));
    }
}
