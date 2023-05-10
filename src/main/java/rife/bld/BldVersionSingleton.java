/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld;

/**
 * Helper class to avoid Double Check Locking
 * and still have a thread-safe singleton pattern
 */
class BldVersionSingleton {
    static final BldVersion INSTANCE = new BldVersion();
}

