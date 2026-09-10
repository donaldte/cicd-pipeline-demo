package tech.squadplus.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Minimal demo web service used to exercise a full CI/CD pipeline.
 *
 * <p>On purpose this has zero third-party runtime dependencies: it is built entirely on {@code
 * com.sun.net.httpserver}, part of the JDK since Java 6. The goal of this repository is to
 * demonstrate the pipeline (CI gates, semantic-release, CD to dev, manual CD with approvals) -- not
 * to showcase a framework. See README.md.
 */
public class Application {

  private static final int DEFAULT_PORT = 8080;

  private final GreetingService greetingService = new GreetingService();

  public static void main(String[] args) throws IOException {
    int port = readPort();
    HttpServer server = new Application().createServer(port);
    server.start();
    System.out.println("cicd-pipeline-demo listening on port " + port);
  }

  /** Package-private so the test suite can spin up a real server on an ephemeral port. */
  HttpServer createServer(int port) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/health", this::handleHealth);
    server.createContext("/api/greeting", this::handleGreeting);
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    return server;
  }

  private void handleHealth(HttpExchange exchange) throws IOException {
    if (!requireGet(exchange)) {
      return;
    }
    respond(exchange, 200, Json.object("status", "UP"));
  }

  private void handleGreeting(HttpExchange exchange) throws IOException {
    if (!requireGet(exchange)) {
      return;
    }
    Map<String, String> params = queryParams(exchange.getRequestURI());
    try {
      String message = greetingService.greet(params.get("name"));
      respond(exchange, 200, Json.object("message", message));
    } catch (IllegalArgumentException e) {
      respond(exchange, 400, Json.object("error", e.getMessage()));
    }
  }

  private boolean requireGet(HttpExchange exchange) throws IOException {
    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
      return true;
    }
    respond(exchange, 405, Json.object("error", "method not allowed"));
    return false;
  }

  private void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
    exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
    exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'none'");
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private Map<String, String> queryParams(URI uri) {
    String query = uri.getRawQuery();
    if (query == null || query.isBlank()) {
      return Map.of();
    }
    Map<String, String> result = new HashMap<>();
    for (String pair : query.split("&")) {
      int idx = pair.indexOf('=');
      if (idx < 0) {
        continue;
      }
      String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
      String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
      result.put(key, value);
    }
    return result;
  }

  private static int readPort() {
    String env = System.getenv("PORT");
    if (env == null || env.isBlank()) {
      return DEFAULT_PORT;
    }
    return Integer.parseInt(env.trim());
  }
}
