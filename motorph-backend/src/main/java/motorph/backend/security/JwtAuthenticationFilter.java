package motorph.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import motorph.backend.repository.TokenRevocationRepository;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Checks Bearer access tokens on authenticated requests.
 * The token is verified for validity, expiration, and revocation.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenRevocationRepository tokenRevocationRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   TokenRevocationRepository tokenRevocationRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenRevocationRepository = tokenRevocationRepository;
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

            // A password reset signs the user out everywhere, which individual token IDs cannot express.
            if (tokenRevocationRepository.isIssuedBeforeSessionRevocation(userAccountId, claims.getIssuedAt().toInstant())) {
                return;
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
