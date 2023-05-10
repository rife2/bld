/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

import java.io.File;

/**
 * Combines the information of a filesystem file with the name it's intended
 * to have.
 *
 * @param name the intended name of the file
 * @param file the location on the filesystem
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public record NamedFile(String name, File file) {
}
