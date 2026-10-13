package motorph.backend.security;

import motorph.backend.exception.TooManyRequestsException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits repeated requests to public authentication endpoints.
 * Requests are tracked using the client IP and the submitted login or recovery value.
 * Authenticator codes are also limited to five attempts per challenge.
 */
@Component
public class LoginRateLimiter {

    private record Window(AtomicInteger count, Instant windowStart) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;

    public LoginRateLimiter(
            @Value("${motorph.auth.login-rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${motorph.auth.login-rate-limit.window-minutes:1}") long windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMinutes * 60_000L;
    }

    public void checkAndRecord(String sourceKey) {
        Instant now = Instant.now();

        Window window = windows.compute(sourceKey, (key, existing) -> {
            if (existing == null || now.toEpochMilli() - existing.windowStart().toEpochMilli() > windowMillis) {
                return new Window(new AtomicInteger(1), now);
            }
            existing.count().incrementAndGet();
            return existing;
        });

        if (window.count().get() > maxAttempts) {
            throw new TooManyRequestsException("Too many attempts. Please try again later.");
        }
    }
}
