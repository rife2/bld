/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Options for jlink tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.1.0
 */
public class JlinkOptions extends LinkedHashMap<String, String> {
    /**
     * All Modules Path.
     */
    @SuppressWarnings("unused")
    public final static String ALL_MODULE_PATH = "ALL-MODULE-PATH";

    /**
     * Root modules to resolve in addition to the initial modules.
     * <p>
     * Module can also be {@link #ALL_MODULE_PATH}
     *
     * @param modules one or more modules
     * @return this map of options
     */
    public JlinkOptions addModules(List<String> modules) {
        put("--add-modules", String.join(",", modules));
        return this;
    }

    /**
     * Root modules to resolve in addition to the initial modules.
     * <p>
     * Module can also be {@link #ALL_MODULE_PATH}
     *
     * @param modules one or more modules
     * @return this map of options
     */
    public JlinkOptions addModules(String... modules) {
        return addModules(List.of(modules));
    }

    /**
     * Link in service provider modules and their dependencies.
     *
     * @param bindServices {@code true} to bind services, {@code false} otherwise
     * @return this map of options
     */
    public JlinkOptions bindServices(boolean bindServices) {
        if (bindServices) {
            put("--bind-services");
        } else {
            remove("--bind-services");
        }
        return this;
    }

    /**
     * Compression to use in compressing resources.
     * <p>
     * <b>Requires Java 21 or higher</b>. Use {@link #compress(CompressionLevel)} for lower versions.
     * <p>
     * Where {@link ZipCompression#ZIP_0 ZIP_0} provides no compression and {@link ZipCompression#ZIP_9 ZIP_9} provides
     * the best compression.
     * <p>Default is {@link ZipCompression#ZIP_6 ZIP_6}
     *
     * @param compression the {@link ZipCompression compression} level
     * @return this map of options
     * @see #compress(ZipCompression)
     */
    public JlinkOptions compress(ZipCompression compression) {
        put("--compress", compression.level);
        return this;
    }

    /**
     * Enable compression of resources.
     * <p>
     * Use {@link #compress(ZipCompression)} on Java 21 or higher.
     *
     * @param compression the {@link CompressionLevel compression} level
     * @return this map of options
     * @see #compress(CompressionLevel)
     */
    public JlinkOptions compress(CompressionLevel compression) {
        put("--compress", compression.level);
        return this;
    }


    /**
     * Byte order of generated jimage.
     * <p>
     * Default: native
     *
     * @param endian the byte order
     * @return this map of options
     */
    public JlinkOptions endian(Endian endian) {
        put("--endian", endian.byteOrder);
        return this;
    }

    /**
     * Suppress a fatal error when signed modular JARs are linked in the image.
     *
     * @param ignoreSigningInformation {@code true} to ignore signing information, {@code false} otherwise
     * @return this map of options
     */
    public JlinkOptions ignoreSigningInformation(boolean ignoreSigningInformation) {
        if (ignoreSigningInformation) {
            put("--ignore-signing-information");
        } else {
            remove("--ignore-signing-information");
        }
        return this;
    }

    /**
     * Add a launcher command of the given name for the module.
     *
     * @param name   the name
     * @param module the module
     * @return this map of options
     */
    @SuppressWarnings("UnusedReturnValue")
    public JlinkOptions launcher(String name, String module) {
        put("--launcher", name + "=" + module);
        return this;
    }

    /**
     * Add a launcher command of the given name for the module and the main class.
     *
     * @param name      the name
     * @param module    the module
     * @param mainClass the main class
     * @return this map of options
     */
    public JlinkOptions launcher(String name, String module, String mainClass) {
        put("--launcher", name + "=" + module + "/" + mainClass);
        return this;
    }

    /**
     * Limit the universe of observable modules.
     *
     * @param modules one or more modules
     * @return this map of options
     */
    public JlinkOptions limitModule(List<String> modules) {
        put("--limit-modules", String.join(",", modules));
        return this;
    }
    /**
     * Limit the universe of observable modules.
     *
     * @param modules one or more modules
     * @return this map of options
     */
    public JlinkOptions limitModule(String... modules) {
        return limitModule(List.of(modules));
    }

    /**
     * Module path.
     * <p>
     * If not specified, the JDKs jmods directory will be used, if it exists. If specified, but it does not contain the
     * java.base module, the JDKs jmods directory will be added, if it exists.
     *
     * @param path the module path
     * @return this map of options
     */
    public JlinkOptions modulePath(String path) {
        put("--module-path", path);
        return this;
    }

