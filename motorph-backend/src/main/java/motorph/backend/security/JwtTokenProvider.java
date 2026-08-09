package motorph.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Mints and parses short-lived, minimal-claim JWTs. Deliberately does NOT
 * embed roles/permissions - those are always resolved fresh from the DB per
 * request (see CurrentUserProvider) so a role change takes effect on the
 * user's very next request instead of waiting for a stale token to expire.
 */
@Component
public class JwtTokenProvider {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessExpiryMinutes;
    private final long refreshExpiryMinutes;

    public JwtTokenProvider(
            @Value("${motorph.jwt.secret}") String secret,
            @Value("${motorph.jwt.access-expiry-minutes:15}") long accessExpiryMinutes,
            @Value("${motorph.jwt.refresh-expiry-minutes:1440}") long refreshExpiryMinutes) {

        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "motorph.jwt.secret must be set to a random value of at least 32 bytes. "
                            + "Refusing to start with a missing or weak signing key.");
        }

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMinutes = accessExpiryMinutes;
        this.refreshExpiryMinutes = refreshExpiryMinutes;
    }

    public IssuedToken generateAccessToken(int userAccountId, int employeeId) {
        return generate(userAccountId, employeeId, TOKEN_TYPE_ACCESS, accessExpiryMinutes);
    }

    public IssuedToken generateRefreshToken(int userAccountId, int employeeId) {
        return generate(userAccountId, employeeId, TOKEN_TYPE_REFRESH, refreshExpiryMinutes);
    }

    private IssuedToken generate(int userAccountId, int employeeId, String tokenType, long expiryMinutes) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiryMinutes * 60);

        String token = Jwts.builder()
                .subject(String.valueOf(userAccountId))
                .claim("employeeId", employeeId)
                .claim("type", tokenType)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, jti, expiresAt, userAccountId, employeeId);
    }

    // parses and validates signature/expiry; throws JwtException on any failure
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(claims.get("type", String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(claims.get("type", String.class));
    }

    public record IssuedToken(String token, String jti, Instant expiresAt, int userAccountId, int employeeId) {
    }
}
