package motorph.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the desktop HTTP client's request and error handling.
 */
@DisplayName("HttpApiClient Tests")
class HttpApiClientTest {

    @Test
    @DisplayName("User-Agent names the app, OS, and Java version")
    void userAgentDescribesTheClient() {
        String userAgent = HttpApiClient.USER_AGENT;

        assertTrue(userAgent.startsWith("MotorPH-Desktop/"));
        assertTrue(userAgent.contains(System.getProperty("os.name")));
        assertTrue(userAgent.contains("Java " + System.getProperty("java.version")));
        assertFalse(userAgent.contains("Java-http-client"));
    }

    @Test
    @DisplayName("User-Agent fits the device column and has no line breaks")
    void userAgentIsSafeToStore() {
        String userAgent = HttpApiClient.USER_AGENT;

        assertTrue(userAgent.length() <= 255);
        assertFalse(userAgent.chars().anyMatch(Character::isISOControl));
    }

    @Test
    @DisplayName("Java HTTP client accepts the User-Agent header")
    void httpClientAcceptsTheHeader() {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost/"))
                .header("User-Agent", HttpApiClient.USER_AGENT)
                .build();

        assertEquals(HttpApiClient.USER_AGENT, request.headers().firstValue("User-Agent").orElseThrow());
    }

    @Test
    @DisplayName("Login API errors preserve the backend's machine-readable reason")
    void loginErrorPreservesReason() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] body = """
                {"code":"AUTHENTICATION_FAILED","message":"Invalid email or password.","reason":"ACCOUNT_LOCKED"}
                """.getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/v1/auth/login", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, body.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        server.start();

        AuthTokenHolder tokenHolder = AuthTokenHolder.getInstance();
        tokenHolder.clear();
        try {
            AuthApiClient authApiClient = new AuthApiClient(
                    new HttpApiClient("http://127.0.0.1:" + server.getAddress().getPort(), tokenHolder),
                    tokenHolder);

            ApiException failure = assertThrows(
                    ApiException.class, () -> authApiClient.login("employee@motorph.com", "not-a-real-password"));

            assertEquals(401, failure.getHttpStatus());
            assertEquals("AUTHENTICATION_FAILED", failure.getCode());
            assertEquals("ACCOUNT_LOCKED", failure.getReason());
        } finally {
            tokenHolder.clear();
            server.stop(0);
        }
    }
}
