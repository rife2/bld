/*
 * Copyright 2024 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Options for jmod tool.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 2.0.2
 */
public class JmodOptions extends HashMap<String, String> {
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
     * Date and time for the timestamps of entries.
     *
     * @param date the date
     * @return this map of options
     */
    public JmodOptions date(ZonedDateTime date) {
        put("--date", date.format(DateTimeFormatter.ISO_INSTANT));
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
     * @param pattern one or more pattern
     * @return the map of options
     */
    public JmodOptions exclude(FilePattern... pattern) {
        var args = new ArrayList<String>();
        for (var p : pattern) {
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
