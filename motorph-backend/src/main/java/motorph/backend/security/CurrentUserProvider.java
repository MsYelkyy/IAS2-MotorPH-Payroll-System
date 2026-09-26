package motorph.backend.security;

import motorph.backend.domain.value.Role;
import motorph.backend.exception.UnauthorizedException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Provides the authenticated caller for the current request from the Spring Security context. It
 * reads per-request state only, so it is safe when several clients are connected at once.
 */
@Component
public class CurrentUserProvider {

    public static final String ACTIVE_ROLE_HEADER = "X-Active-Role";

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

    /**
     * Returns the role the client says the user selected, or {@code null} if none was sent. This is
     * only a request; callers must confirm the account actually holds the role.
     */
    public Role getRequestedRole() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        String value = attributes.getRequest().getHeader(ACTIVE_ROLE_HEADER);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Role.fromValue(value);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Unknown role: " + value.trim());
        }
    }
}
