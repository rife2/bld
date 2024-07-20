/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestVersion {
    @Test
    void testParsing() {
        assertEquals(Version.parse("1"), new VersionNumber(1, 0, 0, null));
        assertEquals(Version.parse("1.0"), new VersionNumber(1, 0, 0, null));
        assertEquals(Version.parse("1.0.0"), new VersionNumber(1, 0, 0, null));

        assertEquals(Version.parse("1.2"), new VersionNumber(1, 2, 0, null));
        assertEquals(Version.parse("1.2.3"), new VersionNumber(1, 2, 3, null));

        assertEquals(Version.parse("1-rc1-SNAPSHOT"), new VersionNumber(1, 0, 0, "rc1-SNAPSHOT"));
        assertEquals(Version.parse("1.2-rc1-SNAPSHOT"), new VersionNumber(1, 2, 0, "rc1-SNAPSHOT"));
        assertEquals(Version.parse("1.2.3-rc1-SNAPSHOT"), new VersionNumber(1, 2, 3, "rc1-SNAPSHOT"));

        assertEquals(Version.parse("11.22"), new VersionNumber(11, 22, 0, null));
        assertEquals(Version.parse("11.22.33"), new VersionNumber(11, 22, 33, null));
        assertEquals(Version.parse("11.22.33-eap"), new VersionNumber(11, 22, 33, "eap"));

        assertEquals(Version.parse("11.fortyfour"), new VersionNumber(11, 0, 0, "fortyfour"));

        assertEquals(Version.parse("1.0.0.0"), new VersionNumber(1, 0, 0, "0"));
        assertEquals(Version.parse("1.0.0.0.0.0.0"), new VersionNumber(1, 0, 0, "0.0.0.0"));
        assertEquals(Version.parse("1.2.3.4-rc1-SNAPSHOT"), new VersionNumber(1, 2, 3, "4-rc1-SNAPSHOT"));
        assertEquals(Version.parse("1.2.3.4.rc1-SNAPSHOT"), new VersionNumber(1, 2, 3, "4.rc1-SNAPSHOT"));

        assertEquals(Version.parse("1.2.3_4"), new VersionNumber(1, 2, 0, "3_4"));
        assertEquals(Version.parse("1.54b"), new VersionNumber(1, 0, 0, "54b"));

        assertEquals(Version.parse("2024-02"), new VersionNumber(2024, null, null, "02"));
        assertEquals(Version.parse("2.0-05"), new VersionNumber(2, 0, null, "05"));
        assertEquals(Version.parse("2024.02"), new VersionNumber(2024, null, null, "02", "."));
        assertEquals(Version.parse("2.0.05"), new VersionNumber(2, 0, null, "05", "."));

        assertEquals(Version.parse("v3-rev20240514-2.0.0"), new VersionGeneric("v3-rev20240514-2.0.0"));
    }

    @Test
    void testInvalidParsed() {
        assertEquals(Version.parse(null), VersionNumber.UNKNOWN);
        assertEquals(Version.parse(""), VersionNumber.UNKNOWN);
        assertEquals(Version.parse("foo"), new VersionGeneric("foo"));
        assertEquals(Version.parse("1."), new VersionGeneric("1."));
        assertEquals(Version.parse("1.2.3-"), new VersionGeneric("1.2.3-"));
        assertEquals(Version.parse("."), new VersionGeneric("."));
        assertEquals(Version.parse("_"), new VersionGeneric("_"));
        assertEquals(Version.parse("-"), new VersionGeneric("-"));
        assertEquals(Version.parse(".1"), new VersionGeneric(".1"));
        assertEquals(Version.parse("a.1"), new VersionGeneric("a.1"));
        assertEquals(Version.parse("1_2"), new VersionGeneric("1_2"));
        assertEquals(Version.parse("1_2_2"), new VersionGeneric("1_2_2"));
    }

    @Test
    void testStringRepresentation() {
        assertEquals(Version.parse("1.0").toString(), "1.0");
        assertEquals(Version.parse("1.2.3").toString(), "1.2.3");
        assertEquals(Version.parse("1.2.3-4").toString(), "1.2.3-4");
        assertEquals(Version.parse("1.2.3.4").toString(), "1.2.3.4");
        assertEquals(Version.parse("1-rc-1").toString(), "1-rc-1");
        assertEquals(Version.parse("1.2.3-rc-1").toString(), "1.2.3-rc-1");
        assertEquals(Version.parse("1.2.3.rc-1").toString(), "1.2.3.rc-1");
    }
}
