package motorph.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.TokenRevocationRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Issues access and refresh tokens and handles refresh and revocation. Revocation is also checked
 * on refresh, so a revoked refresh token cannot be used to obtain new access tokens.
 */
@Service
public class TokenIssuanceService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationRepository tokenRevocationRepository;

    public TokenIssuanceService(JwtTokenProvider jwtTokenProvider,
                                TokenRevocationRepository tokenRevocationRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationRepository = tokenRevocationRepository;
    }

    public TokenResponse issueTokenPair(int userAccountId, int employeeId) {
        JwtTokenProvider.IssuedToken access = jwtTokenProvider.generateAccessToken(userAccountId, employeeId);
        JwtTokenProvider.IssuedToken refresh = jwtTokenProvider.generateRefreshToken(userAccountId, employeeId);

        return new TokenResponse(
                access.token(),
                refresh.token(),
                access.expiresAt().getEpochSecond() - java.time.Instant.now().getEpochSecond()
        );
    }

    /**
     * Exchanges a valid, non-revoked refresh token for a new access token. The refresh token itself
     * is returned unchanged.
     */
    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseOrThrow(refreshToken);

        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new AuthenticationException("Token is not a refresh token.");
        }

        String jti = claims.getId();

        if (tokenRevocationRepository.isRevoked(jti)) {
            throw new AuthenticationException("Refresh token has been revoked.");
        }

        int userAccountId = Integer.parseInt(claims.getSubject());
        int employeeId = claims.get("employeeId", Integer.class);

        JwtTokenProvider.IssuedToken newAccess = jwtTokenProvider.generateAccessToken(userAccountId, employeeId);

        return new TokenResponse(
                newAccess.token(),
                refreshToken,
                newAccess.expiresAt().getEpochSecond() - java.time.Instant.now().getEpochSecond()
        );
    }

    /**
     * Marks a token as revoked. The entry is kept until the token would have expired anyway.
     */
    public void revoke(String jti, int userAccountId, java.time.Instant expiresAt) {
        LocalDateTime expiresAtLocal = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault());
        tokenRevocationRepository.revoke(jti, userAccountId, expiresAtLocal);
    }

    public Claims parseOrThrow(String token) {
        try {
            return jwtTokenProvider.parseClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationException("Token is invalid or expired.");
        }
    }
}
