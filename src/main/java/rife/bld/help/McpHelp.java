/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.help;

import rife.bld.CommandHelp;
import rife.tools.StringUtils;

/**
 * Provides help for the MCP command.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4.0
 */
public class McpHelp implements CommandHelp {
    public String getSummary() {
        return "Starts an MCP server that exposes the build commands";
    }

    public String getDescription(String topic) {
        return StringUtils.replace("""
            Starts a Model Context Protocol (MCP) server that exposes the
            build commands as tools, so that AI agents can drive the build.
            This is an experimental feature and may still change.

            The server communicates over standard input and output with the
            MCP stdio transport and runs until its input stream ends. Launch
            bld with the --use-stderr option to keep standard output free
            for the protocol while the build starts up.

            The install argument doesn't start the server, it registers the
            project with an MCP client by writing the standard configuration
            file inside the project directory:

            install          writes .mcp.json (Claude Code and compatible)
            install cursor   writes .cursor/mcp.json
            install vscode   writes .vscode/mcp.json
            install --print  prints the configuration instead of writing it

            Existing configuration files are merged with, other servers are
            preserved. The registered command launches the wrapper directly
            through java, so that the same file works on every platform.

            Usage : ${topic} [install [claude | cursor | vscode] [--print]]""", "${topic}", topic);
    }
}
