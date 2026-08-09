package motorph.api.dto.employee;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.api.dto.auth.CredentialsPreview;

/** Wire response body for POST /api/v1/employees. */
public record EmployeeCreateResponse(
        EmployeeProfileDTO employee,
        CredentialsPreview credentialsPreview
) {

    @JsonCreator
    public EmployeeCreateResponse(@JsonProperty("employee") EmployeeProfileDTO employee,
                                  @JsonProperty("credentialsPreview") CredentialsPreview credentialsPreview) {
        this.employee = employee;
        this.credentialsPreview = credentialsPreview;
    }
}
