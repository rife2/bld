/**
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

/**
 * The zip compression levels for jlink and jmod.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.0.2
 */
public enum ZipCompression {
    ZIP_0("zip-0"),
    ZIP_1("zip-1"),
    ZIP_2("zip-2"),
    ZIP_3("zip-3"),
    ZIP_4("zip-4"),
    ZIP_5("zip-5"),
    ZIP_6("zip-6"),
    ZIP_7("zip-7"),
    ZIP_8("zip-8"),
    ZIP_9("zip-9");

    public final String level;

    ZipCompression(String level) {
        this.level = level;
    }
}
