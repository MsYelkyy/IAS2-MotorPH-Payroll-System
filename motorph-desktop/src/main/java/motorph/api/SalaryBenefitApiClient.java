package motorph.api;

import motorph.api.dto.salary.EmployeeBenefitDTO;
import motorph.api.dto.salary.SalaryDatesUpdateRequest;
import motorph.api.dto.salary.SalaryHistoryDTO;
import motorph.api.dto.salary.SalaryRecordRequest;

import java.util.List;

/** Thin wrapper around the backend's salary-history/benefits endpoints. */
public final class SalaryBenefitApiClient {

    private final HttpApiClient httpApiClient;

    public SalaryBenefitApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<SalaryHistoryDTO> getSalaryHistory(int employeeId) {
        return httpApiClient.getList("/api/v1/employees/" + employeeId + "/salary-history", SalaryHistoryDTO.class);
    }

    public SalaryHistoryDTO getCurrentSalary(int employeeId) {
        return httpApiClient.get("/api/v1/employees/" + employeeId + "/salary-current", SalaryHistoryDTO.class);
    }

    public void addSalaryRecord(int employeeId, SalaryRecordRequest request) {
        httpApiClient.postNoContent("/api/v1/employees/" + employeeId + "/salary-history", request);
    }

    public void updateSalaryDates(int salaryHistoryId, SalaryDatesUpdateRequest request) {
        httpApiClient.put("/api/v1/salary-history/" + salaryHistoryId, request, Void.class);
    }

    public List<EmployeeBenefitDTO> getAllBenefits(int employeeId) {
        return httpApiClient.getList("/api/v1/employees/" + employeeId + "/benefits", EmployeeBenefitDTO.class);
    }

    public void endBenefit(int employeeBenefitId) {
        httpApiClient.postNoContent("/api/v1/employee-benefits/" + employeeBenefitId + "/end", null);
    }
}
