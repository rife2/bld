/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.wrapper;

import rife.bld.BldCache;
import rife.bld.BuildExecutor;
import rife.bld.dependencies.*;
import rife.ioc.HierarchicalProperties;

import java.io.File;
import java.util.*;

import static rife.bld.dependencies.Dependency.CLASSIFIER_JAVADOC;
import static rife.bld.dependencies.Dependency.CLASSIFIER_SOURCES;

/**
 * Resolves, downloads and purges the bld extension dependencies.
 * <p>
 * This is used by the bld wrapper and should not be called directly.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.8
 */
public class WrapperExtensionResolver {
    private final VersionResolution resolution_;
    private final ArtifactRetriever retriever_;
    private final File destinationDirectory_;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencySet dependencies_ = new DependencySet();
    private final List<File> localArtifacts_ = new ArrayList<>();
    private final boolean downloadSources_;
    private final boolean downloadJavadoc_;

    private boolean headerPrinted_ = false;

    public WrapperExtensionResolver(File currentDir, File destinationDirectory,
                                    Properties jvmProperties, Properties wrapperProperties,
                                    Collection<String> repositories, Collection<String> extensions,
                                    boolean downloadSources, boolean downloadJavadoc) {
        var properties = BuildExecutor.setupProperties(currentDir);
        properties.getRoot().putAll(jvmProperties);
        properties = new HierarchicalProperties().parent(properties);
        properties.putAll(wrapperProperties);

        resolution_ = new VersionResolution(properties);

        retriever_ = ArtifactRetriever.cachingInstance();
        Repository.resolveMavenLocal(properties);

        destinationDirectory_ = destinationDirectory;

        for (var repository : repositories) {
            repositories_.add(Repository.resolveRepository(properties, repository));
        }

        dependencies_.addAll(extensions.stream().map(d -> resolution_.overrideDependency(Dependency.parse(d))).toList());

        downloadSources_ = downloadSources;
        downloadJavadoc_ = downloadJavadoc;
    }

    public void updateExtensions() {
        // verify and update the fingerprint hash file,
        // don't update the extensions if the hash is identical
        var cache = new BldCache(destinationDirectory_, resolution_);
        cache.cacheExtensionsHash(
            repositories_.stream().map(Objects::toString).toList(),
            dependencies_.stream().map(Objects::toString).toList());
        cache.cacheExtensionsDownloads(downloadSources_, downloadJavadoc_);
        if (cache.isExtensionsCacheValid()) {
            return;
        }

        // collect and download the extensions dependencies
        var filenames = transferExtensionDependencies();

        // purge the files that are not part of the latest extensions anymore
        purgeExtensionDependencies(filenames);

        cache.cacheExtensionsLocalArtifacts(localArtifacts_);
        cache.writeCache();

        if (headerPrinted_) {
            System.out.println();
        }
    }

    private Set<String> transferExtensionDependencies() {
        var filenames = new HashSet<String>();
        var dependencies = new DependencySet();
        for (var d : dependencies_) {
            if (d != null) {
                dependencies.addAll(new DependencyResolver(resolution_, retriever_, repositories_, d).getAllDependencies(Scope.compile, Scope.runtime));
            }
        }
        if (!dependencies.isEmpty()) {
            ensurePrintedHeader();

            dependencies.removeIf(dependency -> dependency.baseDependency().equals(new Dependency("com.uwyn.rife2", "bld")));

            var additional_classifiers = new String[0];
            if (downloadSources_ || downloadJavadoc_) {
                var classifiers = new ArrayList<String>();
                if (downloadSources_) classifiers.add(CLASSIFIER_SOURCES);
                if (downloadJavadoc_) classifiers.add(CLASSIFIER_JAVADOC);

                additional_classifiers = classifiers.toArray(new String[0]);
            }

            var artifacts = dependencies.transferIntoDirectory(resolution_, retriever_, repositories_, destinationDirectory_, destinationDirectory_, additional_classifiers);
            for (var artifact : artifacts) {
                var location = artifact.location();

                if (artifact.repository().isLocal()) {
                    localArtifacts_.add(new File(location));
                }
                filenames.add(location.substring(location.lastIndexOf("/") + 1));
            }
        }

        return filenames;
    }

    private void purgeExtensionDependencies(Set<String> filenames) {
        for (var file : destinationDirectory_.listFiles()) {
            if (file.getName().startsWith(Wrapper.WRAPPER_PREFIX) ||
                file.getName().equals(Wrapper.BLD_CACHE)) {
                continue;
            }
            if (!filenames.contains(file.getName())) {
                ensurePrintedHeader();
                System.out.println("Deleting : " + file.getName());
                file.delete();
            }
        }
    }

    private void ensurePrintedHeader() {
        if (!headerPrinted_) {
            System.out.println("Updating bld extensions...");
        }
        headerPrinted_ = true;
    }
}
