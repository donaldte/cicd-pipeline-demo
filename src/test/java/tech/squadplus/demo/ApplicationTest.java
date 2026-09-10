package tech.squadplus.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** End-to-end test: boots the real server on an ephemeral port and issues real HTTP calls. */
class ApplicationTest {

  private HttpServer server;
  private HttpClient client;
  private String baseUrl;

  @BeforeEach
  void startServer() throws IOException {
    // Port 0 lets the OS pick a free ephemeral port, so tests never clash.
    server = new Application().createServer(0);
    server.start();
    baseUrl = "http://localhost:" + server.getAddress().getPort();
    client = HttpClient.newHttpClient();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void healthEndpointReportsUp() throws Exception {
    HttpResponse<String> response = get("/health");
    assertEquals(200, response.statusCode());
    assertEquals("{\"status\":\"UP\"}", response.body());
  }

  @Test
  void greetingEndpointUsesDefaultName() throws Exception {
    HttpResponse<String> response = get("/api/greeting");
    assertEquals(200, response.statusCode());
    assertEquals("{\"message\":\"Hello, World!\"}", response.body());
  }

  @Test
  void greetingEndpointUsesProvidedName() throws Exception {
    HttpResponse<String> response = get("/api/greeting?name=Donald");
    assertEquals(200, response.statusCode());
    assertEquals("{\"message\":\"Hello, Donald!\"}", response.body());
  }

  @Test
  void greetingEndpointRejectsTooLongName() throws Exception {
    String tooLong = "a".repeat(61);
    HttpResponse<String> response = get("/api/greeting?name=" + tooLong);
    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("too long"));
  }

  @Test
  void nonGetMethodIsRejected() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(baseUrl + "/health"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    assertEquals(405, response.statusCode());
  }

  private HttpResponse<String> get(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build();
    return client.send(request, BodyHandlers.ofString());
  }
}
