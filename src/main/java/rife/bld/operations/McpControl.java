/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The details that a single MCP tool call hands to the build process it
 * launches.
 * <p>
 * The command, its arguments, the flags and the excluded commands are
 * written to a control file instead of the command line, so they can never
 * be mistaken for the command's own arguments. The values are escaped so
 * that any character, including newlines, comes through intact. Only the
 * MCP code uses this file, the build itself knows nothing about it.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4.0
 */
final class McpControl {
    final String command;
    final List<String> arguments;
    final boolean offline;
    final boolean verbose;
    final boolean stacktrace;
    final Set<String> exclusions;

    private McpControl(String command, List<String> arguments, boolean offline, boolean verbose, boolean stacktrace, Set<String> exclusions) {
        this.command = command;
        this.arguments = arguments;
        this.offline = offline;
        this.verbose = verbose;
        this.stacktrace = stacktrace;
        this.exclusions = exclusions;
    }

    static void write(File file, String command, List<String> arguments,
                      boolean offline, boolean verbose, boolean stacktrace,
                      Collection<String> exclusions)
    throws IOException {
        var lines = new ArrayList<String>();
        lines.add("command " + escape(command));
        for (var argument : arguments) {
            lines.add("arg " + escape(argument));
        }
        if (offline) {
            lines.add("offline");
        }
        if (verbose) {
            lines.add("verbose");
        }
        if (stacktrace) {
            lines.add("stacktrace");
        }
        for (var exclusion : exclusions) {
            lines.add("exclude " + escape(exclusion));
        }
        Files.write(file.toPath(), lines);
    }

    static McpControl read(File file)
    throws IOException {
        String command = null;
        var arguments = new ArrayList<String>();
        var offline = false;
        var verbose = false;
        var stacktrace = false;
        var exclusions = new LinkedHashSet<String>();
        for (var line : Files.readAllLines(file.toPath())) {
            if (line.equals("offline")) {
                offline = true;
            } else if (line.equals("verbose")) {
                verbose = true;
            } else if (line.equals("stacktrace")) {
                stacktrace = true;
            } else if (line.startsWith("command ")) {
                command = unescape(line.substring("command ".length()));
            } else if (line.startsWith("arg ")) {
                arguments.add(unescape(line.substring("arg ".length())));
            } else if (line.startsWith("exclude ")) {
                exclusions.add(unescape(line.substring("exclude ".length())));
            }
        }
        if (command == null) {
            throw new IOException("the control file doesn't specify a command");
        }
        return new McpControl(command, arguments, offline, verbose, stacktrace, exclusions);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String value) {
        var result = new StringBuilder();
        for (var i = 0; i < value.length(); ++i) {
            var c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                var next = value.charAt(++i);
                switch (next) {
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case '\\' -> result.append('\\');
                    default -> result.append(next);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
