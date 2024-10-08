/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.io.File;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Options for jmod tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.1.0
 */
public class JmodOptions extends LinkedHashMap<String, String> {
    /**
     * Application jar files|dir containing classes.
     *
     * @param classpath the classpath
     * @return this map of options
     */
    public JmodOptions classpath(String classpath) {
        put("--class-path", classpath);
        return this;
    }

    /**
     * Location of native commands.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions cmds(String path) {
        put("--cmds", path);
        return this;
    }

    /**
     * Location of native commands.
     *
     * @param path the location
     * @return this map of options
     */
    @SuppressWarnings("UnusedReturnValue")
    public JmodOptions cmds(File path) {
        return cmds(path.getAbsolutePath());
    }

    /**
     * Location of native commands.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions cmds(Path path) {
        return cmds(path.toFile().getAbsolutePath());
    }

    /**
     * Compression to use when creating the JMOD archive.
     * <p>
     * <b>Requires Java 20 or higher</b>.
     * <p>
     * Where {@link ZipCompression#ZIP_0 ZIP_0} provides no compression and {@link ZipCompression#ZIP_9 ZIP_9} provides the
     * best compression.
     * <p>
     * Default is {@link ZipCompression#ZIP_6 ZIP_6}
     *
     * @param compression the {@link ZipCompression compression} level
     * @return this map of options
     */
    @SuppressWarnings("UnusedReturnValue")
    public JmodOptions compress(ZipCompression compression) {
        put("--compress", compression.level);
        return this;
    }

    /**
     * Location of user-editable config files
     *
     * @param path the path to the config files
     * @return this map of options
     */
    public JmodOptions config(String path) {
        put("--config", path);
        return this;
    }

    /**
     * Location of user-editable config files
     *
     * @param path the path to the config files
     * @return this map of options
     */
    public JmodOptions config(File path) {
        return config(path.getAbsolutePath());
    }

    /**
     * Location of user-editable config files
     *
     * @param path the path to the config files
     * @return this map of options
     */
    public JmodOptions config(Path path) {
        return config(path.toFile().getAbsolutePath());
    }

