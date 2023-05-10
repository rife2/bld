/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.wrapper;

import java.net.URL;
import java.net.URLClassLoader;

final class WrapperClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    WrapperClassLoader(String name, ClassLoader parent) {
        super(name, new URL[0], parent);
    }

    WrapperClassLoader(ClassLoader parent) {
        this("classpath", parent);
    }

    WrapperClassLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    void add(URL url) {
        addURL(url);
    }
}