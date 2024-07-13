/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import rife.ioc.HierarchicalProperties;

public class TestVersionResolution {
    @Test
    public void testVersionResolutionConstructor() {
        var properties = new HierarchicalProperties();
        properties.put("bld.override", "com.bookstore:book-api:2.0.0");

        var versionResolution = new VersionResolution(properties);
        Assertions.assertNotNull(versionResolution);

        versionResolution = new VersionResolution(null);
        Assertions.assertNotNull(versionResolution);
    }

    @Test
    public void testOverrideVersion() {
        var properties = new HierarchicalProperties();
        properties.put("bld.override", "com.bookstore:book-api:2.0.0");

        var versionResolution = new VersionResolution(properties);

        var originalDependency = new Dependency("com.bookstore", "book-api", new VersionNumber(1,0,0));
        var overriddenVersion = versionResolution.overrideVersion(originalDependency);

        Assertions.assertEquals(new VersionNumber(2,0,0), overriddenVersion);
    }

    @Test
    public void testOverrideDependency() {
        var properties = new HierarchicalProperties();
        properties.put("bld.override", "com.bookstore:book-api:2.0.0");

        var versionResolution = new VersionResolution(properties);

        var originalDependency = new Dependency("com.bookstore", "book-api",new  VersionNumber(1,0,0));
        var overriddenDependency = versionResolution.overrideDependency(originalDependency);

        Assertions.assertEquals(new VersionNumber(2,0,0), overriddenDependency.version());
    }
}