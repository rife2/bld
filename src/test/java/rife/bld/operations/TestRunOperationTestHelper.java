/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TestRunOperationTestHelper {
    @Test
    void testServeThenStopWaitsForALateServer()
    throws Exception {
        // the port stays unbound until well after the retrieval starts, so
        // binding the server up front would connect straight away and test
        // nothing
        int port;
        try (var probe = new ServerSocket(0)) {
            port = probe.getLocalPort();
        }
        var url = new URL("http://localhost:" + port + "/");

        // the operation was never executed, so it has no process to stop
        var served = RunOperationTestHelper.serveThenStop(url, new RunOperation());

        Thread.sleep(3000);
        var server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/", exchange -> {
            var bytes = "<p>Hello World app</p>".getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        try {
            assertEquals("<p>Hello World app</p>", served.get(30, TimeUnit.SECONDS));
        } finally {
            server.stop(0);
        }
    }
}
