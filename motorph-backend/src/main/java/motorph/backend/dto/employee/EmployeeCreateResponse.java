package motorph.backend.dto.employee;

import motorph.backend.dto.auth.CredentialsPreview;

/**
 * Wire response body for POST /api/v1/employees. credentialsPreview is
 * populated only in dev-preview mode (see EmployeeCredentialsDeliveryService)
 * - in a real deployment the temporary password is emailed, never returned.
 */
public record EmployeeCreateResponse(
        EmployeeProfileDTO employee,
        CredentialsPreview credentialsPreview
) {
}
