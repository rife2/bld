/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;
import rife.bld.BldVersion;
import rife.bld.BuildCommand;
import rife.bld.BuildExecutor;
import rife.bld.Project;
import rife.bld.dependencies.Bom;
import rife.bld.dependencies.Dependency;
import rife.bld.dependencies.Scope;
import rife.bld.dependencies.VersionNumber;
import rife.bld.operations.exceptions.OperationOptionException;
import rife.json.Json;
import rife.json.JsonObject;
import rife.tools.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TestMcpOperation {
    public static class McpBuild extends BuildExecutor {
        @BuildCommand(summary = "Greets the caller", alias = "hi")
        public void hello() {
            System.out.println("hello " + String.join(" ", arguments()));
            arguments().clear();
        }

        @BuildCommand(summary = "Always fails")
        public void trouble()
        throws Exception {
            throw new Exception("boom");
        }

        @BuildCommand(value = "custom-greet", summary = "Greets with a custom name", description = "Prints a custom greeting to the console.")
        public void customGreet() {
            System.out.println("custom hello");
        }

        private final OnceOperation onceOperation_ = new OnceOperation();

        @BuildCommand(summary = "Runs a one-shot operation")
        public void once()
        throws Exception {
            onceOperation_.executeOnce();
        }

        @BuildCommand(summary = "Prints the invocation state")
        public void state() {
            System.out.println("offline=" + offline() + " verbose=" + verbose());
        }

        @BuildCommand(summary = "Sleeps for a minute")
        public void sleepy()
        throws Exception {
            System.out.println("sleeping");
            Thread.sleep(60_000);
        }

        @BuildCommand(summary = "Prints a lot of output")
        public void spam() {
            for (var i = 0; i < 100; ++i) {
                System.out.println("x".repeat(100));
            }
        }

        @BuildCommand(summary = "Spawns a sleeping child process")
        public void nested()
        throws Exception {
            System.out.println("nested start");
            var child = new ProcessBuilder(
                new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath(),
                "-cp", System.getProperty("java.class.path"),
                SleepMain.class.getName())
                .inheritIO()
                .start();
            child.waitFor();
        }

        public static void main(String[] args) {
            new McpBuild().start(args);
        }
    }

    public static class SleepMain {
        public static void main(String[] args)
        throws Exception {
            Thread.sleep(60_000);
        }
    }

    static class OnceOperation extends AbstractOperation<OnceOperation> {
        public void execute() {
            System.out.println("once executed");
        }
    }

    // a build executor without a no-arg constructor can't be instantiated
    // by the tool runner, which is a server error, not a tool failure
    public static class NoDefaultConstructorBuild extends McpBuild {
        public NoDefaultConstructorBuild(String ignored) {
        }
    }

    @Test
    void testRunnerSetupFailureIsAServerError() {
        var operation = new McpOperation()
            .executor(new NoDefaultConstructorBuild("x"));
        initialize(operation);
        // the runner can't instantiate the class through a no-arg
        // constructor, this is reported as an internal server error
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hello"}}""");
        assertEquals(-32603, response.getObject("error").getInt("code"));
    }

    // commands inherited from parent classes are how bld extensions
    // contribute commands, they're served like any other command
    public static class ExtendedBuild extends McpBuild {
        @BuildCommand(summary = "Provided by the extension")
        public void extended() {
            System.out.println("extended hello");
        }

        public static void main(String[] args) {
            new ExtendedBuild().start(args);
        }
    }

    private McpBuild createBuild() {
        return new McpBuild();
    }

    private void initialize(McpOperation operation) {
        operation.processMessage("""
            {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""");
        operation.processMessage("""
            {"jsonrpc":"2.0","method":"notifications/initialized"}""");
    }

    private McpOperation createOperation() {
        var operation = new McpOperation()
            .executor(createBuild())
            .serverTitle("mcp_test");
        initialize(operation);
        return operation;
    }

    private JsonObject process(McpOperation operation, String message) {
        var response = operation.processMessage(message);
        assertNotNull(response);
        return Json.parseObject(response);
    }

    @Test
    void testInitialize() {
        var operation = createOperation();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""");

        assertEquals("2.0", response.getString("jsonrpc"));
        assertEquals(1, response.getInt("id"));
        var result = response.getObject("result");
        // a supported requested protocol version is echoed
        assertEquals("2025-06-18", result.getString("protocolVersion"));
        assertNotNull(result.getObject("capabilities").getObject("tools"));
        assertNotNull(result.getObject("capabilities").getObject("logging"));
        assertEquals("bld", result.getObject("serverInfo").getString("name"));
        assertEquals("mcp_test", result.getObject("serverInfo").getString("title"));
        assertFalse(result.getObject("serverInfo").getString("version").isEmpty());
        assertTrue(result.getString("instructions").contains("the bld project 'mcp_test'"));
        // the instructions explain the nature of bld builds
        assertTrue(result.getString("instructions").contains("src/bld/java"));
        assertTrue(result.getString("instructions").contains("editing the build class"));

        // an unsupported requested protocol version falls back to the
        // most recent supported one
        var fallback = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"initialize","params":{"protocolVersion":"1999-01-01","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""");
        assertEquals(McpOperation.PROTOCOL_VERSION, fallback.getObject("result").getString("protocolVersion"));
    }

    @Test
    void testInitializeParamsAreValidated() {
        var operation = createOperation();

        // the params are required
        var missing_params = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"initialize"}""");
        assertEquals(-32602, missing_params.getObject("error").getInt("code"));

        // the protocol version has to be a string
        var invalid_version = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"initialize","params":{"protocolVersion":7,"capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""");
        assertEquals(-32602, invalid_version.getObject("error").getInt("code"));

        // the capabilities and client info are required objects
        var missing_capabilities = process(operation, """
            {"jsonrpc":"2.0","id":3,"method":"initialize","params":{"protocolVersion":"2025-06-18","clientInfo":{"name":"test","version":"1.0"}}}""");
        assertEquals(-32602, missing_capabilities.getObject("error").getInt("code"));
        var missing_client_info = process(operation, """
            {"jsonrpc":"2.0","id":4,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{}}}""");
        assertEquals(-32602, missing_client_info.getObject("error").getInt("code"));
    }

    @Test
    void testStrictSchemaValidation() {
        var operation = createOperation();

        // fractional ids are rejected
        var fractional = process(operation, """
            {"jsonrpc":"2.0","id":1.5,"method":"ping"}""");
        assertEquals(-32600, fractional.getObject("error").getInt("code"));

        // params, when present, have to be an object even for methods that
        // ignore them
        var array_params = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"ping","params":[]}""");
        assertEquals(-32602, array_params.getObject("error").getInt("code"));

        // an incomplete client info is rejected
        var empty_client = process(operation, """
            {"jsonrpc":"2.0","id":3,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{}}}""");
        assertEquals(-32602, empty_client.getObject("error").getInt("code"));

        // a numeric tool name is rejected
        var numeric_name = process(operation, """
            {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":5}}""");
        assertEquals(-32602, numeric_name.getObject("error").getInt("code"));

        // an explicit null params is not the same as an omitted params
        var null_params = process(operation, """
            {"jsonrpc":"2.0","id":5,"method":"ping","params":null}""");
        assertEquals(-32602, null_params.getObject("error").getInt("code"));

        // a malformed envelope without an id gets an error with a null id,
        // it isn't silently discarded as a notification
        var empty_object = process(operation, "{}");
        assertEquals(-32600, empty_object.getObject("error").getInt("code"));
        assertNull(empty_object.get("id"));
        var wrong_jsonrpc = process(operation, """
            {"jsonrpc":"1.0","method":"ping"}""");
        assertEquals(-32600, wrong_jsonrpc.getObject("error").getInt("code"));
    }

    @Test
    void testRequestMethodsRequireAnId() {
        var operation = createOperation();
        // an id-less request method is a malformed notification, it must
        // not execute and gets no response
        assertNull(operation.processMessage("""
            {"jsonrpc":"2.0","method":"tools/call","params":{"name":"hello"}}"""));
    }

    @Test
    void testNotificationsAreNeverAnswered() {
        var operation = createOperation();
        // a notification with malformed params still gets no response, a
        // notification is never answered
        assertNull(operation.processMessage("""
            {"jsonrpc":"2.0","method":"ping","params":[]}"""));
        // a notification with an unknown method gets no response either
        assertNull(operation.processMessage("""
            {"jsonrpc":"2.0","method":"bogus"}"""));
    }

    @Test
    void testUnsupportedProtocolVersionsAreNotAdvertised() {
        var operation = createOperation();
        // the 2025-03-26 revision, which mandates batch support, isn't
        // offered, an unsupported request falls back to the newest
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"t","version":"1"}}}""");
        assertEquals(McpOperation.PROTOCOL_VERSION, response.getObject("result").getString("protocolVersion"));
    }

    @Test
    void testInitializedNotificationIsSilent() {
        var operation = createOperation();
        assertNull(operation.processMessage("""
            {"jsonrpc":"2.0","method":"notifications/initialized"}"""));
    }

    @Test
    void testPing() {
        var operation = createOperation();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":"ping-1","method":"ping"}""");
        assertEquals("ping-1", response.getString("id"));
        assertTrue(response.getObject("result").isEmpty());
    }

    @Test
    void testToolsList() {
        var operation = createOperation();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":3,"method":"tools/list"}""");

        var tools = response.getObject("result").getArray("tools");
        JsonObject hello = null;
        for (var element : tools) {
            if ("hello".equals(((JsonObject) element).getString("name"))) {
                hello = (JsonObject) element;
            }
        }
        assertNotNull(hello);
        // the title qualifies the command with the server title for
        // human-facing tool listings
        assertEquals("hello (mcp_test)", hello.getString("title"));
        assertEquals("Greets the caller", hello.getString("description"));
        assertEquals("object", hello.getObject("inputSchema").getString("type"));
        assertEquals("array", hello.getObject("inputSchema").getObject("properties").getObject("arguments").getString("type"));
        // the output schema describes the structured execution metadata
        var output_schema = hello.getObject("outputSchema");
        assertEquals("object", output_schema.getString("type"));
        assertEquals("integer", output_schema.getObject("properties").getObject("exitStatus").getString("type"));
        assertEquals("boolean", output_schema.getObject("properties").getObject("timedOut").getString("type"));
        assertTrue(output_schema.getArray("required").contains("exitStatus"));
    }

    @Test
    void testInstructionsOverride() {
        var operation = createOperation()
            .instructions("Custom guidance");
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""");
        assertEquals("Custom guidance", response.getObject("result").getString("instructions"));
    }

    @Test
    void testCustomAndInheritedCommands() {
        var operation = new McpOperation().executor(new ExtendedBuild());
        initialize(operation);

        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/list"}""");
        JsonObject custom = null;
        var names = new StringBuilder();
        for (var element : response.getObject("result").getArray("tools")) {
            var tool = (JsonObject) element;
            names.append(tool.getString("name")).append(' ');
            if ("custom-greet".equals(tool.getString("name"))) {
                custom = tool;
            }
        }
        // custom command names are used as the tool names, and inherited
        // commands are listed alongside the class's own
        assertTrue(names.toString().contains("extended"));
        assertTrue(names.toString().contains("hello"));
        assertNotNull(custom);
        // the tool description combines the summary and the full help text
        assertTrue(custom.getString("description").startsWith("Greets with a custom name"));
        assertTrue(custom.getString("description").contains("Prints a custom greeting to the console."));

        // both custom-named and inherited commands are callable
        var custom_call = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"custom-greet"}}""");
        assertEquals("custom hello", custom_call.getObject("result").getArray("content").getObject(0).getString("text").trim());
        var inherited_call = process(operation, """
            {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"extended"}}""");
        assertEquals("extended hello", inherited_call.getObject("result").getArray("content").getObject(0).getString("text").trim());
    }

    @Test
    void testToolsListExcludesCommands() {
        var operation = createOperation()
            .excludeCommand("trouble");
        var response = process(operation, """
            {"jsonrpc":"2.0","id":4,"method":"tools/list"}""");

        var names = new StringBuilder();
        for (var element : response.getObject("result").getArray("tools")) {
            names.append(((JsonObject) element).getString("name")).append(' ');
        }
        assertTrue(names.toString().contains("hello"));
        assertFalse(names.toString().contains("trouble"));
    }

    @Test
    void testToolsCall() {
        var operation = createOperation();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":["big","world"]}}}""");

        var result = response.getObject("result");
        assertFalse(result.getBoolean("isError"));
        var content = result.getArray("content").getObject(0);
        assertEquals("text", content.getString("type"));
        assertEquals("hello big world", content.getString("text").trim());

        // the structured content carries the execution metadata
        var structured = result.getObject("structuredContent");
        assertEquals("hello", structured.getString("command"));
        assertEquals(0, structured.getInt("exitStatus"));
        assertFalse(structured.getBoolean("timedOut"));
        assertFalse(structured.getBoolean("truncated"));
        assertTrue(structured.getLong("durationMs") >= 0);
    }

    @Test
    void testToolsCallFailure() {
        var operation = createOperation();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":"trouble"}}""");

        var result = response.getObject("result");
        assertTrue(result.getBoolean("isError"));
        assertTrue(result.getArray("content").getObject(0).getString("text").contains("boom"));
        // a failed command reports its exit status in the structured content
        assertEquals(1, result.getObject("structuredContent").getInt("exitStatus"));
    }

    @Test
    void testToolsCallRepeatsOneShotOperations() {
        var operation = createOperation();
        // each tool call behaves like a separate command line invocation,
        // one-shot operations run every time
        for (var id = 1; id <= 2; ++id) {
            var response = process(operation, """
                {"jsonrpc":"2.0","id":%d,"method":"tools/call","params":{"name":"once"}}""".formatted(id));
            var result = response.getObject("result");
            assertFalse(result.getBoolean("isError"));
            assertTrue(result.getArray("content").getObject(0).getString("text").contains("once executed"));
        }
    }

    @Test
    void testInvocationStateCarriesOver() {
        var build = createBuild();
        build.offline(true);
        build.verbose(true);
        var operation = new McpOperation().executor(build);
        initialize(operation);

        // the offline and verbose state of the original invocation
        // carries over into the tool call processes
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"state"}}""");
        assertEquals("offline=true verbose=true",
            response.getObject("result").getArray("content").getObject(0).getString("text").trim());
    }

    @Test
    void testInitializationGating() {
        var operation = new McpOperation()
            .executor(createBuild())
            .serverTitle("mcp_test");

        // pings are served before initialization, other requests are not
        var ping = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"ping"}""");
        assertNotNull(ping.getObject("result"));
        var gated = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"tools/list"}""");
        assertEquals(-32600, gated.getObject("error").getInt("code"));
        assertTrue(gated.getObject("error").getString("message").contains("not initialized"));

        // after initialization the same request is served
        initialize(operation);
        var listing = process(operation, """
            {"jsonrpc":"2.0","id":3,"method":"tools/list"}""");
        assertNotNull(listing.getObject("result").getArray("tools"));
    }

    @Test
    void testConfirmationCommands() {
        var operation = createOperation()
            .requireConfirmation("trouble");

        // the publish command requires confirmation by default
        assertTrue(operation.confirmationCommands().contains("publish"));

        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/list"}""");
        JsonObject trouble = null;
        for (var element : response.getObject("result").getArray("tools")) {
            if ("trouble".equals(((JsonObject) element).getString("name"))) {
                trouble = (JsonObject) element;
            }
        }
        assertNotNull(trouble);
        // the description instructs agents to obtain confirmation and the
        // annotations flag the command
        assertTrue(trouble.getString("description").contains("explicit human confirmation"));
        assertTrue(trouble.getObject("annotations").getBoolean("destructiveHint"));
        assertTrue(trouble.getObject("annotations").getBoolean("openWorldHint"));
    }

    @Test
    void testNonStringArgumentsAreRejected() {
        var operation = createOperation();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":[1,true]}}}""");
        assertEquals(-32602, response.getObject("error").getInt("code"));
    }

    @Test
    void testMalformedParamsDontTerminateTheServer() {
        var operation = createOperation();
        // an array where an object is expected is an invalid parameter,
        // not a server crash
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":[]}""");
        assertEquals(-32602, response.getObject("error").getInt("code"));

        // the server keeps serving afterwards
        var ping = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"ping"}""");
        assertNotNull(ping.getObject("result"));
    }

    @Test
    void testToolsCallUnknownTool() {
        var operation = createOperation();

        // unknown tools are delegated to the build process, which is the
        // authority on the commands that exist, an unknown command is
        // reported as a protocol error rather than a tool failure
        var response = process(operation, """
            {"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"nonexistent"}}""");
        assertEquals(-32602, response.getObject("error").getInt("code"));

        // excluded commands are rejected outright
        operation.excludeCommand("hello");
        var excluded = process(operation, """
            {"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"hello"}}""");
        assertEquals(-32602, excluded.getObject("error").getInt("code"));
    }

    @Test
    void testArgumentsCannotChainCommands() {
        var operation = createOperation()
            .excludeCommand("state");
        // passing an excluded command as an argument to another command
        // must not execute it, only the called command runs
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":["state"]}}}""");
        var text = response.getObject("result").getArray("content").getObject(0).getString("text");
        assertTrue(text.contains("hello state"), text);
        // the chained command's output is absent, it never executed
        assertFalse(text.contains("offline="), text);
    }

    @Test
    void testGlobalFlagArgumentsReachTheCommand() {
        var operation = createOperation();
        // a leading --offline is the command's own argument, it must reach
        // the command and not be consumed as global bld state
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":["--offline","kept"]}}}""");
        var text = response.getObject("result").getArray("content").getObject(0).getString("text");
        assertTrue(text.contains("hello --offline kept"), text);
    }

    @Test
    void testToolNameNewlineCannotInjectCommand() {
        var operation = createOperation();
        // a tool name containing a newline that would inject a second
        // command record must not execute a different command, the escaped
        // value stays a single unknown command
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"missing\\ncommand hello"}}""");
        assertEquals(-32602, response.getObject("error").getInt("code"));
    }

    @Test
    void testPropertyLikeArgumentsReachTheCommand() {
        var operation = createOperation();
        // a -D argument is the command's own argument, it must reach the
        // command in both the direct and the wrapper mode, not be consumed
        // as a JVM property
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":["-Dfoo=bar","kept"]}}}""");
        var text = response.getObject("result").getArray("content").getObject(0).getString("text");
        assertTrue(text.contains("hello -Dfoo=bar kept"), text);
    }

    @Test
    void testToolArgumentsMustBeStructured() {
        var operation = createOperation();
        // an explicit null tool arguments is not the same as omitted
        var null_args = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hello","arguments":null}}""");
        assertEquals(-32602, null_args.getObject("error").getInt("code"));
        // the inner arguments array must be an array
        var bad_list = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":"nope"}}}""");
        assertEquals(-32602, bad_list.getObject("error").getInt("code"));
        // a numeric list cursor is rejected
        var bad_cursor = process(operation, """
            {"jsonrpc":"2.0","id":3,"method":"tools/list","params":{"cursor":5}}""");
        assertEquals(-32602, bad_cursor.getObject("error").getInt("code"));
    }

    @Test
    void testUnknownToolWithHelpArgumentStillFails() {
        var operation = createOperation();
        // a --help argument to an unknown tool must not be interpreted as
        // global help, the unknown tool is still a protocol error
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"nonexistent","arguments":{"arguments":["--help"]}}}""");
        assertEquals(-32602, response.getObject("error").getInt("code"));
    }

    @Test
    void testChildProcessEnforcesExclusions() {
        // the build process itself refuses an excluded command, this bypasses
        // the server-side check and exercises the child enforcement, hi is
        // an alias of the excluded hello and is refused there
        var operation = createOperation()
            .excludeCommand("hello");
        assertThrows(RuntimeException.class,
            () -> operation.executeToolCall("hi", List.of(), true));
    }

    @Test
    void testExclusionsCoverAliasesAndMatches() {
        var operation = createOperation()
            .excludeCommand("hello");

        // aliases of excluded commands are rejected
        var alias = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"hi"}}""");
        assertEquals(-32602, alias.getObject("error").getInt("code"));

        // unique prefixes that resolve to excluded commands are rejected
        var prefix = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"hell"}}""");
        assertEquals(-32602, prefix.getObject("error").getInt("code"));
    }

    @Test
    void testToolCallTimeoutTerminatesDescendants() {
        var operation = createOperation()
            .toolCallTimeout(2);
        var start = System.currentTimeMillis();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"nested"}}""");
        var elapsed = System.currentTimeMillis() - start;

        var result = response.getObject("result");
        assertTrue(result.getBoolean("isError"));
        assertTrue(result.getArray("content").getObject(0).getString("text").contains("timed out"));
        // the wrapper-style child process is terminated too, the call
        // doesn't wait for it to end on its own
        assertTrue(elapsed < 20_000, String.valueOf(elapsed));
    }

    @Test
    void testToolCallTimeout() {
        var operation = createOperation()
            .toolCallTimeout(1);
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"sleepy"}}""");
        var result = response.getObject("result");
        assertTrue(result.getBoolean("isError"));
        assertTrue(result.getArray("content").getObject(0).getString("text").contains("timed out after 1 seconds"));
        // the timeout is reported in the structured content
        assertTrue(result.getObject("structuredContent").getBoolean("timedOut"));
    }

    @Test
    void testOutputLimit() {
        var operation = createOperation()
            .outputLimit(150);
        var response = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"spam"}}""");
        var text = response.getObject("result").getArray("content").getObject(0).getString("text");
        assertTrue(text.contains("truncated after 150 characters"), text);
        assertTrue(text.length() < 300, String.valueOf(text.length()));
        // the truncation is reported in the structured content
        assertTrue(response.getObject("result").getObject("structuredContent").getBoolean("truncated"));
    }

    @Test
    void testJvmPropertiesCarryOver() {
        var operation = createOperation();
        var command_line = operation.toolCallCommand("hello", List.of());

        // the JVM properties of the original invocation are part of the
        // tool call command lines
        ProcessHandle.current().info().arguments().ifPresent(process_arguments -> {
            for (var argument : process_arguments) {
                if (argument.startsWith("-D")) {
                    assertTrue(command_line.contains(argument), argument);
                }
            }
        });
        // the encoding is forced to UTF-8
        assertTrue(command_line.contains("-Dfile.encoding=UTF-8"), command_line.toString());
        // the command name goes through the control file, not the command
        // line, so that arguments can never be interpreted as commands
        assertFalse(command_line.contains("hello"), command_line.toString());
    }

    @Test
    void testUnknownMethod() {
        var operation = createOperation();
        var response = process(operation, """
            {"jsonrpc":"2.0","id":8,"method":"bogus/method"}""");
        assertEquals(-32601, response.getObject("error").getInt("code"));

        // unknown notifications don't warrant a response
        assertNull(operation.processMessage("""
            {"jsonrpc":"2.0","method":"bogus/notification"}"""));
    }

    @Test
    void testInvalidRequestEnvelopes() {
        var operation = createOperation();

        // syntactically valid JSON that isn't a request object
        var array = process(operation, "[1,2]");
        assertEquals(-32600, array.getObject("error").getInt("code"));
        assertNull(array.get("id"));

        // the jsonrpc version is required
        var missing_version = process(operation, """
            {"id":1,"method":"ping"}""");
        assertEquals(-32600, missing_version.getObject("error").getInt("code"));
        var wrong_version = process(operation, """
            {"jsonrpc":"1.0","id":1,"method":"ping"}""");
        assertEquals(-32600, wrong_version.getObject("error").getInt("code"));
        assertEquals(1, wrong_version.getInt("id"));

        // the method has to be a string
        var invalid_method = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":5}""");
        assertEquals(-32600, invalid_method.getObject("error").getInt("code"));

        // the id has to be a string or a number
        var invalid_id = process(operation, """
            {"jsonrpc":"2.0","id":{},"method":"ping"}""");
        assertEquals(-32600, invalid_id.getObject("error").getInt("code"));
        assertNull(invalid_id.get("id"));

        // an explicitly null id is invalid, it's not a notification
        var null_id = process(operation, """
            {"jsonrpc":"2.0","id":null,"method":"ping"}""");
        assertEquals(-32600, null_id.getObject("error").getInt("code"));
        assertNull(null_id.get("id"));
    }

    @Test
    void testParseError() {
        var operation = createOperation();
        var response = process(operation, "this is not json");
        assertEquals(-32700, response.getObject("error").getInt("code"));
        assertNull(response.get("id"));
    }

    public static class McpProject extends Project {
        public McpProject() {
            this(new File(System.getProperty("user.dir")));
        }

        McpProject(File tmp) {
            workDirectory = tmp;
            pkg = "test.pkg";
            name = "mcp_project";
            version = new VersionNumber(1, 0, 0);
            repositories = List.of();
        }

        public static void main(String[] args) {
            new McpProject().start(args);
        }
    }

    public static class FailingTreeProject extends McpProject {
        public FailingTreeProject() {
        }

        FailingTreeProject(File tmp) {
            super(tmp);
        }

        {
            // a dependency without any repository to resolve it in
            dependencies().scope(Scope.compile)
                .include(new Dependency("com.example", "missing", new VersionNumber(1, 0, 0)));
        }

        public static void main(String[] args) {
            new FailingTreeProject().start(args);
        }
    }

    @Test
    void testResourcesWithoutProject() {
        var operation = createOperation();
        initialize(operation);
        var listing = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"resources/list"}""");
        assertTrue(listing.getObject("result").getArray("resources").isEmpty());

        var read = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"resources/read","params":{"uri":"bld://project"}}""");
        assertEquals(-32002, read.getObject("error").getInt("code"));
    }

    @Test
    void testResources()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpresources").toFile();
        try {
            var project = new McpProject(tmp);
            project.dependencies().scope(Scope.compile)
                .include(new Bom("com.example", "bom1", new VersionNumber(1, 0, 0)))
                .include(new Dependency("com.example", "a"));
            project.dependencies().scope(Scope.test)
                .include(new Dependency("com.example", "t", new VersionNumber(2, 0, 0)));
            var operation = new McpOperation().fromProject(project);
            initialize(operation);

            // the three project resources are listed
            var listing = process(operation, """
                {"jsonrpc":"2.0","id":1,"method":"resources/list"}""");
            var uris = new StringBuilder();
            for (var element : listing.getObject("result").getArray("resources")) {
                uris.append(((JsonObject) element).getString("uri")).append(' ');
            }
            assertTrue(uris.toString().contains("bld://project"));
            assertTrue(uris.toString().contains("bld://dependencies"));
            assertTrue(uris.toString().contains("bld://dependency-tree"));

            // the project resource describes the identification and layout
            var read_project = process(operation, """
                {"jsonrpc":"2.0","id":2,"method":"resources/read","params":{"uri":"bld://project"}}""");
            var project_content = read_project.getObject("result").getArray("contents").getObject(0);
            assertEquals("application/json", project_content.getString("mimeType"));
            var description = Json.parseObject(project_content.getString("text"));
            assertEquals("mcp_project", description.getString("name"));
            assertEquals("1.0.0", description.getString("version"));
            assertEquals("test.pkg", description.getString("package"));
            assertNotNull(description.getObject("directories").getString("srcMainJava"));
            // values that aren't configured are omitted instead of null
            assertFalse(description.containsKey("javaRelease"));

            // the dependencies resource describes the scopes
            var read_dependencies = process(operation, """
                {"jsonrpc":"2.0","id":3,"method":"resources/read","params":{"uri":"bld://dependencies"}}""");
            var dependencies = Json.parseObject(read_dependencies.getObject("result").getArray("contents").getObject(0).getString("text"));
            assertEquals("com.example:bom1:1.0.0@bom", dependencies.getObject("compile").getArray("boms").getString(0));
            assertEquals("com.example:a", dependencies.getObject("compile").getArray("dependencies").getString(0));
            assertEquals("com.example:t:2.0.0", dependencies.getObject("test").getArray("dependencies").getString(0));

            // unknown resources report the dedicated error
            var unknown = process(operation, """
                {"jsonrpc":"2.0","id":4,"method":"resources/read","params":{"uri":"bld://bogus"}}""");
            assertEquals(-32002, unknown.getObject("error").getInt("code"));

            // a missing URI is an invalid parameter
            var missing = process(operation, """
                {"jsonrpc":"2.0","id":5,"method":"resources/read","params":{}}""");
            assertEquals(-32602, missing.getObject("error").getInt("code"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testResourceDependencyTree()
    throws Exception {
        var tmp = Files.createTempDirectory("mcptree").toFile();
        try {
            var operation = new McpOperation().fromProject(new McpProject(tmp));
            initialize(operation);
            var response = process(operation, """
                {"jsonrpc":"2.0","id":1,"method":"resources/read","params":{"uri":"bld://dependency-tree"}}""");
            var content = response.getObject("result").getArray("contents").getObject(0);
            assertEquals("text/plain", content.getString("mimeType"));
            assertTrue(content.getString("text").contains("no dependencies"), content.getString("text"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testExcludedToolDoesNotBreakResource()
    throws Exception {
        var tmp = Files.createTempDirectory("mcptreeexcl").toFile();
        try {
            var operation = new McpOperation()
                .fromProject(new McpProject(tmp))
                .excludeCommand("dependency-tree");
            initialize(operation);

            // excluding the dependency-tree tool doesn't break the still
            // advertised dependency-tree resource, its generation doesn't
            // enforce the tool exclusions
            var response = process(operation, """
                {"jsonrpc":"2.0","id":1,"method":"resources/read","params":{"uri":"bld://dependency-tree"}}""");
            var content = response.getObject("result").getArray("contents").getObject(0);
            assertTrue(content.getString("text").contains("no dependencies"), content.getString("text"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testResourceDependencyTreeReportsFailures()
    throws Exception {
        var tmp = Files.createTempDirectory("mcptreefail").toFile();
        try {
            var operation = new McpOperation().fromProject(new FailingTreeProject(tmp));
            initialize(operation);
            var response = process(operation, """
                {"jsonrpc":"2.0","id":1,"method":"resources/read","params":{"uri":"bld://dependency-tree"}}""");
            var error = response.getObject("error");
            assertEquals(-32603, error.getInt("code"));
            assertTrue(error.getString("message").contains("dependency tree"), error.getString("message"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMcpInstall()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpinstall").toFile();
        try {
            var project = new McpProject(tmp);
            project.arguments().add("install");
            var operation = new McpOperation().fromProject(project);
            // the install arguments are consumed and can't chain commands
            assertTrue(project.arguments().isEmpty());
            operation.silent(true).execute();

            var config = Json.parseObject(Files.readString(new File(tmp, ".mcp.json").toPath()));
            var server = config.getObject("mcpServers").getObject("mcp_project");
            // the wrapper is launched directly through java, so that the
            // same committed file works on every platform
            assertEquals("java", server.getString("command"));
            var args = server.getArray("args");
            assertEquals("-jar", args.getString(0));
            assertEquals("lib/bld/bld-wrapper.jar", args.getString(1));
            assertEquals("./bld", args.getString(2));
            // the wrapper is launched in build mode with the project's
            // build class, mirroring the wrapper script invocation
            assertEquals("--build", args.getString(3));
            assertEquals(McpProject.class.getName(), args.getString(4));
            assertEquals("--use-stderr", args.getString(5));
            assertEquals("mcp", args.getString(6));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMcpInstallMergesAndUpdates()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpinstallmerge").toFile();
        try {
            Files.writeString(new File(tmp, ".mcp.json").toPath(), """
                {"mcpServers":{"other":{"command":"other-cmd"}},"custom":true}""");

            for (var round = 1; round <= 2; ++round) {
                var project = new McpProject(tmp);
                project.arguments().add("install");
                new McpOperation().fromProject(project).silent(true).execute();
            }

            var config = Json.parseObject(Files.readString(new File(tmp, ".mcp.json").toPath()));
            // other servers and unrelated settings are preserved, repeated
            // installs update the same single entry
            assertEquals("other-cmd", config.getObject("mcpServers").getObject("other").getString("command"));
            assertTrue(config.getBoolean("custom"));
            assertEquals(2, config.getObject("mcpServers").size());
            assertEquals("java", config.getObject("mcpServers").getObject("mcp_project").getString("command"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMcpInstallVscode()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpinstallvscode").toFile();
        try {
            var project = new McpProject(tmp);
            project.arguments().addAll(List.of("install", "vscode"));
            new McpOperation().fromProject(project).silent(true).execute();

            var config = Json.parseObject(Files.readString(new File(tmp, ".vscode/mcp.json").toPath()));
            // the VS Code format uses the servers root and a stdio type
            var server = config.getObject("servers").getObject("mcp_project");
            assertEquals("stdio", server.getString("type"));
            assertEquals("java", server.getString("command"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMcpInstallUnknownTarget()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpinstallbogus").toFile();
        try {
            var project = new McpProject(tmp);
            project.arguments().addAll(List.of("install", "bogus"));
            assertThrows(OperationOptionException.class, () -> new McpOperation().fromProject(project));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMcpInstallRefusesCorruptConfig()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpinstallcorrupt").toFile();
        try {
            var config_file = new File(tmp, ".mcp.json");
            Files.writeString(config_file.toPath(), "this is not json");

            var project = new McpProject(tmp);
            project.arguments().add("install");
            var operation = new McpOperation().fromProject(project).silent(true);
            // a file that can't be parsed is reported and never modified
            assertThrows(OperationOptionException.class, operation::execute);
            assertEquals("this is not json", Files.readString(config_file.toPath()));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMcpInstallPrint()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpinstallprint").toFile();
        var previous_out = System.out;
        try {
            var project = new McpProject(tmp);
            project.arguments().addAll(List.of("install", "--print"));
            var captured = new java.io.ByteArrayOutputStream();
            System.setOut(new PrintStream(captured, true));
            new McpOperation().fromProject(project).execute();
            System.setOut(previous_out);

            // the configuration is printed instead of written
            var printed = Json.parseObject(captured.toString());
            assertEquals("java", printed.getObject("mcpServers").getObject("mcp_project").getString("command"));
            assertFalse(new File(tmp, ".mcp.json").exists());
        } finally {
            System.setOut(previous_out);
            FileUtils.deleteDirectory(tmp);
        }
    }

    public static class E2eProject extends Project {
        public E2eProject() {
            this(new File(System.getProperty("user.dir")));
        }

        E2eProject(File work) {
            workDirectory = work;
            pkg = "test.pkg";
            name = "myapp";
            version = new VersionNumber(0, 0, 1);
        }

        public static void main(String[] args) {
            new E2eProject().start(args);
        }
    }

    @Test
    void testMcpInstalledConfigLaunchesTheServer()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpinstalllaunch").toFile();
        try {
            var project = new E2eProject(tmp);
            project.arguments().add("install");
            new McpOperation().fromProject(project).silent(true).execute();

            // the generated entry is the contract with MCP clients, its
            // exact shape is asserted before it's used for the launch
            var config = Json.parseObject(Files.readString(new File(tmp, ".mcp.json").toPath()));
            var server = config.getObject("mcpServers").getObject("myapp");
            assertEquals("java", server.getString("command"));
            var args = server.getArray("args");
            assertEquals(List.of("-jar", "lib/bld/bld-wrapper.jar", "./bld",
                    "--build", E2eProject.class.getName(), "--use-stderr", "mcp"),
                List.of(args.toArray()));

            // the server is launched like an MCP client would: the java
            // command from the entry, an argument array without any shell,
            // and the project directory as the working directory, which
            // exercises the platform behavior of the generated entry ; the
            // wrapper jar segment would download the published bld
            // distribution instead of testing the working tree, it's
            // substituted with the class path of this test run, the wrapper
            // and script mechanics themselves are exercised by every CI
            // build of bld itself
            var command_line = new ArrayList<String>();
            command_line.add(server.getString("command"));
            command_line.add("-cp");
            command_line.add(System.getProperty("java.class.path"));
            for (var i = args.indexOf("--build") + 1; i < args.size(); ++i) {
                command_line.add(args.getString(i));
            }

            var builder = new ProcessBuilder(command_line);
            builder.directory(tmp);
            var process = builder.start();
            try {
                try (var input = process.getOutputStream()) {
                    input.write("""
                        {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
                        {"jsonrpc":"2.0","method":"notifications/initialized"}
                        {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"version"}}
                        """.getBytes(StandardCharsets.UTF_8));
                }
                var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(process.waitFor(120, TimeUnit.SECONDS), output);

                // every line on standard output is a protocol message
                var responses = new ArrayList<JsonObject>();
                var streamed = new StringBuilder();
                for (var line : output.trim().split("\n")) {
                    var message = Json.parseObject(line);
                    if ("notifications/message".equals(message.getString("method"))) {
                        streamed.append(message.getObject("params").getString("data"));
                    } else {
                        responses.add(message);
                    }
                }
                assertEquals(2, responses.size());
                assertEquals("bld", responses.get(0).getObject("result").getObject("serverInfo").getString("name"));

                // the tool call executed against the working tree and
                // reported its outcome in the structured content
                var result = responses.get(1).getObject("result");
                assertFalse(result.getBoolean("isError"));
                assertTrue(result.getArray("content").getObject(0).getString("text").contains(BldVersion.getVersion()));
                assertEquals(0, result.getObject("structuredContent").getInt("exitStatus"));
                assertTrue(streamed.toString().contains(BldVersion.getVersion()));
            } finally {
                process.destroyForcibly();
            }
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testMcpEndToEndWithRealProject()
    throws Exception {
        var tmp = Files.createTempDirectory("mcpe2e").toFile();
        try {
            var create_operation = new CreateAppOperation()
                .workDirectory(tmp)
                .packageName("test.pkg")
                .projectName("myapp")
                .downloadDependencies(true);
            create_operation.execute();
            var project_dir = new File(tmp, "myapp");
            // the generated wrapper would require a published bld
            // distribution, remove it so that the tool calls launch the
            // build class directly with the class path of this test
            new File(project_dir, "lib/bld/bld-wrapper.jar").delete();
            var project = new E2eProject(project_dir);

            var operation = new McpOperation().fromProject(project);
            var input = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
                {"jsonrpc":"2.0","method":"notifications/initialized"}
                {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"compile"}}
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"compile"}}
                {"jsonrpc":"2.0","id":4,"method":"resources/read","params":{"uri":"bld://project"}}
                """;
            var output = new StringWriter();
            operation.executeServerLoop(new BufferedReader(new StringReader(input)), new PrintWriter(output));

            // the streamed log notifications are interleaved with the
            // responses, only the responses are counted here
            var responses = new ArrayList<JsonObject>();
            for (var line : output.toString().trim().split("\n")) {
                var message = Json.parseObject(line);
                if (message.getString("method") == null) {
                    responses.add(message);
                }
            }
            assertEquals(4, responses.size());

            // both compile tool calls executed and reported their output,
            // repeated calls behave like separate command line invocations
            for (var i = 1; i <= 2; ++i) {
                var compile_result = responses.get(i).getObject("result");
                assertFalse(compile_result.getBoolean("isError"));
                assertTrue(compile_result.getArray("content").getObject(0).getString("text").contains("Compilation finished successfully"));
            }

            // the compiled classes are actually there
            var listing = FileUtils.generateDirectoryListing(project.buildMainDirectory());
            assertTrue(listing.contains(".class"), listing);

            // the project resource reflects the created project
            var read_project = responses.get(3);
            var description = Json.parseObject(read_project.getObject("result").getArray("contents").getObject(0).getString("text"));
            assertEquals("myapp", description.getString("name"));
        } finally {
            FileUtils.deleteDirectory(tmp);
        }
    }

    @Test
    void testToolCallOutputIsStreamed()
    throws Exception {
        var operation = createOperation();
        var input = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
            {"jsonrpc":"2.0","method":"notifications/initialized"}
            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":["stream"]}}}
            """;
        var output = new StringWriter();
        operation.executeServerLoop(new BufferedReader(new StringReader(input)), new PrintWriter(output));

        var lines = output.toString().trim().split("\n");
        // the console output arrives live as log message notifications
        // before the tool call response
        var streamed = new StringBuilder();
        var notifications = 0;
        for (var line : lines) {
            var message = Json.parseObject(line);
            if ("notifications/message".equals(message.getString("method"))) {
                ++notifications;
                var params = message.getObject("params");
                assertEquals("info", params.getString("level"));
                assertEquals("hello", params.getString("logger"));
                streamed.append(params.getString("data"));
            }
        }
        assertTrue(notifications >= 1);
        assertTrue(streamed.toString().contains("hello stream"), streamed.toString());

        // the tool call response still carries the full output and comes
        // after the streamed notifications
        var last = Json.parseObject(lines[lines.length - 1]);
        assertEquals(2, last.getInt("id"));
        assertTrue(last.getObject("result").getArray("content").getObject(0).getString("text").contains("hello stream"));
    }

    @Test
    void testLoggingSetLevelSuppressesStreaming()
    throws Exception {
        var operation = createOperation();
        var input = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
            {"jsonrpc":"2.0","method":"notifications/initialized"}
            {"jsonrpc":"2.0","id":2,"method":"logging/setLevel","params":{"level":"warning"}}
            {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":["quiet"]}}}
            """;
        var output = new StringWriter();
        operation.executeServerLoop(new BufferedReader(new StringReader(input)), new PrintWriter(output));

        var lines = output.toString().trim().split("\n");
        // a more severe minimum level suppresses the output streaming,
        // only the three responses are written
        assertEquals(3, lines.length);
        for (var line : lines) {
            assertNull(Json.parseObject(line).getString("method"));
        }
        // the tool call response still carries the full output
        var last = Json.parseObject(lines[2]);
        assertTrue(last.getObject("result").getArray("content").getObject(0).getString("text").contains("hello quiet"));
    }

    @Test
    void testLoggingSetLevelValidation() {
        var operation = createOperation();

        // the params and a valid RFC 5424 level are required
        var missing = process(operation, """
            {"jsonrpc":"2.0","id":1,"method":"logging/setLevel"}""");
        assertEquals(-32602, missing.getObject("error").getInt("code"));
        var bogus = process(operation, """
            {"jsonrpc":"2.0","id":2,"method":"logging/setLevel","params":{"level":"loud"}}""");
        assertEquals(-32602, bogus.getObject("error").getInt("code"));

        // a valid level is acknowledged with an empty result
        var valid = process(operation, """
            {"jsonrpc":"2.0","id":3,"method":"logging/setLevel","params":{"level":"debug"}}""");
        assertTrue(valid.getObject("result").isEmpty());
    }

    @Test
    void testServerLoop()
    throws Exception {
        var operation = createOperation();
        var input = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
            {"jsonrpc":"2.0","method":"notifications/initialized"}

            {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"hello","arguments":{"arguments":["loop"]}}}
            """;
        var output = new StringWriter();
        operation.executeServerLoop(new BufferedReader(new StringReader(input)), new PrintWriter(output));

        // the streamed log notifications are interleaved with the
        // responses, only the responses are counted here
        var responses = new ArrayList<JsonObject>();
        for (var line : output.toString().trim().split("\n")) {
            var message = Json.parseObject(line);
            if (message.getString("method") == null) {
                responses.add(message);
            }
        }
        assertEquals(2, responses.size());
        assertEquals(1, responses.get(0).getInt("id"));
        var call = responses.get(1);
        assertEquals(2, call.getInt("id"));
        assertEquals("hello loop", call.getObject("result").getArray("content").getObject(0).getString("text").trim());
    }
}
