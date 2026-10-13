package motorph.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for {@code POST /api/v1/auth/refresh}. */
public record RefreshRequest(
        @NotBlank @Size(max = 4096) String refreshToken
) {
}
