package motorph.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.domain.UserAccount;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Creates access and refresh tokens and handles token refresh and revocation.
 * Refresh tokens are rejected if they are revoked or belong to an account
 * that is missing, inactive, or locked.
 */
@Service
public class TokenIssuanceService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final UserAccountRepository userAccountRepository;

    public TokenIssuanceService(JwtTokenProvider jwtTokenProvider,
                                TokenRevocationRepository tokenRevocationRepository,
                                UserAccountRepository userAccountRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.userAccountRepository = userAccountRepository;
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
     * Exchanges a valid, non-revoked refresh token for a new access token, provided the account is
     * still active and unlocked. The refresh token itself is returned unchanged.
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

        UserAccount account = userAccountRepository.findById(userAccountId);

        if (account == null || !account.canLogin()) {
            throw new AuthenticationException("Account is not available.");
        }

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
