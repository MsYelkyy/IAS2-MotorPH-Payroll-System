package motorph.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the User-Agent the desktop app sends for the backend audit log.
 */
@DisplayName("HttpApiClient User-Agent Tests")
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
}
