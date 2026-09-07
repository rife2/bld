import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.Executors;

/**
 * A maven repository on the loopback interface, backed by a directory.
 * <p>
 * PUT stores, GET serves, HEAD answers what the release gates ask. Basic
 * authentication guards the writes when a user and password are given, so
 * that a rehearsal exercises the same upload path a real publication takes:
 * the PUT per file, the credentials, and the checksum files that bld only
 * generates for repositories it doesn't consider local.
 * <p>
 * Usage: java RepositoryServer.java &lt;directory&gt; [port] [user] [password]
 */
public class RepositoryServer {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: java RepositoryServer.java <directory> [port] [user] [password]");
            System.exit(1);
        }
        var root = new File(args[0]).getCanonicalFile();
        root.mkdirs();
        var port = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        var user = args.length > 2 ? args[2] : null;
        var password = args.length > 3 ? args[3] : null;

        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/", exchange -> {
            try {
                // reads are open and writes are not, the way a public maven
                // repository works: what a release gate checks is what a
                // consumer can reach, without credentials
                var writing = exchange.getRequestMethod().equals("PUT");
                if (writing && user != null && !authorized(exchange.getRequestHeaders().getFirst("Authorization"), user, password)) {
                    exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"rehearsal\"");
                    exchange.sendResponseHeaders(401, -1);
                    return;
                }
                var path = exchange.getRequestURI().getPath();
                var target = new File(root, path).getCanonicalFile();
                if (!target.toPath().startsWith(root.toPath())) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                switch (exchange.getRequestMethod()) {
                    case "PUT" -> {
                        target.getParentFile().mkdirs();
                        try (var in = exchange.getRequestBody()) {
                            Files.copy(in, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        System.out.println("PUT  " + path);
                        exchange.sendResponseHeaders(201, -1);
                    }
                    case "HEAD" -> exchange.sendResponseHeaders(target.isFile() ? 200 : 404, -1);
                    case "GET" -> {
                        if (!target.isFile()) {
                            exchange.sendResponseHeaders(404, -1);
                            return;
                        }
                        var body = Files.readAllBytes(target.toPath());
                        exchange.sendResponseHeaders(200, body.length);
                        exchange.getResponseBody().write(body);
                    }
                    default -> exchange.sendResponseHeaders(405, -1);
                }
            } catch (IOException e) {
                System.err.println("failed " + exchange.getRequestURI() + ": " + e.getMessage());
                try { exchange.sendResponseHeaders(500, -1); } catch (IOException ignored) { }
            } finally {
                exchange.close();
            }
        });
        server.start();
        System.out.println("repository at http://127.0.0.1:" + server.getAddress().getPort() + "/ serving " + root);
        System.out.flush();
    }

    private static boolean authorized(String header, String user, String password) {
        if (header == null || !header.startsWith("Basic ")) {
            return false;
        }
        var decoded = new String(Base64.getDecoder().decode(header.substring("Basic ".length())));
        return decoded.equals(user + ":" + (password == null ? "" : password));
    }
}
