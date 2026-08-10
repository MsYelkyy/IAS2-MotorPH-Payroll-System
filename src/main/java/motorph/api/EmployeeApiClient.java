package motorph.api;

import motorph.api.dto.employee.ContactInfoUpdateRequest;
import motorph.api.dto.employee.DepartmentDTO;
import motorph.api.dto.employee.EmployeeCreateRequest;
import motorph.api.dto.employee.EmployeeCreateResponse;
import motorph.api.dto.employee.EmployeeProfileDTO;
import motorph.api.dto.employee.EmployeeSummaryDTO;
import motorph.api.dto.employee.EmployeeUpdateRequest;
import motorph.api.dto.employee.GovernmentDetailsDTO;
import motorph.api.dto.employee.PositionDTO;

import java.util.List;

/**
 * Thin wrapper mirroring the backend's /api/v1/employees, /api/v1/departments,
 * and /api/v1/positions endpoints. Method shapes mirror the old desktop
 * EmployeeService so call sites change minimally.
 */
public final class EmployeeApiClient {

    private final HttpApiClient httpApiClient;

    public EmployeeApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<EmployeeSummaryDTO> getAllEmployeeSummaries() {
        return httpApiClient.getList("/api/v1/employees", EmployeeSummaryDTO.class);
    }

    // the backend decides admin-view vs. self-view server-side based on the
    // caller's permissions - the desktop no longer needs to pick a variant
    public EmployeeProfileDTO getEmployee(int employeeId) {
        return httpApiClient.get("/api/v1/employees/" + employeeId, EmployeeProfileDTO.class);
    }

    public GovernmentDetailsDTO getGovernmentDetails(int employeeId) {
        return httpApiClient.get("/api/v1/employees/" + employeeId + "/government-details", GovernmentDetailsDTO.class);
    }

    public List<DepartmentDTO> getAllDepartments() {
        return httpApiClient.getList("/api/v1/departments", DepartmentDTO.class);
    }

    public List<PositionDTO> getAllPositions() {
        return httpApiClient.getList("/api/v1/positions", PositionDTO.class);
    }

    public List<PositionDTO> getPositionsByDepartment(int departmentId) {
        return httpApiClient.getList("/api/v1/positions?departmentId=" + departmentId, PositionDTO.class);
    }

    public EmployeeCreateResponse createEmployee(EmployeeCreateRequest request) {
        return httpApiClient.post("/api/v1/employees", request, EmployeeCreateResponse.class);
    }

    public EmployeeProfileDTO updateEmployee(int employeeId, EmployeeUpdateRequest request) {
        return httpApiClient.put("/api/v1/employees/" + employeeId, request, EmployeeProfileDTO.class);
    }

    // self-service: the backend applies these to the caller's own record
    public void updateOwnContactInfo(String phoneNumber, String address) {
        httpApiClient.put("/api/v1/employees/me/contact",
                new ContactInfoUpdateRequest(phoneNumber, address), Void.class);
    }

    public void uploadOwnPhoto(byte[] content, String extension) {
        httpApiClient.postBytes("/api/v1/employees/me/photo?extension=" + extension, content);
    }

    /** Raw image bytes; throws ApiException with NOT_FOUND when there is no photo on file. */
    public byte[] getEmployeePhoto(int employeeId) {
        return httpApiClient.getBytes("/api/v1/employees/" + employeeId + "/photo");
    }

    public void deactivateEmployee(int employeeId) {
        httpApiClient.delete("/api/v1/employees/" + employeeId);
    }
}
