/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import java.util.regex.Pattern;

/**
 * Contains the information required to describe a Java module dependency in the build system.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.1
 */
public class Module extends Dependency {
    public Module(String groupId, String artifactId) {
        this(groupId, artifactId, null, null, null, null);
    }

    public Module(String groupId, String artifactId, Version version) {
        this(groupId, artifactId, version, null, null, null);
    }

    public Module(String groupId, String artifactId, Version version, String classifier) {
        this(groupId, artifactId, version, classifier, null, null);
    }

    public Module(String groupId, String artifactId, Version version, String classifier, ExclusionSet exclusions) {
        this(groupId, artifactId, version, classifier, exclusions, null);
    }

    public Module(String groupId, String artifactId, Version version, String classifier, ExclusionSet exclusions, Dependency parent) {
        super(groupId, artifactId, version, classifier, TYPE_MODULAR_JAR, exclusions, parent);
    }

    private static final Pattern MODULE_PATTERN = Pattern.compile("^(?<groupId>[^:@]+):(?<artifactId>[^:@]+)(?::(?<version>[^:@]+)(?::(?<classifier>[^:@]+))?)?(?:@modular-jar)?$");

    /**
     * Parses a module from a string representation.
     * The format is {@code groupId:artifactId:version:classifier}.
     * The {@code version} and {@code classifier} are optional.
     * <p>
     * If the string can't be successfully parsed, {@code null} will be returned.
     *
     * @param module the module string to parse
     * @return a parsed instance of {@code Module}; or
     * {@code null} when the string couldn't be parsed
     * @since 2.1
     */
    public static Module parse(String module) {
        if (module == null || module.isEmpty()) {
            return null;
        }

        var matcher = MODULE_PATTERN.matcher(module);
        if (!matcher.matches()) {
            return null;
        }

        var groupId = matcher.group("groupId");
        var artifactId = matcher.group("artifactId");
        var version = Version.parse(matcher.group("version"));
        var classifier = matcher.group("classifier");

        return new Module(groupId, artifactId, version, classifier);
    }
}
