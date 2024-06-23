/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestPublishProperties {
    @Test
    public void testMavenCompilerSourceDefaultValue() {
        var publishProperties = new PublishProperties();
        var actualValue = publishProperties.mavenCompilerSource();
        assertNull(actualValue);
    }

    @Test
    public void testMavenCompilerSourceSetterWhenValueIsNotNull() {
        var publishProperties = new PublishProperties();
        Integer testValue = 15;
        publishProperties.mavenCompilerSource(testValue);
        Integer actualValue = Integer.parseInt(publishProperties.get("maven.compiler.source"));
        assertEquals(testValue, actualValue);
    }

    @Test
    public void testMavenCompilerSourceSetterWhenValueIsNull() {
        var publishProperties = new PublishProperties();
        publishProperties.mavenCompilerSource(null);
        var actualValue = publishProperties.get("maven.compiler.source");
        assertNull(actualValue);
    }

    @Test
    public void testMavenCompilerSourceGetterWhenValueIsNotNull() {
        var publishProperties = new PublishProperties();
        Integer testValue = 8;
        publishProperties.put("maven.compiler.source", String.valueOf(testValue));
        var actualValue = publishProperties.mavenCompilerSource();
        assertEquals(testValue, actualValue);
    }

    @Test
    public void testMavenCompilerSourceGetterWhenValueIsNull() {
        var publishProperties = new PublishProperties();
        publishProperties.put("maven.compiler.source", null);
        var actualValue = publishProperties.mavenCompilerSource();
        assertNull(actualValue);
    }

    @Test
    public void testMavenCompilerTargetDefaultValue() {
        var publishProperties = new PublishProperties();
        var actualValue = publishProperties.mavenCompilerTarget();
        assertNull(actualValue);
    }

    @Test
    public void testMavenCompilerTargetSetterWhenValueIsNotNull() {
        var publishProperties = new PublishProperties();
        Integer testValue = 15;
        publishProperties.mavenCompilerTarget(testValue);
        Integer actualValue = Integer.parseInt(publishProperties.get("maven.compiler.target"));
        assertEquals(testValue, actualValue);
    }

    @Test
    public void testMavenCompilerTargetSetterWhenValueIsNull() {
        var publishProperties = new PublishProperties();
        publishProperties.mavenCompilerTarget(null);
        var actualValue = publishProperties.get("maven.compiler.target");
        assertNull(actualValue);
    }

    @Test
    public void testMavenCompilerTargetGetterWhenValueIsNotNull() {
        var publishProperties = new PublishProperties();
        Integer testValue = 8;
        publishProperties.put("maven.compiler.target", String.valueOf(testValue));
        var actualValue = publishProperties.mavenCompilerTarget();
        assertEquals(testValue, actualValue);
    }

    @Test
    public void testMavenCompilerTargetGetterWhenValueIsNull() {
        var publishProperties = new PublishProperties();
        publishProperties.put("maven.compiler.target", null);
        var actualValue = publishProperties.mavenCompilerTarget();
        assertNull(actualValue);
    }
}