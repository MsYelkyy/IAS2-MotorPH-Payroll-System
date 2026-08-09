package motorph.backend.security;

import motorph.backend.exception.UnauthorizedException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Replaces the desktop's process-wide UserSession singleton. Every call
 * resolves the caller fresh from the current request's SecurityContext
 * (populated by JwtAuthenticationFilter from the validated token) - safe
 * under concurrent multi-client requests, unlike a static singleton.
 */
@Component
public class CurrentUserProvider {

    public AuthenticatedPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user for this request.");
        }

        return principal;
    }

    public int getCurrentUserAccountId() {
        return getCurrentPrincipal().userAccountId();
    }
}
