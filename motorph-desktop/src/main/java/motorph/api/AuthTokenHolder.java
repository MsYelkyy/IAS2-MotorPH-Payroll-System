package motorph.api;

import java.time.Instant;

/**
 * Holds the desktop's active session: an access + refresh token pair. This
 * replaces what UserSession used to be responsible for on the security side -
 * UserSession now only caches role/name for display (see LoginController). No MySQL credentials ever pass through this class.
 */
public final class AuthTokenHolder {

    private static final AuthTokenHolder INSTANCE = new AuthTokenHolder();

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile Instant accessTokenExpiresAt;

    private AuthTokenHolder() {
    }

    public static AuthTokenHolder getInstance() {
        return INSTANCE;
    }

    public synchronized void store(String accessToken, String refreshToken, long expiresInSeconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = Instant.now().plusSeconds(expiresInSeconds);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isAuthenticated() {
        return accessToken != null;
    }

    public boolean isAccessTokenExpired() {
        return accessTokenExpiresAt == null || Instant.now().isAfter(accessTokenExpiresAt);
    }

    public synchronized void clear() {
        accessToken = null;
        refreshToken = null;
        accessTokenExpiresAt = null;
    }
}
