/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import rife.bld.dependencies.Dependency;
import rife.bld.dependencies.DependencyResolver;
import rife.bld.dependencies.DependencySet;
import rife.bld.dependencies.ParallelDependencyResolver;
import rife.bld.dependencies.Repository;
import rife.bld.dependencies.Scope;
import rife.bld.dependencies.ArtifactRetriever;
import rife.bld.dependencies.VersionResolution;
import rife.bld.wrapper.Wrapper;
import rife.ioc.HierarchicalProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves the classpath jars of a single dependency of the extensions
 * that the bld wrapper declares.
 * <p>
 * Unlike the project dependencies, which are declared in the build file's
 * Java code, the extension declarations live in the wrapper properties on
 * disk, this class reads them the same way the wrapper does.
 * <p>
 * An instance holds the declarations it read when it was created, a
 * lookup creates a new instance so that a wrapper properties file that
 * changed is picked up. Downloading and purging the extension artifacts
 * themselves is done by {@code WrapperExtensionResolver} instead, which
 * the wrapper invokes before the build starts.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4.0
 */
class ExtensionClasspath {
    private final File libBldDirectory_;
    private final ArtifactRetriever retriever_;
    private final VersionResolution resolution_;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencySet extensions_ = new DependencySet();

    private Declarations declarations_ = null;
    private DependencySet resolvedExtensions_ = null;

    /**
     * The declarations that the resolution of the extensions is based on.
     * <p>
     * The categories are kept apart, their string forms overlap: an
     * override renders exactly like the dependency string of an extension
     * with that version.
     */
    private record Declarations(List<String> repositories, List<String> extensions, List<String> overrides) {
    }

    ExtensionClasspath(File workDirectory, File libBldDirectory, ArtifactRetriever retriever) {
        libBldDirectory_ = libBldDirectory;
        retriever_ = retriever;

        var wrapper = new Wrapper();
        wrapper.currentDir(workDirectory);
        try {
            wrapper.initWrapperProperties(BldVersion.getVersion());
        } catch (IOException e) {
            throw new RuntimeException("Unable to read the bld wrapper properties.", e);
        }

        var properties = new HierarchicalProperties().parent(BuildExecutor.setupProperties(workDirectory));
        properties.putAll(wrapper.wrapperProperties());

        resolution_ = new VersionResolution(properties);
        Repository.resolveMavenLocal(properties);
        for (var repository : wrapper.repositories()) {
            repositories_.add(Repository.resolveRepository(properties, repository));
        }
        extensions_.addAll(wrapper.extensions().stream().map(d -> resolution_.overrideDependency(Dependency.parse(d))).toList());

        // the declarations are compared as their string forms, the set
        // equality of dependencies ignores their versions, and the version
        // overrides are part of them since they can change the resolution
        // of a transitive dependency without changing an extension
        declarations_ = new Declarations(
            repositories_.stream().map(Objects::toString).toList(),
            extensions_.stream().map(Objects::toString).toList(),
            resolution_.versionOverrides().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .sorted()
                .toList());
    }

    boolean hasSameDeclarationsAs(ExtensionClasspath other) {
        return declarations_.equals(other.declarations_);
    }

    List<File> classpathJars(Dependency dependency, String coordinate) {
        // the fingerprint is derived through the same call as
        // WrapperExtensionResolver, a different hash would make the
        // wrapper consider its own extension cache stale on every build
        // and download all the extension artifacts over and over
        var cache = new BldCache(libBldDirectory_, resolution_);
        cache.cacheExtensionsHash(repositories_, extensions_);
        if (cache.isExtensionsHashValid()) {
            var cached = cache.getCachedExtensionClasspath(coordinate);
            if (cached != null) {
                return BaseProject.resolveCachedFiles(libBldDirectory_, null, cached);
            }
        }

        // the resolved set of all the extensions is reused across
        // lookups of different dependencies in the same build
        if (resolvedExtensions_ == null) {
            resolvedExtensions_ = new ParallelDependencyResolver(resolution_, retriever_, repositories_)
                .resolveAllDependencies(extensions_, Scope.compile, Scope.runtime);
        }

        var resolved_dependency = resolvedExtensions_.get(dependency);
        if (resolved_dependency == null) {
            throw new IllegalArgumentException("Dependency '" + coordinate + "' isn't part of the extensions of this project.");
        }

        var resolver = new DependencyResolver(resolution_, retriever_, repositories_, resolved_dependency);
        // the wrapper transfers the modules into the same directory as
        // the other extension artifacts, there is no separate one
        var jars = resolver.getAllDependencies(Scope.compile, Scope.runtime).stream()
            .map(d -> BaseProject.transferredFile(d, resolution_, retriever_, repositories_, libBldDirectory_, null))
            .toList();

        cache.cacheExtensionClasspath(coordinate, BaseProject.cacheableFiles(null, jars));
        cache.writeCache();
        return jars;
    }
}