    /**
     * Module path.
     * <p>
     * If not specified, the JDKs jmods directory will be used, if it exists. If specified, but it does not contain the
     * java.base module, the JDKs jmods directory will be added, if it exists.
     *
     * @param path the module path
     * @return this map of options
     */
    public JlinkOptions modulePath(File path) {
        return modulePath(path.getAbsolutePath());
    }

    /**
     * Module path.
     * <p>
     * If not specified, the JDKs jmods directory will be used, if it exists. If specified, but it does not contain the
     * java.base module, the JDKs jmods directory will be added, if it exists.
     *
     * @param path the module path
     * @return this map of options
     */
    public JlinkOptions modulePath(Path path) {
        return modulePath(path.toFile());
    }

    /**
     * Exclude include header files.
     *
     * @param noHeaderFiles {@code true} to exclude header files, {@code false} otherwise
     * @return this map of options
     */
    public JlinkOptions noHeaderFiles(boolean noHeaderFiles) {
        if (noHeaderFiles) {
            put("--no-header-files");
        } else {
            remove("--no-header-files");
        }
        return this;
    }

    /**
     * Exclude man pages.
     *
     * @param noManPages {@code true} to exclude man pages, {@code false} otherwise
     * @return this map of options
     */
    public JlinkOptions noManPages(boolean noManPages) {
        if (noManPages) {
            put("--no-man-pages");
        } else {
            remove("--no-man-pages");
        }
        return this;
    }

    /**
     * Location of output path.
     *
     * @param path the output path
     * @return this map of options
     */
    public JlinkOptions output(String path) {
        put("--output", path);
        return this;
    }

    /**
     * Location of output path.
     *
     * @param path the output path
     * @return this map of options
     */
    public JlinkOptions output(File path) {
        return output(path.getAbsolutePath());
    }

    /**
     * Location of output path.
     *
     * @param path the output path
     * @return this map of options
     */
    public JlinkOptions output(Path path) {
        return output(path.toFile());
    }

    /**
     * Associates {@code null} with the specified key in this map. If the map previously contained a mapping for the
     * key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated
     */
    public void put(String key) {
        put(key, null);
    }

    /**
     * Suggest providers that implement the given service types from the module path.
     *
     * @param filename the filename
     * @return this map of options
     */
    public JlinkOptions saveOpts(String filename) {
        put("--save-opts", filename);
        return this;
    }

    /**
     * Strip debug information.
     *
     * @param stripDebug {@code true} to strip debug info, {@code false} otherwise
     * @return this map of options
     */
    public JlinkOptions stripDebug(boolean stripDebug) {
        if (stripDebug) {
            put("--strip-debug");
        } else {
            remove("--strip-debug");
        }
        return this;
    }

    /**
     * Strip native commands.
     *
     * @param stripNativeCommands {@code true} to strip, {@code false} otherwise
     * @return this map of options
     */
    public JlinkOptions stripNativeCommands(boolean stripNativeCommands) {
        if (stripNativeCommands) {
            put("--strip-native-commands");
        } else {
            remove("--strip-native-commands");
        }
        return this;
    }

    /**
     * Suggest providers that implement the given service types from the module path.
     *
     * @param names one or more provider names
     * @return this map of options
     */
    public JlinkOptions suggestProviders(List<String> names) {
        put("--suggest-providers", String.join(",", names));
        return this;
    }

    /**
     * Suggest providers that implement the given service types from the module path.
     *
     * @param names one or more provider names
     * @return this map of options
     */
    public JlinkOptions suggestProviders(String... names) {
        return suggestProviders(List.of(names));
    }

    public List<String> toList() {
        var list = new ArrayList<String>();
        forEach((k, v) -> {
            list.add(k);
            if (v != null && !v.isEmpty()) {
                list.add(v);
            }
        });
        return list;
    }

    /**
     * Enable verbose tracing.
     *
     * @param verbose {@code true} to enable verbose tracing, {@code false} otherwise.
     * @return this map of options
     */
    public JlinkOptions verbose(boolean verbose) {
        if (verbose) {
            put("--verbose");
        } else {
            remove("--verbose");
        }
        return this;
    }

    /**
     * The byte orders.
     */
    public enum Endian {
        BIG("big"), LITTLE("little");

        public final String byteOrder;

        Endian(String byteOrder) {
            this.byteOrder = byteOrder;
        }
    }

    /**
     * Resources compression levels.
     */
    public enum CompressionLevel {
        /**
         * Level 0: No compression
         */
        NO_COMPRESSION("0"),
        /**
         * Level 1: Constant string sharing
         */
        CONSTANT_STRING_SHARING("1"),
        /**
         * Level 2: ZIP
         */
        ZIP("2");

        public final String level;

        CompressionLevel(String level) {
            this.level = level;
        }
    }
}
