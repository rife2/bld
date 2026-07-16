/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.regex.Pattern;

/**
 * Contains the information required to describe a bill of materials (BOM)
 * in the build system.
 * <p>
 * A BOM is a POM whose dependency management section supplies versions
 * during dependency resolution: dependencies that are declared without a
 * version take the BOM's version, and transitive dependencies that match a
 * BOM entry are pinned to the BOM's version. Versions that are explicitly
 * declared in the build file, and versions provided through the
 * {@code bld.override} property, always take precedence over a BOM.
 * <p>
 * BOMs don't transfer any artifacts, they only participate in resolution.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4.0
 */
public class Bom extends Dependency {
    public Bom(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public Bom(String groupId, String artifactId, Version version) {
        super(groupId, artifactId, version, null, TYPE_BOM);
    }

    private static final Pattern BOM_PATTERN = Pattern.compile("^(?<groupId>[^:@]+):(?<artifactId>[^:@]+)(?::(?<version>[^:@]+))?(?:@(?:bom|pom))?$");

    /**
     * Parses a BOM from a string representation.
     * The format is {@code groupId:artifactId:version}.
     * The {@code version} is optional, and an optional {@code @bom} or
     * {@code @pom} type suffix is accepted. BOMs can't have classifiers,
     * strings that contain one are rejected.
     * <p>
     * If the string can't be successfully parsed, {@code null} will be returned.
     *
     * @param bom the BOM string to parse
     * @return a parsed instance of {@code Bom}; or
     * {@code null} when the string couldn't be parsed
     * @since 2.4.0
     */
    public static Bom parse(String bom) {
        if (bom == null || bom.isEmpty()) {
            return null;
        }

        var matcher = BOM_PATTERN.matcher(bom);
        if (!matcher.matches()) {
            return null;
        }

        var groupId = matcher.group("groupId");
        var artifactId = matcher.group("artifactId");
        var version = Version.parse(matcher.group("version"));

        return new Bom(groupId, artifactId, version);
    }
}
