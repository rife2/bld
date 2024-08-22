/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestModule {
    @Test
    void testModule() {
        var module1 = new Module("com.uwyn.rife2", "rife2");
        assertNotNull(module1);
        assertEquals("com.uwyn.rife2", module1.groupId());
        assertEquals("rife2", module1.artifactId());
        assertEquals(VersionNumber.UNKNOWN, module1.version());
        assertEquals("", module1.classifier());
        assertEquals("modular-jar", module1.type());
        assertTrue(module1.isModularJar());
        assertFalse(module1.isClasspathJar());

        var module2 = new Module("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0));
        assertNotNull(module2);
        assertEquals("com.uwyn.rife2", module2.groupId());
        assertEquals("rife2", module2.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), module2.version());
        assertEquals("", module2.classifier());
        assertEquals("modular-jar", module2.type());
        assertTrue(module2.isModularJar());
        assertFalse(module2.isClasspathJar());

        var module3 = new Module("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent");
        assertNotNull(module3);
        assertEquals("com.uwyn.rife2", module3.groupId());
        assertEquals("rife2", module3.artifactId());
        assertEquals(new VersionNumber(1, 4, 0), module3.version());
        assertEquals("agent", module3.classifier());
        assertEquals("modular-jar", module3.type());
        assertTrue(module3.isModularJar());
        assertFalse(module3.isClasspathJar());
    }

    @Test
    void testParse() {
        assertEquals("com.uwyn.rife2:rife2@modular-jar", Module.parse("com.uwyn.rife2:rife2").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0@modular-jar", Module.parse("com.uwyn.rife2:rife2:1.4.0").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0:agent@modular-jar", Module.parse("com.uwyn.rife2:rife2:1.4.0:agent").toString());
        assertEquals("com.uwyn.rife2:rife2@modular-jar", Module.parse("com.uwyn.rife2:rife2@modular-jar").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0@modular-jar", Module.parse("com.uwyn.rife2:rife2:1.4.0@modular-jar").toString());
        assertEquals("com.uwyn.rife2:rife2:1.4.0:agent@modular-jar", Module.parse("com.uwyn.rife2:rife2:1.4.0:agent@modular-jar").toString());

        assertEquals(new Module("com.uwyn.rife2", "rife2").toString(), Module.parse("com.uwyn.rife2:rife2").toString());
        assertEquals(new Module("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0)).toString(), Module.parse("com.uwyn.rife2:rife2:1.4.0").toString());
        assertEquals(new Module("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent").toString(), Module.parse("com.uwyn.rife2:rife2:1.4.0:agent").toString());
        assertEquals(new Module("com.uwyn.rife2", "rife2").toString(), Module.parse("com.uwyn.rife2:rife2@modular-jar").toString());
        assertEquals(new Module("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0)).toString(), Module.parse("com.uwyn.rife2:rife2:1.4.0@modular-jar").toString());
        assertEquals(new Module("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0), "agent").toString(), Module.parse("com.uwyn.rife2:rife2:1.4.0:agent@modular-jar").toString());
    }

    @Test
    void testEquals() {
        assertEquals(new Dependency("com.uwyn.rife2", "rife2"), new Module("com.uwyn.rife2", "rife2"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2"), new Module("com.uwyn.rife2", "rife1"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2"), new Module("com.uwyn.rife1", "rife2"));

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Module("com.uwyn.rife2", "rife2", null));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Module("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)), new Module("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)));

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)));
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife2", null));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)));

        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), "agent"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), "agent"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife2", null, "agent"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), "agent"));
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar"), new Module("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), "agent"));
    }

    @Test
    void testHashcode() {
        assertEquals(new Dependency("com.uwyn.rife2", "rife2").hashCode(), new Module("com.uwyn.rife2", "rife2").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2").hashCode(), new Module("com.uwyn.rife2", "rife1").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2").hashCode(), new Module("com.uwyn.rife1", "rife2").hashCode());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Module("com.uwyn.rife2", "rife2", null).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Module("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode(), new Module("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)).hashCode());

        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1)).hashCode());
        assertEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife2", null).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0)).hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0)).hashCode());

        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), "agent").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife2", new VersionNumber(1,4,1), "agent").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife2", null, "agent").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife2", "rife1", new VersionNumber(1,4,0), "agent").hashCode());
        assertNotEquals(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,4,0), null, "jar").hashCode(), new Module("com.uwyn.rife1", "rife2", new VersionNumber(1,4,0), "agent").hashCode());
    }
}
