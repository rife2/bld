/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestVersionNumber {
    @Test
    void testInstantiation() {
        var version1 = new VersionNumber(1);
        var version10 = new VersionNumber(1, 0);
        var version100 = new VersionNumber(1, 0, 0);
        var version100s = new VersionNumber(1, 0, 0, "SNAPSHOT");
        assertEquals("1", version1.toString());
        assertEquals("1.0", version10.toString());
        assertEquals("1.0.0", version100.toString());
        assertEquals("1.0.0-SNAPSHOT", version100s.toString());
    }

    @Test
    void testParsing() {
        assertEquals(VersionNumber.parse("1"), new VersionNumber(1, 0, 0, null));
        assertEquals(VersionNumber.parse("1.0"), new VersionNumber(1, 0, 0, null));
        assertEquals(VersionNumber.parse("1.0.0"), new VersionNumber(1, 0, 0, null));

        assertEquals(VersionNumber.parse("1.2"), new VersionNumber(1, 2, 0, null));
        assertEquals(VersionNumber.parse("1.2.3"), new VersionNumber(1, 2, 3, null));

        assertEquals(VersionNumber.parse("1-rc1-SNAPSHOT"), new VersionNumber(1, 0, 0, "rc1-SNAPSHOT"));
        assertEquals(VersionNumber.parse("1.2-rc1-SNAPSHOT"), new VersionNumber(1, 2, 0, "rc1-SNAPSHOT"));
        assertEquals(VersionNumber.parse("1.2.3-rc1-SNAPSHOT"), new VersionNumber(1, 2, 3, "rc1-SNAPSHOT"));

        assertEquals(VersionNumber.parse("11.22"), new VersionNumber(11, 22, 0, null));
        assertEquals(VersionNumber.parse("11.22.33"), new VersionNumber(11, 22, 33, null));
        assertEquals(VersionNumber.parse("11.22.33-eap"), new VersionNumber(11, 22, 33, "eap"));

        assertEquals(VersionNumber.parse("11.fortyfour"), new VersionNumber(11, 0, 0, "fortyfour"));

        assertEquals(VersionNumber.parse("1.0.0.0"), new VersionNumber(1, 0, 0, "0"));
        assertEquals(VersionNumber.parse("1.0.0.0.0.0.0"), new VersionNumber(1, 0, 0, "0.0.0.0"));
        assertEquals(VersionNumber.parse("1.2.3.4-rc1-SNAPSHOT"), new VersionNumber(1, 2, 3, "4-rc1-SNAPSHOT"));
        assertEquals(VersionNumber.parse("1.2.3.4.rc1-SNAPSHOT"), new VersionNumber(1, 2, 3, "4.rc1-SNAPSHOT"));

        assertEquals(VersionNumber.parse("1.2.3_4"), new VersionNumber(1, 2, 0, "3_4"));
        assertEquals(VersionNumber.parse("1.54b"), new VersionNumber(1, 0, 0, "54b"));
    }

    @Test
    void testIsSnapshot() {
        assertFalse(new VersionNumber(1, 0, 0, null).isSnapshot());
        assertFalse(new VersionNumber(1, 2, 3, null).isSnapshot());
        assertTrue(new VersionNumber(1, 0, 0, "rc1-SNAPSHOT").isSnapshot());
        assertTrue(new VersionNumber(1, 2, 0, "rc1-SNAPSHOT").isSnapshot());
        assertTrue(new VersionNumber(1, 2, 3, "rc1-SNAPSHOT").isSnapshot());
        assertFalse(new VersionNumber(11, 22, 33, "eap").isSnapshot());
        assertFalse(new VersionNumber(11, 0, 0, "fortyfour").isSnapshot());
        assertTrue(new VersionNumber(1, 2, 3, "4-rc1-SNAPSHOT").isSnapshot());
    }

    @Test
    void testInvalidParsed() {
        assertEquals(VersionNumber.parse(null), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse(""), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("foo"), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("1."), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("1.2.3-"), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("."), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("_"), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("-"), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse(".1"), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("a.1"), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("1_2"), VersionNumber.UNKNOWN);
        assertEquals(VersionNumber.parse("1_2_2"), VersionNumber.UNKNOWN);
    }

    @Test
    void testAccessors() {
        var version = new VersionNumber(1, 2, 3, "beta");

        assertEquals(1, version.major());
        assertEquals(2, version.minor());
        assertEquals(3, version.revision());
        assertEquals("beta", version.qualifier());
    }

    @Test
    void testStringRepresentation() {
        assertEquals(VersionNumber.parse("1.0").toString(), "1.0");
        assertEquals(VersionNumber.parse("1.2.3").toString(), "1.2.3");
        assertEquals(VersionNumber.parse("1.2.3-4").toString(), "1.2.3-4");
        assertEquals(VersionNumber.parse("1.2.3.4").toString(), "1.2.3.4");
        assertEquals(VersionNumber.parse("1-rc-1").toString(), "1-rc-1");
        assertEquals(VersionNumber.parse("1.2.3-rc-1").toString(), "1.2.3-rc-1");
        assertEquals(VersionNumber.parse("1.2.3.rc-1").toString(), "1.2.3.rc-1");
    }

    @Test
    void testEquality() {
        var version = new VersionNumber(1, 1, 1, null);
        var qualified = new VersionNumber(1, 1, 1, "beta-2");

        assertEquals(new VersionNumber(1, 1, 1, null), version);
        assertNotEquals(new VersionNumber(2, 1, 1, null), version);
        assertNotEquals(new VersionNumber(1, 2, 1, null), version);
        assertNotEquals(new VersionNumber(1, 1, 2, null), version);
        assertNotEquals(new VersionNumber(1, 1, 1, "rc"), version);
        assertEquals(new VersionNumber(1, 1, 1, "beta-2"), qualified);
        assertNotEquals(new VersionNumber(1, 1, 1, "beta-3"), qualified);
    }

    @Test
    void testComparison() {
        assertEquals(0, new VersionNumber(1, 1, 1, null).compareTo(new VersionNumber(1, 1, 1, null)));

        assertTrue(new VersionNumber(2, 1, 1, null).compareTo(new VersionNumber(1, 1, 1, null)) > 0);
        assertTrue(new VersionNumber(1, 2, 1, null).compareTo(new VersionNumber(1, 1, 1, null)) > 0);
        assertTrue(new VersionNumber(1, 1, 2, null).compareTo(new VersionNumber(1, 1, 1, null)) > 0);

        assertTrue(new VersionNumber(1, 1, 1, "rc").compareTo(new VersionNumber(1, 1, 1, null)) < 0);
        assertTrue(new VersionNumber(1, 1, 1, "beta").compareTo(new VersionNumber(1, 1, 1, "alpha")) > 0);
        assertTrue(new VersionNumber(1, 1, 1, "RELEASE").compareTo(new VersionNumber(1, 1, 1, "beta")) > 0);
        assertTrue(new VersionNumber(1, 1, 1, "SNAPSHOT").compareTo(new VersionNumber(1, 1, 1, null)) < 0);

        assertTrue(new VersionNumber(1, 1, 1, null).compareTo(new VersionNumber(2, 1, 1, null)) < 0);
        assertTrue(new VersionNumber(1, 1, 1, null).compareTo(new VersionNumber(1, 2, 1, null)) < 0);
        assertTrue(new VersionNumber(1, 1, 1, null).compareTo(new VersionNumber(1, 1, 2, null)) < 0);
        assertTrue(new VersionNumber(1, 1, 1, null).compareTo(new VersionNumber(1, 1, 1, "rc")) > 0);
        assertTrue(new VersionNumber(1, 1, 1, "alpha").compareTo(new VersionNumber(1, 1, 1, "beta")) < 0);
        assertTrue(new VersionNumber(1, 1, 1, "beta").compareTo(new VersionNumber(1, 1, 1, "RELEASE")) < 0);
    }

    @Test
    void testBaseVersion() {
        assertEquals(new VersionNumber(1, 2, 3, null).getBaseVersion(), new VersionNumber(1, 2, 3, null));
        assertEquals(new VersionNumber(1, 2, 3, "beta").getBaseVersion(), new VersionNumber(1, 2, 3, null));
    }

    @Test
    void testWithQualifier() {
        assertEquals(new VersionNumber(1, 2, 3, null).withQualifier("SNAPSHOT"), new VersionNumber(1, 2, 3, "SNAPSHOT"));
        assertEquals(new VersionNumber(1, 2, 3, "beta").withQualifier("SNAPSHOT"), new VersionNumber(1, 2, 3, "SNAPSHOT"));
        assertEquals(new VersionNumber(1, 2, 4, "beta").withQualifier(null), new VersionNumber(1, 2, 4, null));
    }
}
