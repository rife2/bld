/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import rife.bld.dependencies.*;
import rife.template.Template;
import rife.template.TemplateFactory;
import rife.tools.StringUtils;

import java.util.Objects;

/**
 * Provides the functionalities to build a Maven POM xml file.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.7
 */
public class PomBuilder {
    private PublishInfo info_ = null;
    private DependencyScopes dependencies_ = new DependencyScopes();

    /**
     * Provides the publishing info to build the POM for.
     *
     * @param info the publishing info to use
     * @return this {@code PomBuilder} instance
     * @since 1.5.7
     */
    public PomBuilder info(PublishInfo info) {
        info_ = info;
        return this;
    }

    /**
     * Retrieves the publishing info to build the POM for.
     *
     * @return the publishing info
     * @since 1.5.7
     */
    public PublishInfo info() {
        return info_;
    }

    /**
     * Provides the dependencies to build the POM for.
     *
     * @param dependencies the dependencies to use
     * @return this {@code PomBuilder} instance
     * @since 1.5.7
     */
    public PomBuilder dependencies(DependencyScopes dependencies) {
        dependencies_ = dependencies;
        return this;
    }

    /**
     * Retrieves the dependencies to build the POM for.
     *
     * @return the dependencies to use
     * @since 1.5.7
     */
    public DependencyScopes dependencies() {
        return dependencies_;
    }

    /**
     * Builds the Maven POM xml file.
     *
     * @return the generated Maven POM xml file as a string
     * @since 1.5.7
     */
    public String build() {
        var t = TemplateFactory.XML.get("bld.pom_blueprint");

        var info = info();
        if (info != null) {
            t.setValueEncoded("groupId", Objects.requireNonNullElse(info.groupId(), ""));
            t.setValueEncoded("artifactId", Objects.requireNonNullElse(info.artifactId(), ""));
            t.setValueEncoded("version", Objects.requireNonNullElse(info.version(), ""));
            t.setValueEncoded("name", Objects.requireNonNullElse(info.name(), ""));
            t.setValueEncoded("description", Objects.requireNonNullElse(info.description(), ""));
            t.setValueEncoded("url", Objects.requireNonNullElse(info.url(), ""));

            if (!info.licenses().isEmpty()) {
                for (var license : info.licenses()) {
                    t.setValueEncoded("license-name", Objects.requireNonNullElse(license.name(), ""));
                    t.setValueEncoded("license-url", Objects.requireNonNullElse(license.url(), ""));
                    t.appendBlock("licenses", "license");
                }
                t.setBlock("licenses-tag");
            }

            if (!info.developers().isEmpty()) {
                for (var developer : info.developers()) {
                    t.setValueEncoded("developer-id", Objects.requireNonNullElse(developer.id(), ""));
                    t.setValueEncoded("developer-name", Objects.requireNonNullElse(developer.name(), ""));
                    t.setValueEncoded("developer-email", Objects.requireNonNullElse(developer.email(), ""));
                    t.setValueEncoded("developer-url", Objects.requireNonNullElse(developer.url(), ""));
                    t.appendBlock("developers", "developer");
                }
                t.setBlock("developers-tag");
            }

            if (info.scm() != null) {
                var scm = info.scm();
                t.setValueEncoded("scm-connection", Objects.requireNonNullElse(scm.connection(), ""));
                t.setValueEncoded("scm-developerConnection", Objects.requireNonNullElse(scm.developerConnection(), ""));
                t.setValueEncoded("scm-url", Objects.requireNonNullElse(scm.url(), ""));
                t.setBlock("scm-tag");
            }
        }

        if (dependencies() != null && !dependencies().isEmpty()) {
            addDependencies(t, Scope.compile);
            addDependencies(t, Scope.runtime);
            t.setBlock("dependencies-tag");
        }

        return StringUtils.stripBlankLines(t.getContent());
    }

    private void addDependencies(Template t, Scope scope) {
        var scoped_dependencies = dependencies().scope(scope);
        if (!scoped_dependencies.isEmpty()) {
            for (var dependency : scoped_dependencies) {
                t.setValueEncoded("dependency-groupId", dependency.groupId());
                t.setValueEncoded("dependency-artifactId", dependency.artifactId());

                t.blankValue("dependency-version");
                t.blankValue("dependency-version-tag");
                if (!dependency.version().equals(VersionNumber.UNKNOWN)) {
                    t.setValueEncoded("dependency-version", dependency.version());
                    t.setBlock("dependency-version-tag");
                }

                t.blankValue("dependency-type");
                t.blankValue("dependency-type-tag");
                if (!dependency.type().equals("jar")) {
                    t.setValueEncoded("dependency-type", dependency.type());
                    t.setBlock("dependency-type-tag");
                }

                t.blankValue("dependency-classifier");
                t.blankValue("dependency-classifier-tag");
                if (!dependency.classifier().isBlank()) {
                    t.setValueEncoded("dependency-classifier", dependency.classifier());
                    t.setBlock("dependency-classifier-tag");
                }

                t.setValueEncoded("dependency-scope", scope);
                t.blankValue("dependency-exclusions");
                t.blankValue("dependency-exclusions-tag");
                if (!dependency.exclusions().isEmpty()) {
                    for (var exclusion : dependency.exclusions()) {
                        t.setValueEncoded("exclusion-groupId", exclusion.groupId());
                        t.setValueEncoded("exclusion-artifactId", exclusion.artifactId());
                        t.appendBlock("dependency-exclusions", "dependency-exclusion");
                    }
                    t.setBlock("dependency-exclusions-tag");
                }

                t.appendBlock("dependencies", "dependency");
            }
        }
    }
}
