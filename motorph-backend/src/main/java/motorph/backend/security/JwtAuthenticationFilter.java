package motorph.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import motorph.backend.domain.UserAccount;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Authenticates requests that carry a Bearer access token. The signature, expiry, and revocation
 * status are checked on every request, which is what lets logout take effect immediately.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final UserAccountRepository userAccountRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   TokenRevocationRepository tokenRevocationRepository,
                                   UserAccountRepository userAccountRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length()).trim();
            authenticate(token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);

            if (!jwtTokenProvider.isAccessToken(claims)) {
                return;
            }

            String jti = claims.getId();

            if (tokenRevocationRepository.isRevoked(jti)) {
                return;
            }

            int userAccountId = Integer.parseInt(claims.getSubject());

            // Realtime Active/Locked Account Check for JWT
            UserAccount account = userAccountRepository.findById(userAccountId);
            if (account == null || !account.isActive() || account.isLocked()) {
                return; // Rejects access immediately if inactive or locked
            }

            int employeeId = claims.get("employeeId", Integer.class);
            Instant expiresAt = claims.getExpiration().toInstant();

            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userAccountId, employeeId, jti, expiresAt);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {
            // Leave the request unauthenticated; Spring Security rejects it further down the chain.
        }
    }
}
