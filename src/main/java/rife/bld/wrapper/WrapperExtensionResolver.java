/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.wrapper;

import rife.bld.BuildExecutor;
import rife.bld.dependencies.*;
import rife.tools.FileUtils;
import rife.tools.StringUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final ArtifactRetriever retriever_ = ArtifactRetriever.cachingInstance();
    private final File hashFile_;
    private final String fingerPrintHash_;
    private final File destinationDirectory_;
    private final List<Repository> repositories_ = new ArrayList<>();
    private final DependencySet dependencies_ = new DependencySet();
    private final List<File> localArtifacts_ = new ArrayList<>();
    private final boolean downloadSources_;
    private final boolean downloadJavadoc_;

    private boolean headerPrinted_ = false;

    public WrapperExtensionResolver(File currentDir, File hashFile, File destinationDirectory,
                                    Collection<String> repositories, Collection<String> extensions,
                                    boolean downloadSources, boolean downloadJavadoc) {
        var properties = BuildExecutor.setupProperties(currentDir);
        Repository.resolveMavenLocal(properties);

        hashFile_ = hashFile;
        destinationDirectory_ = destinationDirectory;
        for (var repository : repositories) {
            repositories_.add(Repository.resolveRepository(properties, repository));
        }
        dependencies_.addAll(extensions.stream().map(Dependency::parse).toList());
        downloadSources_ = downloadSources;
        downloadJavadoc_ = downloadJavadoc;
        fingerPrintHash_ = createHash(repositories_.stream().map(Objects::toString).toList(), extensions, downloadSources, downloadJavadoc);
    }

    private String createHash(Collection<String> repositories, Collection<String> extensions, boolean downloadSources, boolean downloadJavadoc) {
        try {
            var fingerprint = String.join("\n", repositories) + "\n" + String.join("\n", extensions) + "\n" + downloadSources + "\n" + downloadJavadoc;
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(fingerprint.getBytes(StandardCharsets.UTF_8));
            return StringUtils.encodeHexLower(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    public void updateExtensions() {
        // verify and update the fingerprint hash file,
        // don't update the extensions if the hash is identical
        if (validateHash()) {
            return;
        }

        // collect and download the extensions dependencies
        var filenames = transferExtensionDependencies();

        // purge the files that are not part of the latest extensions anymore
        purgeExtensionDependencies(filenames);

        writeHash();

        if (headerPrinted_) {
            System.out.println();
        }
    }

    private boolean validateHash() {
        try {
            if (hashFile_.exists()) {
                var contents = FileUtils.readString(hashFile_);
                var lines = StringUtils.split(contents, "\n");
                if (!lines.isEmpty()) {
                    // first line is the fingerprint hash
                    if (lines.remove(0).equals(fingerPrintHash_)) {
                        // other lines are last modified timestamps of local files
                        // that were dependency artifacts
                        while (!lines.isEmpty()) {
                            var line = lines.get(0);
                            var parts = line.split(":", 2);
                            // verify that the local file has the same modified timestamp still
                            if (parts.length == 2) {
                                var file = new File(parts[1]);
                                if (!file.exists() || !file.canRead() || file.lastModified() != Long.parseLong(parts[0])) {
                                    break;
                                }
                            } else {
                                break;
                            }
                            lines.remove(0);
                        }

                        // there were no invalid lines, so the hash file contents are valid
                        if (lines.isEmpty()) {
                            return true;
                        }
                    }
                }
                hashFile_.delete();
            }
            return false;
        } catch (FileUtilsErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeHash() {
        try {
            var contents = new StringBuilder();
            contents.append(fingerPrintHash_);
            for (var file : localArtifacts_) {
                if (file.exists() && file.canRead()) {
                    contents.append("\n").append(file.lastModified()).append(":").append(file.getAbsolutePath());
                }
            }

            FileUtils.writeString(contents.toString(), hashFile_);
        } catch (FileUtilsErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> transferExtensionDependencies() {
        var filenames = new HashSet<String>();
        var dependencies = new DependencySet();
        for (var d : dependencies_) {
            if (d != null) {
                dependencies.addAll(new DependencyResolver(retriever_, repositories_, d).getAllDependencies(Scope.compile, Scope.runtime));
            }
        }
        if (!dependencies.isEmpty()) {
            ensurePrintedHeader();

            dependencies.removeIf(dependency -> dependency.baseDependency().equals(new Dependency("com.uwyn.rife2", "rife2")));

            var additional_classifiers = new String[0];
            if (downloadSources_ || downloadJavadoc_) {
                var classifiers = new ArrayList<String>();
                if (downloadSources_) classifiers.add(CLASSIFIER_SOURCES);
                if (downloadJavadoc_) classifiers.add(CLASSIFIER_JAVADOC);

                additional_classifiers = classifiers.toArray(new String[0]);
            }

            var artifacts = dependencies.transferIntoDirectory(retriever_, repositories_, destinationDirectory_, additional_classifiers);
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
                file.getName().equals(Wrapper.BLD_BUILD_HASH)) {
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
