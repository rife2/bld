/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import rife.bld.dependencies.exceptions.DependencyTransferException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Collects the artifact transfers of one or more dependency sets so that
 * they can be transferred together in a single parallel batch, instead of
 * separate consecutive batches per dependency set.
 * <p>
 * The batch itself is a passive collector, the resolution context is only
 * provided when the transfers are {@linkplain #transfer performed}. The
 * parallelism is determined by {@link VersionResolution#transferParallelism()},
 * setting it to {@code 1} makes the transfers sequential. Identical
 * transfers into the same directory are only performed once.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.3.1
 */
public class DependencyTransferBatch {
    private record TransferRequest(Dependency dependency, File directory, String[] classifiers) {
    }

    private final List<TransferRequest> requests_ = new ArrayList<>();
    private final Set<String> transferTargets_ = new HashSet<>();

    /**
     * Adds the artifact transfers for a dependency set to this batch.
     * <p>
     * The destination directory must exist and be writable.
     *
     * @param dependencies     the dependencies whose artifacts to transfer
     * @param directory        the directory to transfer the artifacts into
     * @param modulesDirectory the directory to download the modules into
     * @param classifiers      the additional classifiers to transfer
     * @return this batch instance
     * @throws DependencyTransferException when the transfer couldn't be prepared
     * @since 2.3.1
     */
    public DependencyTransferBatch add(DependencySet dependencies, File directory, File modulesDirectory, String... classifiers) {
        for (var dependency : dependencies) {
            var transfer_directory = directory;
            if (dependency.isModularJar()) {
                if (modulesDirectory == null) {
                    throw new DependencyTransferException(dependency, "modules directory is not provided");
                }
                transfer_directory = modulesDirectory;
            }
            else if (directory == null) {
                throw new DependencyTransferException(dependency, "artifacts directory is not provided");
            }

            if (!transfer_directory.exists()) {
                if (!transfer_directory.mkdirs()) {
                    throw new DependencyTransferException(dependency, transfer_directory, "couldn't create directory");
                }
            }

            // skip transfers that are already batched for the same directory
            if (!transferTargets_.add(dependency + " -> " + transfer_directory.getAbsolutePath())) {
                continue;
            }

            requests_.add(new TransferRequest(dependency, transfer_directory, classifiers));
        }
        return this;
    }

    /**
     * Performs all the collected artifact transfers in a single parallel
     * batch, in the order they were added.
     * <p>
     * This empties the batch, transfers can be collected and transferred
     * again with the same instance.
     *
     * @param resolution   the version resolution state that can be cached
     * @param retriever    the retriever to use to get artifacts
     * @param repositories the repositories to use for the transfer
     * @return the list of artifacts that were transferred successfully
     * @throws DependencyTransferException when an error occurred during the transfer
     * @since 2.3.1
     */
    public List<RepositoryArtifact> transfer(VersionResolution resolution, ArtifactRetriever retriever, List<Repository> repositories) {
        final var repos = (repositories == null ? List.<Repository>of() : repositories);
        try {
            var transfers = new ArrayList<Supplier<List<RepositoryArtifact>>>(requests_.size());
            for (var request : requests_) {
                transfers.add(() -> {
                    var artifacts = new ArrayList<RepositoryArtifact>();
                    var artifact = new DependencyResolver(resolution, retriever, repos, request.dependency()).transferIntoDirectory(request.directory());
                    if (artifact != null) {
                        artifacts.add(artifact);
                    }

                    if (request.classifiers() != null) {
                        for (var classifier : request.classifiers()) {
                            if (classifier != null && !request.dependency().excludedClassifiers().contains(classifier)) {
                                var classifier_artifact = new DependencyResolver(resolution, retriever, repos, request.dependency().withClassifier(classifier)).transferIntoDirectory(request.directory());
                                if (classifier_artifact != null) {
                                    artifacts.add(classifier_artifact);
                                }
                            }
                        }
                    }
                    return artifacts;
                });
            }

            var result = new ArrayList<RepositoryArtifact>();
            for (var artifacts : ParallelExecution.execute(transfers, resolution.transferParallelism())) {
                result.addAll(artifacts);
            }
            return result;
        } finally {
            requests_.clear();
            transferTargets_.clear();
        }
    }
}
