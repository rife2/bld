/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

/**
 * Contains the information required to describe a local module for the build system.
 * <p>
 * If the local module points to a directory, it will be scanned for jar files.
 *
 * @param path the file system path of the local module
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.1
 */
public record LocalModule(String path) {
}
