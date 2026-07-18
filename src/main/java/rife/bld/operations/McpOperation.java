/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.BaseProject;
import rife.bld.BldVersion;
import rife.bld.BuildExecutor;
import rife.bld.operations.exceptions.OperationOptionException;
import rife.bld.wrapper.Wrapper;
import rife.json.Json;
import rife.json.JsonArray;
import rife.json.JsonObject;
import rife.json.JsonParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Serves the build commands of a project as tools over the Model Context
 * Protocol (MCP), so that AI agents can drive the build.
 * <p>
 * The server communicates over standard input and output with
 * newline-delimited JSON-RPC messages, following the MCP stdio transport.
 * Every build command is exposed as an MCP tool, invoking a tool executes
 * the corresponding command as a separate build process and returns its
 * console output, so that every call behaves exactly like a command line
 * invocation and can never disturb the protocol streams. The project
 * layout, the declared dependencies and the transitive dependency tree
 * are exposed as MCP resources. The server runs until its input stream
 * ends.
 * <p>
 * MCP support is experimental and its behavior may still change.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.4.0
 */
public class McpOperation extends AbstractOperation<McpOperation> {
    /**
     * The most recent MCP protocol version that the server implements.
     *
     * @since 2.4.0
     */
    public static final String PROTOCOL_VERSION = "2025-06-18";

    // only the batch-free revision is advertised, the 2024-11-05 and
    // 2025-03-26 revisions mandate receiving JSON-RPC batches, which this
    // server doesn't implement, batching was removed in 2025-06-18
    private static final List<String> SUPPORTED_PROTOCOL_VERSIONS = List.of(PROTOCOL_VERSION);

    private static final int ERROR_PARSE = -32700;
    private static final int ERROR_INVALID_REQUEST = -32600;
    private static final int ERROR_METHOD_NOT_FOUND = -32601;
    private static final int ERROR_INVALID_PARAMS = -32602;
    private static final int ERROR_INTERNAL = -32603;
    private static final int ERROR_RESOURCE_NOT_FOUND = -32002;

    /**
     * The URI of the resource that describes the project identification and layout.
     *
     * @since 2.4.0
     */
    public static final String RESOURCE_PROJECT = "bld://project";

    /**
     * The URI of the resource that describes the declared dependencies and BOMs.
     *
     * @since 2.4.0
     */
    public static final String RESOURCE_DEPENDENCIES = "bld://dependencies";

    /**
     * The URI of the resource that describes the transitive dependency tree.
     *
     * @since 2.4.0
     */
    public static final String RESOURCE_DEPENDENCY_TREE = "bld://dependency-tree";

    // the RFC 5424 syslog severities that MCP logging uses, from the least
    // to the most severe, the streamed build output is sent at info
    private static final List<String> LOG_LEVELS = List.of(
        "debug", "info", "notice", "warning", "error", "critical", "alert", "emergency");

    /**
     * The command argument that registers the MCP server with a client
     * instead of starting it.
     * @since 2.4.0
     */
    public static final String ARGUMENT_INSTALL = "install";

    private static final String ARGUMENT_INSTALL_PRINT = "--print";
    private static final String INSTALL_TARGET_CLAUDE = "claude";
    private static final String INSTALL_TARGET_CURSOR = "cursor";
    private static final String INSTALL_TARGET_VSCODE = "vscode";
    private static final List<String> INSTALL_TARGETS = List.of(INSTALL_TARGET_CLAUDE, INSTALL_TARGET_CURSOR, INSTALL_TARGET_VSCODE);

    private BuildExecutor executor_;
    private BaseProject project_ = null;
    private String serverTitle_ = null;
    private String instructions_ = null;
    private String installTarget_ = null;
    private boolean installPrint_ = false;
    private boolean initialized_ = false;
    private int toolCallTimeout_ = 0;
    private int outputLimit_ = 1_000_000;
    private final Set<String> excludedCommands_ = new LinkedHashSet<>(List.of("mcp"));
    private final Set<String> confirmationCommands_ = new LinkedHashSet<>(List.of("publish"));
    private final Set<Process> activeProcesses_ = ConcurrentHashMap.newKeySet();
    // the writer of the running server loop, log notifications from the
    // output reader threads are interleaved with the responses through it
    private volatile PrintWriter notificationWriter_ = null;
    private volatile String logLevel_ = "info";
    private final Object writeLock_ = new Object();

    private static final class ResourceNotFoundException extends RuntimeException {
        ResourceNotFoundException(String uri) {
            super("Resource not found: " + uri);
        }
    }

    // signals a client error in the request parameters, distinct from an
    // internal server failure, so that the two are classified differently
    private static final class InvalidParamsException extends RuntimeException {
        InvalidParamsException(String message) {
            super(message);
        }
    }

