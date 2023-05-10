/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

/**
 * Contains the information required to describe a local dependency for the build system.
 * <p>
 * If the local dependency points to a directory, it will be scanned for jar files.
 *
 * @param path the file system path of the local dependency
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.2
 */
public record LocalDependency(String path) {
}
