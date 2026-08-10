package motorph.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import motorph.api.dto.auth.ErrorResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Low-level JSON-over-HTTPS client. Deliberately uses the JDK's default
 * HttpClient/SSLContext - real certificate validation - rather than a
 * trust-all SSLContext, so this stays safe to point at a non-localhost
 * backend. Attaches the Bearer token automatically when one is held.
 */
public final class HttpApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final AuthTokenHolder tokenHolder;

    public HttpApiClient(String baseUrl, AuthTokenHolder tokenHolder) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.tokenHolder = tokenHolder;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public <T> T get(String path, Class<T> responseType) {
        return send(newRequestBuilder(path).GET(), responseType);
    }

    public <T> List<T> getList(String path, Class<T> elementType) {
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return send(newRequestBuilder(path).GET(), listType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return send(newRequestBuilder(path).POST(jsonBody(body)), responseType);
    }

    public <T> T put(String path, Object body, Class<T> responseType) {
        return send(newRequestBuilder(path).PUT(jsonBody(body)), responseType);
    }

    // for endpoints that return no body (e.g. deactivate)
    public void delete(String path) {
        send(newRequestBuilder(path).DELETE(), Void.class);
    }

    // for binary responses (e.g. a generated PDF) rather than JSON
    public byte[] getBytes(String path) {
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    newRequestBuilder(path).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            throw parseError(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8), response.statusCode());

        } catch (IOException e) {
            throw new ApiException(0, "NETWORK_ERROR", "Unable to reach the server. Please check your connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "NETWORK_ERROR", "The request was interrupted.");
        }
    }

    // for endpoints that take raw bytes rather than JSON (e.g. a photo upload)
    public void postBytes(String path, byte[] content) {
        // setHeader, not header: newRequestBuilder already applied application/json
        HttpRequest.Builder builder = newRequestBuilder(path)
                .setHeader("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(content));

        send(builder, Void.class);
    }

    // for endpoints that return no body (e.g. logout)
    public void postNoContent(String path, Object body) {
        send(newRequestBuilder(path).POST(jsonBody(body)), Void.class);
    }

    private <T> T send(HttpRequest.Builder requestBuilder, Class<T> responseType) {
        return send(requestBuilder, objectMapper.getTypeFactory().constructType(responseType));
    }

    private <T> T send(HttpRequest.Builder requestBuilder, JavaType responseType) {
        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseSuccess(response, responseType);
            }

            throw parseError(response);

        } catch (IOException e) {
            throw new ApiException(0, "NETWORK_ERROR", "Unable to reach the server. Please check your connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "NETWORK_ERROR", "The request was interrupted.");
        }
    }

    private <T> T parseSuccess(HttpResponse<String> response, JavaType responseType) {
        try {
            if (responseType.getRawClass() == Void.class || response.body() == null || response.body().isBlank()) {
                return null;
            }

            return objectMapper.readValue(response.body(), responseType);

        } catch (IOException e) {
            throw new ApiException(response.statusCode(), "MALFORMED_RESPONSE", "Received an unreadable response from the server.");
        }
    }

    private ApiException parseError(HttpResponse<String> response) {
        return parseError(response.body(), response.statusCode());
    }

    private ApiException parseError(String body, int statusCode) {
        try {
            ErrorResponse error = objectMapper.readValue(body, ErrorResponse.class);
            return new ApiException(statusCode, error.code(), error.message());
        } catch (IOException e) {
            return new ApiException(statusCode, "UNKNOWN_ERROR", "Request failed with status " + statusCode + ".");
        }
    }

    private HttpRequest.Builder newRequestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30));

        String accessToken = tokenHolder.getAccessToken();

        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        return builder;
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        try {
            String json = body == null ? "{}" : objectMapper.writeValueAsString(body);
            return HttpRequest.BodyPublishers.ofString(json);
        } catch (IOException e) {
            throw new ApiException(0, "SERIALIZATION_ERROR", "Failed to prepare the request.");
        }
    }
}
