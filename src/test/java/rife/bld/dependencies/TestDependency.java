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
        assertFalse(dependency1.isModularJar());
        assertFalse(dependency1.isClasspathJar());

        var dependency2 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0));
        assertNotNull(dependency2);
        assertEquals("com.uwyn.rife2", dependency2.groupId());
        assertEquals("rife2", dependency2.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency2.version());
        assertEquals("", dependency2.classifier());
        assertEquals("jar", dependency2.type());
        assertFalse(dependency2.isModularJar());
        assertFalse(dependency2.isClasspathJar());

        var dependency3 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent");
        assertNotNull(dependency3);
        assertEquals("com.uwyn.rife2", dependency3.groupId());
        assertEquals("rife2", dependency3.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency3.version());
        assertEquals("agent", dependency3.classifier());
        assertEquals("jar", dependency3.type());
        assertFalse(dependency3.isModularJar());
        assertFalse(dependency3.isClasspathJar());

        var dependency4 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip");
        assertNotNull(dependency4);
        assertEquals("com.uwyn.rife2", dependency4.groupId());
        assertEquals("rife2", dependency4.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency4.version());
        assertEquals("bld", dependency4.classifier());
        assertEquals("zip", dependency4.type());
        assertFalse(dependency4.isModularJar());
        assertFalse(dependency4.isClasspathJar());

        var dependency5 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), null, "modular-jar");
        assertNotNull(dependency5);
        assertEquals("com.uwyn.rife2", dependency5.groupId());
        assertEquals("rife2", dependency5.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency5.version());
        assertEquals("", dependency5.classifier());
        assertEquals("modular-jar", dependency5.type());
        assertTrue(dependency5.isModularJar());
        assertFalse(dependency5.isClasspathJar());

        var dependency6 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), null, "classpath-jar");
        assertNotNull(dependency6);
        assertEquals("com.uwyn.rife2", dependency6.groupId());
        assertEquals("rife2", dependency6.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), dependency6.version());
        assertEquals("", dependency6.classifier());
        assertEquals("classpath-jar", dependency6.type());
        assertFalse(dependency6.isModularJar());
        assertTrue(dependency6.isClasspathJar());
    }

    @Test
    void testParent() {
        var parent1 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0));
        var parent2 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), null, "modular-jar");
        var parent3 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), null, "classpath-jar");

        var child1a = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, null, null, parent1);
        assertFalse(child1a.isModularJar());
        assertFalse(child1a.isClasspathJar());
        var child1b = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, null, null, parent2);
        assertTrue(child1b.isModularJar());
        assertFalse(child1b.isClasspathJar());
        var child1c = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, null, null, parent3);
        assertFalse(child1c.isModularJar());
        assertFalse(child1c.isClasspathJar());

        var child2a = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, "modular-jar", null, parent1);
        assertTrue(child2a.isModularJar());
        assertFalse(child2a.isClasspathJar());
        var child2b = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, "modular-jar", null, parent2);
        assertTrue(child2b.isModularJar());
        assertFalse(child2b.isClasspathJar());
        var child2c = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, "modular-jar", null, parent3);
        assertTrue(child2c.isModularJar());
        assertFalse(child2c.isClasspathJar());

        var child3a = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, "classpath-jar", null, parent1);
        assertFalse(child3a.isModularJar());
        assertTrue(child3a.isClasspathJar());
        var child3b = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, "classpath-jar", null, parent2);
        assertFalse(child3b.isModularJar());
        assertTrue(child3b.isClasspathJar());
        var child3c = new Dependency("com.uwyn.rife2", "bld", new VersionNumber(1, 5, 0), null, "classpath-jar", null, parent3);
        assertFalse(child3c.isModularJar());
        assertTrue(child3c.isClasspathJar());
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
    void testToArtifactString() {
        assertEquals("com.uwyn.rife2:rife2", new Dependency("com.uwyn.rife2", "rife2").toArtifactString());
        assertEquals("com.uwyn.rife2:rife2", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0)).toArtifactString());
        assertEquals("com.uwyn.rife2:rife2", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent").toArtifactString());
        assertEquals("com.uwyn.rife2:rife2", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip").toArtifactString());
    }

    @Test
    void testToFileName() {
        assertEquals("rife2-0.0.0.jar", new Dependency("com.uwyn.rife2", "rife2").toFileName());
        assertEquals("rife2-1.4.0.jar", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0)).toFileName());
        assertEquals("rife2-1.4.0-agent.jar", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent").toFileName());
        assertEquals("rife2-1.4.0-bld.zip", new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "bld", "zip").toFileName());
    }

    @Test
    void testParse() {
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

    @Test
    void testEquals() {
        assertEquals(new Dependency("com.uwyn.rife2", "rife2"), new Dependency("com.uwyn.rife2", "rife2"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2"), new Dependency("com.uwyn.rife2", "rife1"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2"), new Dependency("com.uwyn.rife1", "rife2"));

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Dependency("com.uwyn.rife2", "rife2", null));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)));

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", null));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)));

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "jar"));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", null, null, "jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "jar"));

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "modular-jar"));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "modular-jar"));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", null, null, "modular-jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "modular-jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "modular-jar"));

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "classpath-jar"));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "classpath-jar"));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", null, null, "classpath-jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "classpath-jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "classpath-jar"));

        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "zip"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "zip"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", null, null, "zip"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "zip"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "zip"));

        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), "agent", "jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), "agent", "jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife2", null, "agent", "jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), "agent", "jar"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), "agent", "jar"));
    }

    @Test
    void testHashcode() {
        assertEquals(new Dependency("com.uwyn.rife2", "rife2").hashCode(), new Dependency("com.uwyn.rife2", "rife2").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2").hashCode(), new Dependency("com.uwyn.rife2", "rife1").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2").hashCode(), new Dependency("com.uwyn.rife1", "rife2").hashCode());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Dependency("com.uwyn.rife2", "rife2", null).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)).hashCode());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", null).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)).hashCode());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "jar").hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", null, null, "jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "modular-jar").hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "modular-jar").hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", null, null, "modular-jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "modular-jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "modular-jar").hashCode());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "classpath-jar").hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "classpath-jar").hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", null, null, "classpath-jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "classpath-jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "classpath-jar").hashCode());

        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "zip").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), null, "zip").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", null, null, "zip").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), null, "zip").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), null, "zip").hashCode());

        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), "agent", "jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), "agent", "jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife2", null, "agent", "jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), "agent", "jar").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Dependency("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), "agent", "jar").hashCode());
    }
}
