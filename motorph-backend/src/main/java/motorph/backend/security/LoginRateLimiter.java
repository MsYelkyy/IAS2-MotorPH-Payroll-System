package motorph.backend.security;

import motorph.backend.exception.TooManyRequestsException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory fixed-window limiter for the public login/verify-otp
 * endpoints, keyed by source IP. Combined with TwoFactorService's existing
 * per-account 5-attempt OTP lockout, this stops account lockout from being
 * weaponized into a denial-of-service against a known username (an attacker
 * spamming one account's OTP from many IPs is still capped by the account-level
 * lockout; an attacker spamming many accounts from one IP is capped here).
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
