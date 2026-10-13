package motorph.backend.security;

import motorph.backend.exception.TooManyRequestsException;
import motorph.backend.service.MutableClock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private static final String IP = "203.0.113.7";
    private static final int PER_KEY = 3;
    private static final int PER_IP = 5;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-13T08:00:00Z"));

    private LoginRateLimiter limiter() {
        return limiter(PER_KEY, PER_IP, 100, "");
    }

    private LoginRateLimiter limiter(int perKey, int perIp, int maxEntries, String trustedProxies) {
        return new LoginRateLimiter(perKey, perIp, 1, maxEntries, trustedProxies, clock);
    }

    private static void record(LoginRateLimiter limiter, String ip, String subject, int times) {
        for (int i = 0; i < times; i++) {
            limiter.checkAndRecord(ip, subject);
        }
    }

    @Nested
    @DisplayName("Account limit")
    class AccountLimit {

        @Test
        @DisplayName("The same email from the same IP is refused after its budget")
        void sameEmailIsLimited() {
            LoginRateLimiter limiter = limiter();

            record(limiter, IP, "a@motorph.com", PER_KEY);

            assertThatThrownBy(() -> limiter.checkAndRecord(IP, "a@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);
        }

        @Test
        @DisplayName("Case and surrounding spaces do not earn a new budget")
        void emailIsNormalised() {
            LoginRateLimiter limiter = limiter();

            limiter.checkAndRecord(IP, "A@Motorph.com");
            limiter.checkAndRecord(IP, " a@motorph.com ");
            limiter.checkAndRecord(IP, "a@MOTORPH.com");

            assertThatThrownBy(() -> limiter.checkAndRecord(IP, "a@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);
        }

        @Test
        @DisplayName("One throttled email does not block other emails from the same IP")
        void otherEmailsAreNotBlocked() {
            LoginRateLimiter limiter = limiter();
            record(limiter, IP, "a@motorph.com", PER_KEY);

            assertThatCode(() -> limiter.checkAndRecord(IP, "b@motorph.com")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("A throttled email stays throttled until its window ends")
        void staysThrottledWithinWindow() {
            LoginRateLimiter limiter = limiter();
            record(limiter, IP, "a@motorph.com", PER_KEY);

            clock.advance(Duration.ofSeconds(59));

            assertThatThrownBy(() -> limiter.checkAndRecord(IP, "a@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);
        }
    }

    @Nested
    @DisplayName("IP-wide limit")
    class IpLimit {

        @Test
        @DisplayName("Changing the email on every request cannot get past the IP budget")
        void rotatingEmailsAreLimited() {
            LoginRateLimiter limiter = limiter();

            for (int i = 0; i < PER_IP; i++) {
                limiter.checkAndRecord(IP, "user" + i + "@motorph.com");
            }

            assertThatThrownBy(() -> limiter.checkAndRecord(IP, "someone-new@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);
        }

        @Test
        @DisplayName("A throttled IP does not affect another IP")
        void otherIpIsNotBlocked() {
            LoginRateLimiter limiter = limiter();
            for (int i = 0; i < PER_IP; i++) {
                limiter.checkAndRecord(IP, "user" + i + "@motorph.com");
            }
            assertThatThrownBy(() -> limiter.checkAndRecord(IP, "a@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);

            assertThatCode(() -> limiter.checkAndRecord("198.51.100.9", "a@motorph.com")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("A missing subject is treated as an empty one and is still limited")
        void missingSubjectIsStillLimited() {
            LoginRateLimiter limiter = limiter();

            record(limiter, IP, null, PER_KEY);

            assertThatThrownBy(() -> limiter.checkAndRecord(IP, null)).isInstanceOf(TooManyRequestsException.class);
        }
    }

    @Nested
    @DisplayName("Expiry and memory")
    class ExpiryAndMemory {

        @Test
        @DisplayName("Both budgets reset once the window has passed")
        void budgetsResetAfterTheWindow() {
            LoginRateLimiter limiter = limiter();
            record(limiter, IP, "a@motorph.com", PER_KEY);
            assertThatThrownBy(() -> limiter.checkAndRecord(IP, "a@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);

            clock.advance(Duration.ofMinutes(1));

            assertThatCode(() -> limiter.checkAndRecord(IP, "a@motorph.com")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Expired entries are removed, so the maps shrink when traffic stops")
        void expiredEntriesAreRemoved() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 1000, "");
            for (int i = 0; i < 50; i++) {
                limiter.checkAndRecord("192.0.2." + i, "user" + i + "@motorph.com");
            }
            assertThat(limiter.trackedWindows()).isEqualTo(100);

            clock.advance(Duration.ofMinutes(2));
            limiter.checkAndRecord(IP, "a@motorph.com");

            assertThat(limiter.trackedWindows()).isEqualTo(2);
        }

        @Test
        @DisplayName("Repeating the same email and IP does not add entries")
        void repeatedRequestsReuseTheirEntry() {
            LoginRateLimiter limiter = limiter(100, 100, 100, "");

            record(limiter, IP, "a@motorph.com", 50);

            assertThat(limiter.trackedWindows()).isEqualTo(2);
        }

        @Test
        @DisplayName("When the maps are full of live entries, new keys are refused instead of growing without limit")
        void mapSizeIsCapped() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 3, "");
            for (int i = 0; i < 3; i++) {
                limiter.checkAndRecord("192.0.2." + i, "user@motorph.com");
            }

            assertThatThrownBy(() -> limiter.checkAndRecord("192.0.2.99", "user@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);
            assertThat(limiter.trackedWindows()).isLessThanOrEqualTo(6);
        }

        @Test
        @DisplayName("Once entries expire, the full maps accept new keys again")
        void fullMapRecoversAfterExpiry() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 3, "");
            for (int i = 0; i < 3; i++) {
                limiter.checkAndRecord("192.0.2." + i, "user@motorph.com");
            }

            clock.advance(Duration.ofMinutes(1));

            assertThatCode(() -> limiter.checkAndRecord("192.0.2.99", "user@motorph.com")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Client address")
    class ClientAddress {

        private MockHttpServletRequest request(String remote, String forwardedFor) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(remote);
            if (forwardedFor != null) {
                request.addHeader("X-Forwarded-For", forwardedFor);
            }
            return request;
        }

        @Test
        @DisplayName("Without trusted proxies the connection address is used and X-Forwarded-For is ignored")
        void forwardedHeaderIsIgnoredByDefault() {
            assertThat(limiter().clientAddress(request("203.0.113.7", "1.2.3.4"))).isEqualTo("203.0.113.7");
        }

        @Test
        @DisplayName("A client cannot dodge the IP budget by sending a different X-Forwarded-For each time")
        void rotatingForwardedHeaderCannotBypassTheBudget() {
            LoginRateLimiter limiter = limiter();

            for (int i = 0; i < PER_IP; i++) {
                limiter.checkAndRecord(request("203.0.113.7", "10.0.0." + i), "user" + i + "@motorph.com");
            }

            assertThatThrownBy(() ->
                    limiter.checkAndRecord(request("203.0.113.7", "10.0.1.1"), "another@motorph.com"))
                    .isInstanceOf(TooManyRequestsException.class);
        }

        @Test
        @DisplayName("A header from a connection that is not a trusted proxy is ignored")
        void headerFromUntrustedConnectionIsIgnored() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 100, "10.0.0.5");

            assertThat(limiter.clientAddress(request("203.0.113.7", "1.2.3.4"))).isEqualTo("203.0.113.7");
        }

        @Test
        @DisplayName("Behind a trusted proxy the client is the address the proxy saw")
        void trustedProxyForwardsTheClient() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 100, "10.0.0.5");

            assertThat(limiter.clientAddress(request("10.0.0.5", "198.51.100.20"))).isEqualTo("198.51.100.20");
        }

        @Test
        @DisplayName("Entries a client prepends to X-Forwarded-For are not believed")
        void spoofedLeadingEntriesAreSkipped() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 100, "10.0.0.5");

            assertThat(limiter.clientAddress(request("10.0.0.5", "6.6.6.6, 198.51.100.20")))
                    .isEqualTo("198.51.100.20");
        }

        @Test
        @DisplayName("Several trusted proxies in the chain are skipped")
        void trustedHopsAreSkipped() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 100, "10.0.0.5, 10.0.0.6");

            assertThat(limiter.clientAddress(request("10.0.0.5", "198.51.100.20, 10.0.0.6")))
                    .isEqualTo("198.51.100.20");
        }

        @Test
        @DisplayName("A trusted proxy that sends no header, or only trusted hops, falls back to its own address")
        void missingOrUselessHeaderFallsBack() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 100, "10.0.0.5, 10.0.0.6");

            assertThat(limiter.clientAddress(request("10.0.0.5", null))).isEqualTo("10.0.0.5");
            assertThat(limiter.clientAddress(request("10.0.0.5", "10.0.0.6"))).isEqualTo("10.0.0.5");
            assertThat(limiter.clientAddress(request("10.0.0.5", " , "))).isEqualTo("10.0.0.5");
        }

        @Test
        @DisplayName("An oversized forwarded value is not used as a key")
        void oversizedHopFallsBack() {
            LoginRateLimiter limiter = limiter(PER_KEY, PER_IP, 100, "10.0.0.5");

            assertThat(limiter.clientAddress(request("10.0.0.5", "x".repeat(200)))).isEqualTo("10.0.0.5");
        }
    }
}