    /**
     * Date and time for the timestamps of entries.
     *
     * @param date the date
     * @return this map of options
     */
    public JmodOptions date(ZonedDateTime date) {
        put("--date", date.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_INSTANT));
        return this;
    }

    /**
     * Target directory for extract
     *
     * @param path the directory path
     * @return this map of options
     */
    public JmodOptions dir(String path) {
        put("--dir", path);
        return this;
    }

    /**
     * Target directory for extract
     *
     * @param path the directory path
     * @return this map of options
     */
    public JmodOptions dir(File path) {
        return dir(path.getAbsolutePath());
    }

    /**
     * Target directory for extract
     *
     * @param path the directory path
     * @return this map of options
     */
    public JmodOptions dir(Path path) {
        return dir(path.toFile().getAbsolutePath());
    }

    /**
     * Exclude from the default root set of modules.
     *
     * @param doNotResolveByDefault {@code true} to not resolve, {@code false} otherwise
     * @return this map of options
     */
    public JmodOptions doNotResolveByDefault(boolean doNotResolveByDefault) {
        if (doNotResolveByDefault) {
            put("--do-not-resolve-by-default");
        } else {
            remove("--do-not-resolve-by-default");
        }
        return this;
    }

    /**
     * Dry run of hash mode.
     *
     * @param dryRun {@code true} for dry run, {@code false} otherwise
     * @return this list of operation
     */
    public JmodOptions dryRun(boolean dryRun) {
        if (dryRun) {
            put("--dry-run");
        } else {
            remove("--dry-run");
        }
        return this;
    }

    /**
     * Exclude files matching the supplied pattern list.
     *
     * @param patterns one or more patterns
     * @return the map of options
     */
    public JmodOptions exclude(List<FilePattern> patterns) {
        var args = new ArrayList<String>();
        for (var p : patterns) {
            if (p.type == FilePatternType.GLOB) {
                args.add("glob:" + p.pattern);
            } else if (p.type == FilePatternType.REGEX) {
                args.add("regex:" + p.pattern);
            }
        }
        put("--exclude", String.join(",", args));
        return this;
    }

    /**
     * Exclude files matching the supplied pattern list.
     *
     * @param patterns one or more patterns
     * @return the map of options
     */
    public JmodOptions exclude(FilePattern... patterns) {
        return exclude(List.of(patterns));
    }

    /**
     * Compute and record hashes to tie a packaged module with modules matching the given regular expression pattern and
     * depending upon it directly or indirectly. The hashes are recorded in the JMOD file being created, or a JMOD file
     * or modular JAR on the module path specified the jmod hash command.
     *
     * @param regexPattern the regular expression pattern
     * @return this map of options
     */
    public JmodOptions hashModules(String regexPattern) {
        put("--hash-modules", regexPattern);
        return this;
    }

    /**
     * Location of header files.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions headerFiles(String path) {
        put("--header-files", path);
        return this;
    }

    /**
     * Location of header files.
     *
     * @param path the location
     * @return this map of options
     */
    @SuppressWarnings("UnusedReturnValue")
    public JmodOptions headerFiles(File path) {
        return headerFiles(path.getAbsolutePath());
    }

    /**
     * Location of header files.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions headerFiles(Path path) {
        return headerFiles(path.toFile().getAbsolutePath());
    }

    /**
     * Location of legal notices.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions legalNotices(String path) {
        put("--legal-notices", path);
        return this;
    }

    /**
     * Location of legal notices.
     *
     * @param path the location
     * @return this map of options
     */
    @SuppressWarnings("UnusedReturnValue")
    public JmodOptions legalNotices(File path) {
        return legalNotices(path.getAbsolutePath());
    }

    /**
     * Location of legal notices.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions legalNotices(Path path) {
        return legalNotices(path.toFile().getAbsolutePath());
    }

    /**
     * Location of native libraries.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions libs(String path) {
        put("--libs", path);
        return this;
    }

    /**
     * Location of native libraries.
     *
     * @param path the location
     * @return this map of options
     */
    @SuppressWarnings("UnusedReturnValue")
    public JmodOptions libs(File path) {
        return libs(path.getAbsolutePath());
    }

    /**
     * Location of native libraries.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions libs(Path path) {
        return libs(path.toFile().getAbsolutePath());
    }

    /**
     * Main class.
     *
     * @param name the class name
     * @return this list of operation
     */
    public JmodOptions mainClass(String name) {
        put("--main-class", name);
        return this;
    }

    /**
     * Location of man pages.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions manPages(String path) {
        put("--man-pages", path);
        return this;
    }

    /**
     * Location of man pages.
     *
     * @param path the location
     * @return this map of options
     */
    @SuppressWarnings("UnusedReturnValue")
    public JmodOptions manPages(File path) {
        return manPages(path.getAbsolutePath());
    }

    /**
     * Location of man pages.
     *
     * @param path the location
     * @return this map of options
     */
    public JmodOptions manPages(Path path) {
        return manPages(path.toFile().getAbsolutePath());
    }

    /**
     * Module path.
     *
     * @param path the module path
     * @return this map of options
     */
    public JmodOptions modulePath(String path) {
        put("--module-path", path);
        return this;
    }

    /**
     * Module path.
     *
     * @param path the module path
     * @return this map of options
     */
    public JmodOptions modulePath(File path) {
        return modulePath(path.getAbsolutePath());
    }

    /**
     * Module path.
     *
     * @param path the module path
     * @return this map of options
     */
    public JmodOptions modulePath(Path path) {
        return modulePath(path.toFile().getAbsolutePath());
    }

    /**
     * Module version.
     *
     * @param version the module version.
     * @return this map of options
     */
    public JmodOptions moduleVersion(String version) {
        put("--module-version", version);
        return this;
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
     * Target platform.
     *
     * @param platform the platform
     * @return this list of operation
     */
    public JmodOptions targetPlatform(String platform) {
        put("--target-platform", platform);
        return this;
    }

    /**
     * Hint for a tool to issue a warning if the module is resolved.
     *
     * @param reason the reason
     * @return this map of options
     */
    public JmodOptions warnIfResolved(ResolvedReason reason) {
        put("--warn-if-resolved", reason.reason);
        return this;
    }

    /**
     * The resolved reasons.
     */
    public enum ResolvedReason {
        DEPRECATED("deprecated"),
        DEPRECATED_FOR_REMOVAL("deprecated-for-removal"),
        INCUBATING("incubating");

        final String reason;

        ResolvedReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * The file pattern types.
     */
    public enum FilePatternType {
        GLOB, REGEX
    }

    /**
     * Defines a file pattern and pattern type.
     *
     * @param type    the pattern type
     * @param pattern the pattern
     */
    public record FilePattern(FilePatternType type, String pattern) {
    }
}