    /**
     * Performs the MCP operation, serving requests from standard input
     * until it ends.
     * <p>
     * While the server runs, the console output of the executed build
     * commands is captured into the tool results so that it never
     * corrupts the protocol stream.
     *
     * @throws IOException when an error occurred while reading or writing
     * @since 2.4.0
     */
    public void execute()
    throws IOException {
        // the install argument registers the server with an MCP client
        // instead of starting it
        if (installTarget_ != null) {
            executeInstall();
            return;
        }

        var previous_output = System.out;
        var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        // the protocol messages are written to the real standard output
        // descriptor, even when System.out was redirected during startup
        var writer = new PrintWriter(new FileOutputStream(FileDescriptor.out), false, StandardCharsets.UTF_8);
        // a terminated server never leaves tool call processes behind
        var cleanup = new Thread(() -> {
            for (var process : activeProcesses_) {
                destroyProcessTree(process);
            }
        });
        Runtime.getRuntime().addShutdownHook(cleanup);
        // stray output outside of tool calls is thrown away so that
        // standard output only carries the protocol messages
        System.setOut(new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8));
        try {
            executeServerLoop(reader, writer);
        } finally {
            for (var process : activeProcesses_) {
                destroyProcessTree(process);
            }
            try {
                Runtime.getRuntime().removeShutdownHook(cleanup);
            } catch (IllegalStateException e) {
                // the shutdown is already in progress
            }
            System.setOut(previous_output);
        }
    }

    /**
     * Part of the {@link #execute} operation, reads newline-delimited
     * JSON-RPC messages from the reader and writes the responses to the
     * writer until the input ends.
     *
     * @param reader the reader to read the protocol messages from
     * @param writer the writer to write the protocol responses to
     * @throws IOException when an error occurred while reading or writing
     * @since 2.4.0
     */
    protected void executeServerLoop(BufferedReader reader, PrintWriter writer)
    throws IOException {
        // the writer is shared with the output streaming of tool calls
        // while the loop runs
        notificationWriter_ = writer;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                var response = processMessage(line);
                if (response != null) {
                    writeProtocolMessage(writer, response);
                }
            }
        } finally {
            notificationWriter_ = null;
        }
    }

    // responses and log notifications are written as complete lines under
    // a shared lock, so that concurrent streaming can never interleave
    // inside a message
    private void writeProtocolMessage(PrintWriter writer, String message) {
        synchronized (writeLock_) {
            writer.print(message);
            writer.print('\n');
            writer.flush();
        }
    }

    /**
     * Part of the {@link #execute} operation, sends the console output of
     * a running tool call as an MCP log message notification.
     * <p>
     * The output is sent at the {@code info} level with the command name
     * as the logger, when the client set a more severe minimum level with
     * {@code logging/setLevel} the output isn't streamed. Nothing is sent
     * when no server loop is running.
     *
     * @param command the name of the build command that produced the output
     * @param data    the chunk of console output to send
     * @since 2.4.0
     */
    protected void sendToolCallOutput(String command, String data) {
        var writer = notificationWriter_;
        if (writer == null || LOG_LEVELS.indexOf(logLevel_) > LOG_LEVELS.indexOf("info")) {
            return;
        }
        var notification = new JsonObject()
            .set("jsonrpc", "2.0")
            .set("method", "notifications/message")
            .object("params", p -> p
                .set("level", "info")
                .set("logger", command)
                .set("data", data))
            .toString();
        writeProtocolMessage(writer, notification);
    }

    /**
     * Part of the {@link #execute} operation, registers the MCP server
     * with an MCP client by writing the standard configuration file
     * inside the project directory.
     * <p>
     * The registered command launches the wrapper jar directly through
     * {@code java}, the wrapper scripts are platform specific while the
     * same committed configuration file is shared by every platform.
     * An existing configuration file is merged with: other servers are
     * preserved and repeated installs update the same entry. A file that
     * can't be parsed is never modified.
     *
     * @throws IOException when the configuration couldn't be read or written
     * @since 2.4.0
     */
    protected void executeInstall()
    throws IOException {
        if (project_ == null) {
            throw new OperationOptionException("ERROR: The MCP install requires a project.");
        }

        var name = project_.name() == null ? "bld" : project_.name();
        var wrapper_jar = "lib/bld/" + Wrapper.WRAPPER_PREFIX + ".jar";
        var entry = new JsonObject();
        if (INSTALL_TARGET_VSCODE.equals(installTarget_)) {
            entry.set("type", "stdio");
        }
        // the entry replicates the wrapper script invocation: the wrapper
        // jar is launched in build mode with the project's build class,
        // the leading script path lets the wrapper derive the project
        // directory, the client launches the server with the project
        // directory as the working directory so the relative paths resolve
        entry.set("command", "java")
            .array("args", a -> a
                .append("-jar")
                .append(wrapper_jar)
                .append("./bld")
                .append(Wrapper.BUILD_ARGUMENT)
                .append(project_.getClass().getName())
                .append(Wrapper.USE_STDERR_ARGUMENT)
                .append("mcp"));

        var root_key = INSTALL_TARGET_VSCODE.equals(installTarget_) ? "servers" : "mcpServers";
        var relative_path = switch (installTarget_) {
            case INSTALL_TARGET_CURSOR -> ".cursor/mcp.json";
            case INSTALL_TARGET_VSCODE -> ".vscode/mcp.json";
            default -> ".mcp.json";
        };

        if (installPrint_) {
            System.out.println(new JsonObject().object(root_key, r -> r.set(name, entry)).toPrettyString());
            return;
        }

        var file = new File(project_.workDirectory(), relative_path);
        JsonObject config;
        if (file.exists()) {
            try {
                config = Json.parseObject(Files.readString(file.toPath()));
            } catch (JsonParseException e) {
                throw new OperationOptionException("ERROR: Unable to parse the existing '" + relative_path + "', not modifying it.");
            }
        } else {
            config = new JsonObject();
        }
        var existing_root = config.get(root_key);
        JsonObject servers;
        if (existing_root == null) {
            servers = new JsonObject();
            config.set(root_key, servers);
        } else if (existing_root instanceof JsonObject existing_servers) {
            servers = existing_servers;
        } else {
            throw new OperationOptionException("ERROR: '" + root_key + "' in '" + relative_path + "' isn't an object, not modifying it.");
        }
        servers.set(name, entry);

        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), config.toPrettyString() + "\n");

        if (!silent()) {
            System.out.println("The MCP server '" + name + "' was registered in '" + relative_path + "'.");
            System.out.println("The registered command launches the wrapper through 'java', which has to be on the PATH of the MCP client.");
            if (!new File(project_.workDirectory(), wrapper_jar).isFile()) {
                System.out.println("Warning: '" + wrapper_jar + "' wasn't found, the registered command expects the project wrapper.");
            }
        }
    }

    /**
     * Part of the {@link #execute} operation, processes a single JSON-RPC
     * message and returns the response.
     *
     * @param message the JSON-RPC message to process
     * @return the JSON-RPC response; or {@code null} when the message is a
     * notification that doesn't warrant one
     * @since 2.4.0
     */
    protected String processMessage(String message) {
        Object parsed;
        try {
            parsed = Json.parse(message);
        } catch (JsonParseException e) {
            return errorResponse(null, ERROR_PARSE, "Parse error");
        }

        // syntactically valid JSON that isn't a valid JSON-RPC request
        // envelope is an invalid request, not a parse error
        if (!(parsed instanceof JsonObject request)) {
            return errorResponse(null, ERROR_INVALID_REQUEST, "Invalid request");
        }
        var id = request.get("id");
        // a message that isn't a structurally valid request object is an
        // invalid request, even without an id, its error uses a null id
        // when the id is absent or itself invalid
        if (!"2.0".equals(request.get("jsonrpc")) || !(request.get("method") instanceof String method)) {
            return errorResponse(isValidId(id) ? id : null, ERROR_INVALID_REQUEST, "Invalid request");
        }
        // a valid request object without an id is a notification,
        // notifications are never answered, not even for parameter errors,
        // and request methods must never execute as a notification
        if (!request.containsKey("id")) {
            return null;
        }
        // an id has to be a string or an integer, an explicitly null or
        // fractional id is invalid
        if (!isValidId(id)) {
            return errorResponse(null, ERROR_INVALID_REQUEST, "Invalid request");
        }
        // the params, when present, have to be a structured object, an
        // explicit null is not the same as an omitted params
        if (request.containsKey("params") && !(request.get("params") instanceof JsonObject)) {
            return errorResponse(id, ERROR_INVALID_PARAMS, "Invalid params");
        }
        var params = (JsonObject) request.get("params");

        // besides pings, requests are only served after initialization
        if (!initialized_ && !"initialize".equals(method) && !"ping".equals(method)) {
            return errorResponse(id, ERROR_INVALID_REQUEST, "Server not initialized");
        }

        JsonObject result;
        try {
            result = switch (method) {
                case "initialize" -> processInitialize(params);
                case "ping" -> new JsonObject();
                case "tools/list" -> {
                    validateCursor(params);
                    yield processToolsList();
                }
                case "tools/call" -> processToolsCall(params);
                case "resources/list" -> {
                    validateCursor(params);
                    yield processResourcesList();
                }
                case "resources/read" -> processResourcesRead(params);
                case "logging/setLevel" -> processLoggingSetLevel(params);
                default -> null;
            };
        } catch (InvalidParamsException e) {
            // a client error in the request parameters
            return errorResponse(id, ERROR_INVALID_PARAMS, e.getMessage() == null ? "Invalid params" : e.getMessage());
        } catch (ResourceNotFoundException e) {
            return errorResponse(id, ERROR_RESOURCE_NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            // any other failure, including internal exceptions raised while
            // handling a valid request, is a server error, the server
            // survives it instead of terminating the process
            return errorResponse(id, ERROR_INTERNAL, e.getMessage() == null ? "Internal error" : e.getMessage());
        }

        if ("initialize".equals(method) && result != null) {
            initialized_ = true;
        }

        if (result == null) {
            return errorResponse(id, ERROR_METHOD_NOT_FOUND, "Method not found: " + method);
        }
        return new JsonObject()
            .set("jsonrpc", "2.0")
            .set("id", id)
            .set("result", result)
            .toString();
    }

    private static void validateCursor(JsonObject params) {
        // MCP pagination cursors are strings, this server doesn't paginate
        // but a malformed cursor is still an invalid parameter
        if (params != null && params.containsKey("cursor") && !(params.get("cursor") instanceof String)) {
            throw new InvalidParamsException("The cursor has to be a string");
        }
    }

    private static boolean isValidId(Object id) {
        if (id instanceof String) {
            return true;
        }
        if (id instanceof Long) {
            return true;
        }
        // JSON-RPC ids that are numbers have to be integers
        if (id instanceof Double number) {
            return number == Math.floor(number) && !number.isInfinite();
        }
        return false;
    }

    private static String errorResponse(Object id, int code, String message) {
        return new JsonObject()
            .set("jsonrpc", "2.0")
            .set("id", id)
            .object("error", e -> e
                .set("code", code)
                .set("message", message))
            .toString();
    }

    /**
     * Part of the {@link #execute} operation, handles the MCP
     * initialization handshake.
     *
     * @param params the parameters of the initialize request
     * @return the initialization result
     * @since 2.4.0
     */
    protected JsonObject processInitialize(JsonObject params) {
        if (params == null ||
            !(params.get("protocolVersion") instanceof String requested_version) ||
            !(params.get("capabilities") instanceof JsonObject) ||
            !(params.get("clientInfo") instanceof JsonObject client_info) ||
            !(client_info.get("name") instanceof String) ||
            !(client_info.get("version") instanceof String)) {
            throw new InvalidParamsException("Invalid initialize params");
        }
        var version = SUPPORTED_PROTOCOL_VERSIONS.contains(requested_version) ? requested_version : PROTOCOL_VERSION;
        return new JsonObject()
            .set("protocolVersion", version)
            .object("capabilities", c -> c
                .object("tools", t -> {})
                .object("resources", r -> {})
                .object("logging", l -> {}))
            .object("serverInfo", s -> {
                s.set("name", "bld");
                if (serverTitle_ != null) {
                    s.set("title", serverTitle_);
                }
                s.set("version", BldVersion.getVersion());
            })
            .set("instructions", instructions_ == null ? defaultInstructions() : instructions_);
    }

    /**
     * Part of the {@link #execute} operation, handles the MCP
     * {@code logging/setLevel} request.
     * <p>
     * The minimum level determines whether the console output of running
     * tool calls is streamed as log message notifications, the output is
     * sent at the {@code info} level.
     *
     * @param params the parameters of the set level request
     * @return the empty set level result
     * @since 2.4.0
     */
    protected JsonObject processLoggingSetLevel(JsonObject params) {
        if (params == null ||
            !(params.get("level") instanceof String level) ||
            !LOG_LEVELS.contains(level)) {
            throw new InvalidParamsException("Invalid logging level");
        }
        logLevel_ = level;
        return new JsonObject();
    }

    /**
     * Part of the {@link #execute} operation, generates the default
     * instructions that the server reports during initialization when
     * none were provided.
     *
     * @return the default server instructions
     * @since 2.4.0
     */
    protected String defaultInstructions() {
        var target = serverTitle_ == null ? "a bld build" : "the bld project '" + serverTitle_ + "'";
        var instructions = new StringBuilder();
        instructions.append("This server exposes the build commands of ").append(target).append(" as tools. ")
            .append("bld is a build tool for Java projects whose build logic is written in plain Java: ")
            .append("the build is defined by a Java class in the src/bld/java directory, and every build ")
            .append("command is a method of that class that executes immediately, without a daemon. ")
            .append("Dependencies and build behavior are changed by editing the build class, not by ")
            .append("editing XML or build scripts. ")
            .append("Calling a tool executes the corresponding build command and returns its console output, ")
            .append("optional command line arguments are passed through the 'arguments' array of the tool input.");
        if (project_ != null) {
            instructions.append(" The bld://project and bld://dependencies resources describe the project ")
                .append("without executing anything, the bld://dependency-tree resource resolves the ")
                .append("transitive dependency tree on demand.");
        }
        instructions.append(" Every tool call runs as a separate build process and picks up changes to ")
            .append("the build sources, the tool listing and the project resources reflect the state ")
            .append("at server start though: commands that were added to the build sources are callable ")
            .append("by name even before they appear in the listing.");
        if (!confirmationCommands_.isEmpty()) {
            instructions.append(" Tools whose description requires explicit human confirmation must not ")
                .append("be called without the human approving that specific call first.");
        }
        return instructions.toString();
    }

    /**
     * Part of the {@link #execute} operation, lists the build commands
     * as MCP tools.
     *
     * @return the tool listing result
     * @since 2.4.0
     */
    protected JsonObject processToolsList() {
        var tools = new JsonArray();
        for (var entry : executor_.buildCommands().entrySet()) {
            if (excludedCommands_.contains(entry.getKey())) {
                continue;
            }
            var help = entry.getValue().getHelp();
            var summary = help.getSummary();
            var details = help.getDescription(entry.getKey());
            var description = summary;
            if (details != null && !details.isBlank()) {
                description = summary == null || summary.isBlank() ? details : summary + "\n\n" + details;
            }
            var confirmation = confirmationCommands_.contains(entry.getKey());
            if (confirmation) {
                description = description + "\n\nThis command has significant external effects, " +
                              "obtain explicit human confirmation before calling it.";
            }
            var tool_description = description;
            tools.object(t -> {
                t.set("name", entry.getKey());
                if (serverTitle_ != null) {
                    // the title distinguishes the same commands of multiple
                    // projects in human-facing tool listings
                    t.set("title", entry.getKey() + " (" + serverTitle_ + ")");
                }
                t.set("description", tool_description)
                    .object("inputSchema", s -> s
                        .set("type", "object")
                        .object("properties", p -> p
                            .object("arguments", a -> a
                                .set("type", "array")
                                .object("items", i -> i.set("type", "string"))
                                .set("description", "The command line arguments to pass to the build command"))));
                t.set("outputSchema", toolOutputSchema());
                if (confirmation) {
                    t.object("annotations", a -> a
                        .set("destructiveHint", true)
                        .set("openWorldHint", true));
                }
            });
        }
        return new JsonObject().set("tools", tools);
    }

    /**
     * Part of the {@link #execute} operation, builds the output schema
     * that describes the structured content of the tool call results.
     * <p>
     * The structure captures the execution metadata that is reliably
     * known for every command: the command name, the exit status of the
     * build process, whether the call timed out or its output was
     * truncated, and the execution duration. The console output itself
     * stays in the text content.
     *
     * @return the output schema of the tool call results
     * @since 2.4.0
     */
    protected JsonObject toolOutputSchema() {
        return new JsonObject()
            .set("type", "object")
            .object("properties", p -> p
                .object("command", c -> c
                    .set("type", "string")
                    .set("description", "The build command that was executed"))
                .object("exitStatus", e -> e
                    .set("type", "integer")
                    .set("description", "The exit status of the build process, 0 for success, -1 when it couldn't be determined"))
                .object("timedOut", t -> t
                    .set("type", "boolean")
                    .set("description", "Whether the command was terminated after the tool call timeout"))
                .object("truncated", t -> t
                    .set("type", "boolean")
                    .set("description", "Whether the console output was truncated at the output limit"))
                .object("durationMs", d -> d
                    .set("type", "integer")
                    .set("description", "The wall clock duration of the whole tool call process in milliseconds, including the JVM and wrapper startup")))
            .array("required", r -> r
                .append("command")
                .append("exitStatus")
                .append("timedOut")
                .append("truncated")
                .append("durationMs"));
    }

    /**
     * Part of the {@link #execute} operation, executes a build command
     * for an MCP tool call.
     *
     * @param params the parameters of the tool call
     * @return the tool call result with the captured console output
     * @since 2.4.0
     */
    protected JsonObject processToolsCall(JsonObject params) {
        if (params == null || !(params.get("name") instanceof String name)) {
            throw new InvalidParamsException("Missing tool name");
        }
        // the exclusions are checked against the command that the name
        // would actually resolve to, so that aliases, prefixes and fuzzy
        // matches can't reach excluded commands either
        var resolved = executor_.resolveCommand(name);
        if (excludedCommands_.contains(name) ||
            (resolved != null && excludedCommands_.contains(resolved))) {
            throw new InvalidParamsException("Unknown tool: " + name);
        }

        var arguments = new ArrayList<String>();
        // the tool arguments, when present, have to be an object, an
        // explicit null is not the same as an omitted arguments
        if (params.containsKey("arguments") && !(params.get("arguments") instanceof JsonObject)) {
            throw new InvalidParamsException("The tool arguments have to be an object");
        }
        var tool_arguments = (JsonObject) params.get("arguments");
        if (tool_arguments != null && tool_arguments.containsKey("arguments")) {
            // the inner arguments array has to be an array of strings
            if (!(tool_arguments.get("arguments") instanceof JsonArray argument_list)) {
                throw new InvalidParamsException("The arguments have to be an array");
            }
            for (var argument : argument_list) {
                if (!(argument instanceof String string)) {
                    throw new InvalidParamsException("The arguments have to be strings");
                }
                arguments.add(string);
            }
        }

        // tools that aren't part of the startup listing are still executed,
        // the build process is the authority on the commands that exist,
        // commands that were added to the build sources are callable even
        // before the listing refreshes
        return executeToolCall(name, arguments);
    }

    /**
     * Part of the {@link #execute} operation, executes a single build
     * command with the provided arguments as a separate build process,
     * capturing its console output.
     * <p>
     * Running every tool call as a separate process makes it behave
     * exactly like a command line invocation: one-shot operations always
     * execute, changes to the build classes are picked up, and the
     * process can never disturb the protocol streams of the server.
     *
     * @param command   the name of the build command to execute
     * @param arguments the arguments to pass to the build command
     * @return the tool call result with the captured console output
     * @since 2.4.0
     */
    protected JsonObject executeToolCall(String command, List<String> arguments) {
        return executeToolCall(command, arguments, true);
    }

    /**
     * Part of the {@link #execute} operation, executes a single build
     * command with the provided arguments as a separate build process,
     * capturing its console output.
     * <p>
     * The command name and the excluded commands are passed to the build
     * through the control file, so that they can never get mixed up with
     * the command's own arguments. The build reports an unknown or
     * excluded command through a status file, so that it can never be
     * confused with a regular exit status of a build command.
     *
     * @param command           the name of the build command to execute
     * @param arguments          the arguments to pass to the build command
     * @param enforceExclusions  whether the excluded commands are refused,
     *                           the internal resource generation doesn't
     *                           enforce them
     * @return the tool call result with the captured console output
     * @since 2.4.0
     */
    protected JsonObject executeToolCall(String command, List<String> arguments, boolean enforceExclusions) {
        return executeToolCall(command, arguments, enforceExclusions, true);
    }

    /**
     * Part of the {@link #execute} operation, executes a single build
     * command with the provided arguments as a separate build process,
     * capturing its console output.
     *
     * @param command           the name of the build command to execute
     * @param arguments          the arguments to pass to the build command
     * @param enforceExclusions  whether the excluded commands are refused
     * @param streamOutput       whether the console output is streamed as
     *                           log message notifications while the
     *                           command runs, the internal resource
     *                           generation doesn't stream
     * @return the tool call result with the captured console output
     * @since 2.4.0
     */
    protected JsonObject executeToolCall(String command, List<String> arguments, boolean enforceExclusions, boolean streamOutput) {
        var output = new StringBuilder();
        var truncated = new boolean[]{false};
        // the descendants are collected while the process is alive, so that
        // background children that were reparented before the process
        // ended are still terminated
        var descendants = ConcurrentHashMap.<ProcessHandle>newKeySet();
        Process process;
        File control_file = null;
        File status_file = null;
        try {
            // the command, the flags and the excluded commands go through a
            // control file that the build deletes as soon as it's read, so
            // that they never get mixed up with the command's arguments and
            // never leak into nested processes
            // the temporary files are created with owner-only permissions,
            // the control file can carry sensitive command arguments
            control_file = Files.createTempFile("bld-mcp-control", ".txt").toFile();
            status_file = Files.createTempFile("bld-mcp-status", ".txt").toFile();
            // the command, its arguments, the flags and the exclusions all
            // travel through the control file, nothing user-controlled is on
            // the command line
            McpControl.write(control_file, command, arguments,
                executor_.offline(), executor_.verbose(), executor_.showStacktrace(),
                enforceExclusions ? excludedCommands_ : List.of());
            var builder = new ProcessBuilder(toolCallCommand(command, arguments));
            if (project_ != null) {
                builder.directory(project_.workDirectory());
            }
            builder.redirectErrorStream(true);
            builder.environment().put(McpToolRunner.ENV_CONTROL_FILE, control_file.getAbsolutePath());
            builder.environment().put(McpToolRunner.ENV_STATUS_FILE, status_file.getAbsolutePath());
            process = builder.start();
        } catch (IOException e) {
            // failing to set up or start the tool call process is a server
            // failure, not a failure of the tool itself, the temporary
            // files are always cleaned up
            deleteQuietly(control_file);
            deleteQuietly(status_file);
            throw new RuntimeException("Unable to start the tool call process", e);
        }

        var final_status_file = status_file;
        var final_control_file = control_file;
        try {
            return runToolCallProcess(command, process, descendants, output, truncated, final_status_file, streamOutput);
        } finally {
            deleteQuietly(final_control_file);
            deleteQuietly(final_status_file);
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null) {
            file.delete();
        }
    }


    private JsonObject runToolCallProcess(String command, Process process, Set<ProcessHandle> descendants,
                                          StringBuilder output, boolean[] truncated, File status_file, boolean stream_output) {

        var error = false;
        var timed_out = false;
        // -1 signals that the exit status couldn't be determined, for
        // instance when the wait for the process was interrupted
        var exit_status = -1;
        var start_time = System.currentTimeMillis();
        try {
            // the process is tracked so that it's terminated when the
            // server shuts down
            activeProcesses_.add(process);
            // the tool call never reads from the protocol stream
            process.getOutputStream().close();

            var alive = process;
            var sampler = new Thread(() -> {
                while (alive.isAlive()) {
                    alive.descendants().forEach(descendants::add);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            });
            sampler.setDaemon(true);
            sampler.start();

            // the output is collected on a separate thread so that the
            // process can be timed out while it's still producing output,
            // and it's capped so that it can never exhaust the memory of
            // the server
            var process_output = process.getInputStream();
            var reader = new Thread(() -> {
                try (var stream = new InputStreamReader(process_output, StandardCharsets.UTF_8)) {
                    var buffer = new char[8192];
                    int read;
                    while ((read = stream.read(buffer)) != -1) {
                        String streamed = null;
                        synchronized (output) {
                            var remaining = outputLimit_ - output.length();
                            if (remaining > 0) {
                                var appended = Math.min(read, remaining);
                                output.append(buffer, 0, appended);
                                if (stream_output) {
                                    streamed = new String(buffer, 0, appended);
                                }
                            }
                            if (read > remaining) {
                                truncated[0] = true;
                            }
                        }
                        // the captured chunk is also streamed live as a log
                        // message notification, outside of the output lock
                        if (streamed != null) {
                            sendToolCallOutput(command, streamed);
                        }
                    }
                } catch (IOException e) {
                    // the stream ended, the process is gone
                }
            });
            reader.setDaemon(true);
            reader.start();

            if (toolCallTimeout_ > 0) {
                timed_out = !process.waitFor(toolCallTimeout_, TimeUnit.SECONDS);
                if (timed_out) {
                    destroyProcessTree(process, descendants);
                }
            }
            exit_status = process.waitFor();
            error = exit_status != 0 || timed_out;
            // a background child can keep the output pipe open, the join
            // is bounded so that it can never block the server
            reader.join(10_000);

            if (timed_out) {
                synchronized (output) {
                    output.append("\nThe command timed out after ").append(toolCallTimeout_).append(" seconds and was terminated.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = true;
            synchronized (output) {
                output.append(e).append('\n');
            }
        } catch (IOException e) {
            error = true;
            synchronized (output) {
                output.append(e.getMessage() != null ? e.getMessage() : e.getClass().getName()).append('\n');
            }
        } finally {
            // the process and the descendants it spawned are terminated
            destroyProcessTree(process, descendants);
            activeProcesses_.remove(process);
        }

        // an unknown or excluded command comes back through the status file
        // and is a protocol error, the build is the authority on the commands
        // that exist and are available
        String status;
        try {
            status = Files.readString(status_file.toPath()).trim();
        } catch (IOException e) {
            status = "";
        }
        if (!timed_out) {
            // an unknown or excluded command is a client error, a runner
            // setup failure is a server error, both are reported out of
            // band so that they're never confused with a regular exit
            // status of a build command
            if (McpToolRunner.STATUS_UNKNOWN_COMMAND.equals(status) || McpToolRunner.STATUS_EXCLUDED_COMMAND.equals(status)) {
                throw new InvalidParamsException("Unknown tool: " + command);
            }
            if (McpToolRunner.STATUS_RUNNER_ERROR.equals(status)) {
                throw new RuntimeException("The tool call process couldn't be set up");
            }
        }

        String output_text;
        boolean truncated_result;
        synchronized (output) {
            truncated_result = truncated[0];
            if (truncated_result) {
                output.append("\nThe output was truncated after ").append(outputLimit_).append(" characters.");
            }
            output_text = output.toString();
        }
        var error_result = error;
        var timed_out_result = timed_out;
        var exit_status_result = exit_status;
        var duration = System.currentTimeMillis() - start_time;
        // besides the console output, the result carries the execution
        // metadata as structured content, matching the output schema of
        // the tool listing
        return new JsonObject()
            .array("content", c -> c.object(o -> o
                .set("type", "text")
                .set("text", output_text)))
            .object("structuredContent", s -> s
                .set("command", command)
                .set("exitStatus", exit_status_result)
                .set("timedOut", timed_out_result)
                .set("truncated", truncated_result)
                .set("durationMs", duration))
            .set("isError", error_result);
    }

    private static void destroyProcessTree(Process process) {
        destroyProcessTree(process, Set.of());
    }

    private static void destroyProcessTree(Process process, Set<ProcessHandle> descendants) {
        // the descendants that were sampled while the process was alive
        // are terminated too, the wrapper launches the actual build as a
        // child and commands can spawn their own children
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        descendants.forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    /**
     * Part of the {@link #execute} operation, composes the command line
     * that executes a single tool call as a separate build process.
     * <p>
     * The project's wrapper is launched directly through {@code java}
     * when it's available, avoiding the platform specific wrapper
     * scripts, so that changes to the build sources are compiled and
     * picked up. Otherwise the build executor's class is launched with
     * the class path of the server, it then has to provide a standard
     * {@code main} method. The command name, the offline, verbose and
     * stacktrace state, and the excluded commands are passed through the
     * control file, only the command's own arguments are on the command
     * line.
     *
     * @param command   the name of the build command to execute
     * @param arguments the arguments to pass to the build command
     * @return the command line for the tool call process
     * @since 2.4.0
     */
    protected List<String> toolCallCommand(String command, List<String> arguments) {
        // the JVM properties of the original invocation carry over into
        // the tool call processes, when the platform exposes the process
        // arguments
        var inherited_properties = new ArrayList<String>();
        ProcessHandle.current().info().arguments().ifPresent(process_arguments -> {
            for (var argument : process_arguments) {
                if (argument.startsWith("-D")) {
                    inherited_properties.add(argument);
                }
            }
        });
        // the output is forced to UTF-8 so that it's decoded reliably
        // regardless of the platform default encoding, these come after the
        // inherited properties so that they always win
        var forced_properties = List.of(
            "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8");

        var command_line = new ArrayList<String>();
        command_line.add(new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath());
        var wrapper_jar = wrapperJar();
        if (wrapper_jar != null) {
            // the outer wrapper JVM is forced to UTF-8 too so that its
            // diagnostics are decoded reliably
            command_line.addAll(forced_properties);
            command_line.add("-jar");
            command_line.add(wrapper_jar.getAbsolutePath());
            // the wrapper derives the project directory from the parent
            // of the first argument, just like the wrapper scripts do
            command_line.add(new File(project_.workDirectory(), "bld").getAbsolutePath());
            command_line.add(Wrapper.BUILD_ARGUMENT);
            // the wrapper launches the MCP tool runner, which drives a
            // single command of the build executor class
            command_line.add(McpToolRunner.class.getName());
            if (executor_.offline()) {
                // the wrapper recognizes the offline option right after the
                // launched class, so that it doesn't check or download the
                // distribution and extensions, the build takes its offline
                // state from the control file
                command_line.add("--offline");
            }
            command_line.add(executor_.getClass().getName());
        } else {
            // without a wrapper the build runs on the server's own class
            // path, this is the fallback for setups that have no wrapper,
            // the wrapper path is used for real projects
            command_line.addAll(inherited_properties);
            command_line.addAll(forced_properties);
            command_line.add("-cp");
            // relative class path entries are resolved against the server's
            // own working directory, the process may run in another one
            var class_path = new StringBuilder();
            for (var entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
                if (!class_path.isEmpty()) {
                    class_path.append(File.pathSeparator);
                }
                class_path.append(new File(entry).getAbsolutePath());
            }
            command_line.add(class_path.toString());
            // the MCP tool runner drives a single command of the build
            // executor class, passed as its last argument
            command_line.add(McpToolRunner.class.getName());
            command_line.add(executor_.getClass().getName());
        }
        if (wrapper_jar != null) {
            // the wrapper moves the JVM property arguments into the java
            // command that launches the build, the forced encoding comes
            // last so that it wins over an inherited file encoding
            command_line.addAll(inherited_properties);
            command_line.addAll(forced_properties);
        }
        // the command name, its arguments, the flags and the excluded
        // commands all go through the control file, nothing that is
        // user-controlled is on the command line, so that it can never be
        // interpreted as a command, a global flag or a JVM property
        return command_line;
    }

    /**
     * Part of the {@link #execute} operation, locates the wrapper jar
     * of the project.
     *
     * @return the wrapper jar; or {@code null} when there is no project
     * or the jar couldn't be found
     * @since 2.4.0
     */
    protected File wrapperJar() {
        if (project_ == null) {
            return null;
        }
        var jar = new File(project_.libBldDirectory(), Wrapper.WRAPPER_PREFIX + ".jar");
        if (jar.isFile()) {
            return jar;
        }
        return null;
    }

    /**
     * Part of the {@link #execute} operation, lists the project resources.
     * <p>
     * Resources are only available when the operation was configured from
     * a project.
     *
     * @return the resource listing result
     * @since 2.4.0
     */
    protected JsonObject processResourcesList() {
        var resources = new JsonArray();
        if (project_ != null) {
            resources.object(r -> r
                .set("uri", RESOURCE_PROJECT)
                .set("name", "project")
                .set("description", "The identification and directory layout of the project, values that aren't configured are omitted")
                .set("mimeType", "application/json"));
            resources.object(r -> r
                .set("uri", RESOURCE_DEPENDENCIES)
                .set("name", "dependencies")
                .set("description", "The declared dependencies and BOMs of the project, per scope")
                .set("mimeType", "application/json"));
            resources.object(r -> r
                .set("uri", RESOURCE_DEPENDENCY_TREE)
                .set("name", "dependency-tree")
                .set("description", "The transitive dependency tree of the project")
                .set("mimeType", "text/plain"));
        }
        return new JsonObject().set("resources", resources);
    }

    /**
     * Part of the {@link #execute} operation, reads a project resource.
     *
     * @param params the parameters of the resource read request
     * @return the resource contents result
     * @since 2.4.0
     */
    protected JsonObject processResourcesRead(JsonObject params) {
        if (params == null || !(params.get("uri") instanceof String uri)) {
            throw new InvalidParamsException("Missing resource URI");
        }
        if (project_ == null) {
            throw new ResourceNotFoundException(uri);
        }

        String mime_type;
        String text;
        switch (uri) {
            case RESOURCE_PROJECT -> {
                mime_type = "application/json";
                text = readProjectResource();
            }
            case RESOURCE_DEPENDENCIES -> {
                mime_type = "application/json";
                text = readDependenciesResource();
            }
            case RESOURCE_DEPENDENCY_TREE -> {
                mime_type = "text/plain";
                text = readDependencyTreeResource();
            }
            default -> throw new ResourceNotFoundException(uri);
        }

        var resource_text = text;
        var resource_mime_type = mime_type;
        return new JsonObject()
            .array("contents", c -> c.object(o -> o
                .set("uri", uri)
                .set("mimeType", resource_mime_type)
                .set("text", resource_text)));
    }

    /**
     * Part of the {@link #execute} operation, describes the project
     * identification and directory layout as JSON.
     * <p>
     * Values that aren't configured are omitted, so that their absence
     * can't be mistaken for missing data.
     *
     * @return the JSON description of the project
     * @since 2.4.0
     */
    protected String readProjectResource() {
        var json = new JsonObject();
        setIfPresent(json, "name", project_.name());
        setIfPresent(json, "version", project_.version() == null ? null : project_.version().toString());
        setIfPresent(json, "package", project_.pkg());
        setIfPresent(json, "mainClass", project_.mainClass());
        setIfPresent(json, "javaRelease", project_.javaRelease());
        var directories = new JsonObject();
        setIfPresent(directories, "work", directoryPath(project_.workDirectory()));
        setIfPresent(directories, "srcMainJava", directoryPath(project_.srcMainJavaDirectory()));
        setIfPresent(directories, "srcTestJava", directoryPath(project_.srcTestJavaDirectory()));
        setIfPresent(directories, "buildMain", directoryPath(project_.buildMainDirectory()));
        setIfPresent(directories, "buildTest", directoryPath(project_.buildTestDirectory()));
        json.set("directories", directories);
        return json.toPrettyString();
    }

    private static void setIfPresent(JsonObject json, String name, Object value) {
        if (value != null) {
            json.set(name, value);
        }
    }

    private static String directoryPath(File directory) {
        return directory == null ? null : directory.getAbsolutePath();
    }

    /**
     * Part of the {@link #execute} operation, describes the declared
     * dependencies and BOMs of the project as JSON, per scope.
     *
     * @return the JSON description of the dependencies
     * @since 2.4.0
     */
    protected String readDependenciesResource() {
        var json = new JsonObject();
        for (var entry : project_.dependencies().entrySet()) {
            json.object(entry.getKey().toString(), s -> {
                var boms = new JsonArray();
                for (var bom : entry.getValue().boms()) {
                    boms.append(bom.toString());
                }
                if (!boms.isEmpty()) {
                    s.set("boms", boms);
                }
                var declared = new JsonArray();
                for (var dependency : entry.getValue()) {
                    declared.append(dependency.toString());
                }
                s.set("dependencies", declared);
            });
        }
        return json.toPrettyString();
    }

    /**
     * Part of the {@link #execute} operation, generates the transitive
     * dependency tree of the project.
     *
     * @return the dependency tree description
     * @since 2.4.0
     */
    protected String readDependencyTreeResource() {
        // the resource generation runs the command internally and doesn't
        // enforce the tool exclusions, so that excluding the tool doesn't
        // break the still advertised resource, its output is the resource
        // itself and isn't streamed
        var result = executeToolCall("dependency-tree", List.of(), false, false);
        var text = result.getArray("content").getObject(0).getString("text");
        if (result.getBoolean("isError")) {
            throw new RuntimeException("Unable to generate the dependency tree:\n" + text);
        }
        return text;
    }

    /**
     * Configures the MCP operation from a project.
     *
     * @param project the project to configure the MCP operation from
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation fromProject(BaseProject project) {
        project_ = project;
        var operation = executor(project);
        if (project.name() != null) {
            operation = operation.serverTitle(project.name());
        }
        // the install argument and its options are consumed from the
        // command arguments
        var arguments = project.arguments();
        if (!arguments.isEmpty() && arguments.get(0).equals(ARGUMENT_INSTALL)) {
            arguments.remove(0);
            installTarget_ = INSTALL_TARGET_CLAUDE;
            if (!arguments.isEmpty() && !arguments.get(0).startsWith("-")) {
                var target = arguments.remove(0);
                if (!INSTALL_TARGETS.contains(target)) {
                    throw new OperationOptionException("ERROR: Unknown MCP install target '" + target + "', " +
                                                       "expecting " + String.join(", ", INSTALL_TARGETS) + ".");
                }
                installTarget_ = target;
            }
            if (!arguments.isEmpty() && arguments.get(0).equals(ARGUMENT_INSTALL_PRINT)) {
                arguments.remove(0);
                installPrint_ = true;
            }
        }
        return operation;
    }

    /**
     * Provides the build executor whose commands are served as tools.
     * <p>
     * A tool call runs a fresh build process that instantiates the
     * executor's class through an accessible no-arg constructor and runs
     * the single command through its public API, the class's {@code main}
     * method is not invoked. The {@code --auto-download-purge} option that
     * the MCP server itself was launched with isn't carried over to the
     * tool call processes, an automatic download and purge only happens
     * when it's configured on the project.
     *
     * @param executor the build executor to serve
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation executor(BuildExecutor executor) {
        executor_ = executor;
        return this;
    }

    /**
     * Provides the title that the server reports during initialization.
     *
     * @param title the server title
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation serverTitle(String title) {
        serverTitle_ = title;
        return this;
    }

    /**
     * Provides the instructions that the server reports during
     * initialization, replacing the generated default.
     *
     * @param instructions the server instructions
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation instructions(String instructions) {
        instructions_ = instructions;
        return this;
    }

    /**
     * Retrieves the instructions that the server reports during
     * initialization.
     *
     * @return the server instructions; or {@code null} when the generated
     * default is used
     * @since 2.4.0
     */
    public String instructions() {
        return instructions_;
    }

    /**
     * Excludes a build command from being served as a tool.
     * <p>
     * The {@code mcp} command itself is always excluded.
     *
     * @param command the name of the build command to exclude
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation excludeCommand(String command) {
        excludedCommands_.add(command);
        return this;
    }

    /**
     * Requires explicit human confirmation before a build command is
     * called as a tool.
     * <p>
     * The tool description instructs agents to obtain the confirmation
     * and the tool annotations flag the command as destructive. The
     * {@code publish} command requires confirmation by default.
     * <p>
     * This is advisory metadata for agents and clients, the server itself
     * doesn't block the call: MCP clients are responsible for enforcing
     * their tool approval flows. Use {@link #excludeCommand} to make a
     * command unavailable altogether.
     *
     * @param command the name of the build command that requires confirmation
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation requireConfirmation(String command) {
        confirmationCommands_.add(command);
        return this;
    }

    /**
     * Provides the maximum number of seconds that a tool call process is
     * allowed to run before it's terminated.
     * <p>
     * By default no timeout applies. Commands that don't terminate on
     * their own, like {@code run} for server applications, block the
     * server until their process ends, provide a timeout or exclude such
     * commands when that's a concern.
     *
     * @param seconds the tool call timeout in seconds, {@code 0} disables it
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation toolCallTimeout(int seconds) {
        toolCallTimeout_ = seconds;
        return this;
    }

    /**
     * Retrieves the maximum number of seconds that a tool call process is
     * allowed to run before it's terminated.
     *
     * @return the tool call timeout in seconds; or {@code 0} when no
     * timeout applies
     * @since 2.4.0
     */
    public int toolCallTimeout() {
        return toolCallTimeout_;
    }

    /**
     * Provides the maximum number of characters of console output that a
     * tool call collects, the remainder is truncated with a marker.
     * <p>
     * The default limit is one million characters.
     *
     * @param characters the output limit in characters
     * @return this operation instance
     * @since 2.4.0
     */
    public McpOperation outputLimit(int characters) {
        outputLimit_ = characters;
        return this;
    }

    /**
     * Retrieves the maximum number of characters of console output that a
     * tool call collects.
     *
     * @return the output limit in characters
     * @since 2.4.0
     */
    public int outputLimit() {
        return outputLimit_;
    }

    /**
     * Retrieves the names of the build commands that require explicit
     * human confirmation before they're called as tools.
     *
     * @return the command names that require confirmation
     * @since 2.4.0
     */
    public Set<String> confirmationCommands() {
        // the backing set isn't exposed, use requireConfirmation to add
        return Set.copyOf(confirmationCommands_);
    }

    /**
     * Retrieves the build executor whose commands are served as tools.
     *
     * @return the build executor
     * @since 2.4.0
     */
    public BuildExecutor executor() {
        return executor_;
    }

    /**
     * Retrieves the title that the server reports during initialization.
     *
     * @return the server title; or {@code null} when none was provided
     * @since 2.4.0
     */
    public String serverTitle() {
        return serverTitle_;
    }

    /**
     * Retrieves the names of the build commands that are excluded from
     * being served as tools.
     *
     * @return the excluded command names
     * @since 2.4.0
     */
    public Set<String> excludedCommands() {
        // the mcp command is always excluded, the backing set isn't exposed
        return Set.copyOf(excludedCommands_);
    }
}
