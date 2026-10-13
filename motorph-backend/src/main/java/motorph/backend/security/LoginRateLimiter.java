package motorph.backend.security;

import motorph.backend.exception.TooManyRequestsException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Limits repeated requests to public authentication endpoints with two fixed-window budgets: one
 * per client IP and submitted login or recovery value, and one per client IP across all values, so
 * changing the email on every request does not earn a fresh budget. Authenticator codes are also
 * limited to five attempts per challenge.
 *
 * <p>Expired windows are removed, and each map holds at most {@code maxEntries} windows. When a map
 * is full of live windows, new keys are refused until some expire.
 *
 * <p>The client IP is the address of the connection. {@code X-Forwarded-For} is read only when that
 * connection comes from one of the configured trusted proxies.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ADDRESS_LENGTH = 45;

    private static final class Window {
        private final long startMillis;
        private int count;

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }

    private final Map<String, Window> keyWindows = new HashMap<>();
    private final Map<String, Window> ipWindows = new HashMap<>();
    private final int maxAttemptsPerKey;
    private final int maxAttemptsPerIp;
    private final long windowMillis;
    private final int maxEntries;
    private final Set<String> trustedProxies;
    private final Clock clock;
    private long lastCleanupMillis;

    public LoginRateLimiter(
            @Value("${motorph.auth.login-rate-limit.max-attempts:10}") int maxAttemptsPerKey,
            @Value("${motorph.auth.login-rate-limit.max-attempts-per-ip:60}") int maxAttemptsPerIp,
            @Value("${motorph.auth.login-rate-limit.window-minutes:1}") long windowMinutes,
            @Value("${motorph.auth.login-rate-limit.max-entries:10000}") int maxEntries,
            @Value("${motorph.auth.trusted-proxies:}") String trustedProxies,
            Clock clock) {
        this.maxAttemptsPerKey = maxAttemptsPerKey;
        this.maxAttemptsPerIp = maxAttemptsPerIp;
        this.windowMillis = windowMinutes * 60_000L;
        this.maxEntries = maxEntries;
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(address -> !address.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.clock = clock;
        this.lastCleanupMillis = clock.millis();
    }

    public void checkAndRecord(HttpServletRequest request, String subject) {
        checkAndRecord(clientAddress(request), subject);
    }

    public synchronized void checkAndRecord(String clientAddress, String subject) {
        long now = clock.millis();

        removeExpired(now);
        hit(ipWindows, clientAddress, maxAttemptsPerIp, now);
        hit(keyWindows, clientAddress + "|" + (subject == null ? "" : subject.trim().toLowerCase()),
                maxAttemptsPerKey, now);
    }

    synchronized int trackedWindows() {
        return keyWindows.size() + ipWindows.size();
    }

    String clientAddress(HttpServletRequest request) {
        String remote = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");

        if (!trustedProxies.contains(remote) || forwarded == null) {
            return remote;
        }

        String[] hops = forwarded.split(",");

        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();

            if (hop.isEmpty() || trustedProxies.contains(hop)) {
                continue;
            }

            return hop.length() <= MAX_ADDRESS_LENGTH ? hop : remote;
        }

        return remote;
    }

    private void hit(Map<String, Window> windows, String key, int maxAttempts, long now) {
        Window window = windows.get(key);

        if (window == null || isExpired(window, now)) {
            if (window == null && windows.size() >= maxEntries) {
                throw tooManyRequests();
            }

            window = new Window(now);
            windows.put(key, window);
        }

        window.count++;

        if (window.count > maxAttempts) {
            throw tooManyRequests();
        }
    }

    private void removeExpired(long now) {
        boolean full = keyWindows.size() >= maxEntries || ipWindows.size() >= maxEntries;

        if (!full && now - lastCleanupMillis < windowMillis) {
            return;
        }

        keyWindows.values().removeIf(window -> isExpired(window, now));
        ipWindows.values().removeIf(window -> isExpired(window, now));
        lastCleanupMillis = now;
    }

    private boolean isExpired(Window window, long now) {
        return now - window.startMillis >= windowMillis;
    }

    private TooManyRequestsException tooManyRequests() {
        return new TooManyRequestsException("Too many attempts. Please try again later.");
    }
}
