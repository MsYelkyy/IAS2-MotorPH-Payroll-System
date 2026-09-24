package motorph.backend.security;

import motorph.backend.exception.UnauthorizedException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Provides the authenticated caller for the current request from the Spring Security context. It
 * reads per-request state only, so it is safe when several clients are connected at once.
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
