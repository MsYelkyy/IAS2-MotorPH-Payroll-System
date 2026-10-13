package motorph.backend.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/support-tickets}. The employee is always the caller, so no
 * employee ID is accepted.
 *
 * <p>The length limits match the {@code support_ticket} columns. Text is stored exactly as sent
 * and encoded when it is displayed, so markup characters are allowed; only control characters are
 * refused, apart from tabs and, in the description, line breaks.
 */
public record TicketSubmitRequest(
        @NotBlank @Size(max = 30) String category,
        @NotBlank @Size(max = 150) @Pattern(regexp = "[\\t\\P{Cc}]*") String subject,
        @NotBlank @Size(max = 1000) @Pattern(regexp = "[\\t\\r\\n\\P{Cc}]*") String description
) {
}
