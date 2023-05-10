/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestDependency {
    @Test
    void testInstantiation() {
        var dependency1 = new Dependency("com.uwyn.rife2", "rife2");
        assertNotNull(dependency1);
        assertEquals("com.uwyn.rife2", dependency1.groupId());
        assertEquals("rife2", dependency1.artifactId());
        assertEquals(VersionNumber.UNKNOWN, dependency1.version());
        assertEquals("", dependency1.classifier());
        assertEquals("jar", dependency1.type());

        var dependency2 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0));
        assertNotNull(dependency2);
        assertEquals("com.uwyn.rife2", dependency2.groupId());
        assertEquals("rife2", dependency2.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency2.version());
        assertEquals("", dependency2.classifier());
        assertEquals("jar", dependency2.type());

        var dependency3 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent");
        assertNotNull(dependency3);
        assertEquals("com.uwyn.rife2", dependency3.groupId());
        assertEquals("rife2", dependency3.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency3.version());
        assertEquals("agent", dependency3.classifier());
        assertEquals("jar", dependency3.type());

        var dependency4 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip");
        assertNotNull(dependency4);
        assertEquals("com.uwyn.rife2", dependency4.groupId());
        assertEquals("rife2", dependency4.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency4.version());
        assertEquals("bld", dependency4.classifier());
        assertEquals("zip", dependency4.type());
    }

    @Test
    void testBaseDependency() {
        var dependency1 = new Dependency("com.uwyn.rife2", "rife2");
        assertEquals(dependency1, dependency1.baseDependency());

        var dependency2 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0));
        assertEquals(dependency2, dependency1.baseDependency());

        var dependency3 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent");
        assertNotEquals(dependency3, dependency1.baseDependency());

        var dependency4 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip");
        assertNotEquals(dependency4, dependency1.baseDependency());
    }

    @Test
    void testWithClassifier() {
        var dependency1 = new Dependency("com.uwyn.rife2", "rife2");
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", VersionNumber.UNKNOWN, "sources"), dependency1.withClassifier("sources"));

        var dependency2 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "sources"), dependency2.withClassifier("sources"));

        var dependency3 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent");
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "sources"), dependency3.withClassifier("sources"));

        var dependency4 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip");
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "sources", "zip"), dependency4.withClassifier("sources"));
    }

    @Test
    void testToString() {
        assertEquals("com.uwyn.rife2:rife2", new Dependency("com.uwyn.rife2", "rife2").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0)).toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0:agent", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0:bld@zip", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip").toString());
    }

    @Test
    void testToFileName() {
        assertEquals("rife2-0.0.0.jar", new Dependency("com.uwyn.rife2", "rife2").toFileName());
        assertEquals("rife2-1.4.0.jar", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0)).toFileName());
        assertEquals("rife2-1.4.0-agent.jar", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent").toFileName());
        assertEquals("rife2-1.4.0-bld.zip", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip").toFileName());
    }

    @Test
    void testToParse() {
        assertEquals("com.uwyn.rife2:rife2", Dependency.parse("com.uwyn.rife2:rife2").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0", Dependency.parse("com.uwyn.rife2:rife2:1.4.0").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0:agent", Dependency.parse("com.uwyn.rife2:rife2:1.4.0:agent").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0:agent@zip", Dependency.parse("com.uwyn.rife2:rife2:1.4.0:agent@zip").toString());
        assertEquals("com.uwyn.rife2:rife2@zip", Dependency.parse("com.uwyn.rife2:rife2@zip").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0@zip", Dependency.parse("com.uwyn.rife2:rife2:1.4.0@zip").toString());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2").toString(), Dependency.parse("com.uwyn.rife2:rife2").toString());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0)).toString(), Dependency.parse("com.uwyn.rife2:rife2:1.4.0").toString());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent").toString(), Dependency.parse("com.uwyn.rife2:rife2:1.4.0:agent").toString());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent", "zip").toString(), Dependency.parse("com.uwyn.rife2:rife2:1.4.0:agent@zip").toString());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", null, null, "zip").toString(), Dependency.parse("com.uwyn.rife2:rife2@zip").toString());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), null, "zip").toString(), Dependency.parse("com.uwyn.rife2:rife2:1.4.0@zip").toString());
    }
}
