/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

abstract class AbstractVersionTest {
    protected static final int X_LT_Y = -1;
    protected static final int X_EQ_Y = 0;
    protected static final int X_GT_Y = 1;

    protected abstract Version newVersion(String version);

    protected void assertOrder(int expected, String version1, String version2) {
        Version v1 = newVersion(version1);
        Version v2 = newVersion(version2);

        if (expected > 0) {
            assertEquals(1, Integer.signum(v1.compareTo(v2)), "expected " + v1 + " > " + v2);
            assertEquals(-1, Integer.signum(v2.compareTo(v1)), "expected " + v2 + " < " + v1);
            assertNotEquals(v1, v2, "expected " + v1 + " != " + v2);
            assertNotEquals(v2, v1, "expected " + v2 + " != " + v1);
        } else if (expected < 0) {
            assertEquals(-1, Integer.signum(v1.compareTo(v2)), "expected " + v1 + " < " + v2);
            assertEquals(1, Integer.signum(v2.compareTo(v1)), "expected " + v2 + " > " + v1);
            assertNotEquals(v1, v2, "expected " + v1 + " != " + v2);
            assertNotEquals(v2, v1, "expected " + v2 + " != " + v1);
        } else {
            assertEquals(0, v1.compareTo(v2), "expected " + v1 + " == " + v2);
            assertEquals(0, v2.compareTo(v1), "expected " + v2 + " == " + v1);
            assertEquals(v1, v2, "expected " + v1 + " == " + v2);
            assertEquals(v2, v1, "expected " + v2 + " == " + v1);
            assertEquals(v1.hashCode(), v2.hashCode(), "expected #(" + v1 + ") == #(" + v1 + ")");
        }
    }

    protected void assertSequence(String... versions) {
        for (int i = 0; i < versions.length - 1; i++) {
            for (int j = i + 1; j < versions.length; j++) {
                assertOrder(X_LT_Y, versions[i], versions[j]);
            }
        }
    }
}