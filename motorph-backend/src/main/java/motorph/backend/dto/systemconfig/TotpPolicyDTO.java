package motorph.backend.dto.systemconfig;

/**
 * The authenticator-code rules enforced by the backend, exposed so the system configuration screen
 * shows the live values instead of its own copy.
 */
public record TotpPolicyDTO(
        int codeLength,
        int periodSeconds,
        int maxAttempts
) {
}
