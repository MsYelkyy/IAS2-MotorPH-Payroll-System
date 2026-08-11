package motorph.api;

import motorph.api.dto.payroll.CompletedPayrunSummaryDTO;
import motorph.api.dto.payroll.PayrollPreviewDTO;
import motorph.api.dto.payroll.PayrollSummaryDTO;
import motorph.api.dto.payroll.PayrunCreateRequest;
import motorph.api.dto.payroll.PayrunDTO;
import motorph.api.dto.payroll.PayrunReadinessDTO;
import motorph.api.dto.payroll.PendingPayrunSummaryDTO;

import java.util.List;

/** Thin wrapper around the backend's /api/v1/payruns endpoints. */
public final class PayrunApiClient {

    private final HttpApiClient httpApiClient;

    public PayrunApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<PayrunDTO> getAllPayruns() {
        return httpApiClient.getList("/api/v1/payruns", PayrunDTO.class);
    }

    public PayrunDTO getPayrun(int payrunId) {
        return httpApiClient.get("/api/v1/payruns/" + payrunId, PayrunDTO.class);
    }

    public List<PayrunDTO> getPayrunsWithPayrollData() {
        return httpApiClient.getList("/api/v1/payruns/with-results", PayrunDTO.class);
    }

    public List<PendingPayrunSummaryDTO> getPendingPayruns() {
        return httpApiClient.getList("/api/v1/payruns/pending", PendingPayrunSummaryDTO.class);
    }

    public List<CompletedPayrunSummaryDTO> getCompletedPayruns() {
        return httpApiClient.getList("/api/v1/payruns/completed", CompletedPayrunSummaryDTO.class);
    }

    public List<PayrunReadinessDTO> getPayrunReadiness(int payrunId) {
        return httpApiClient.getList("/api/v1/payruns/" + payrunId + "/readiness", PayrunReadinessDTO.class);
    }

    public List<PayrollSummaryDTO> getPayrollSummaries(int payrunId) {
        return httpApiClient.getList("/api/v1/payruns/" + payrunId + "/summaries", PayrollSummaryDTO.class);
    }

    public PayrollPreviewDTO previewPayrollForEmployee(int payrunId, int employeeId) {
        return httpApiClient.get(
                "/api/v1/payruns/" + payrunId + "/preview/" + employeeId, PayrollPreviewDTO.class);
    }

    public PayrunDTO createPayrun(PayrunCreateRequest request) {
        return httpApiClient.post("/api/v1/payruns", request, PayrunDTO.class);
    }

    public int generatePayroll(int payrunId) {
        return httpApiClient.post("/api/v1/payruns/" + payrunId + "/generate", null, Integer.class);
    }
}
