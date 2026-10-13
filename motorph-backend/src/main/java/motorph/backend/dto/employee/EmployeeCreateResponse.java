package motorph.backend.dto.employee;

import motorph.backend.dto.auth.CredentialsPreview;

/**
 * Response body for {@code POST /api/v1/employees}. {@code credentialsPreview} is only set while
 * preview mode is enabled and is {@code null} otherwise.
 */
public record EmployeeCreateResponse(
        EmployeeProfileDTO employee,
        CredentialsPreview credentialsPreview
) {
}
