package motorph.api;

import motorph.api.dto.payroll.PayslipSummaryDTO;

import java.util.List;

/**
 * Thin wrapper mirroring the backend's /api/v1/payslips endpoints.
 */
public final class PayslipApiClient {

    private final HttpApiClient httpApiClient;

    public PayslipApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    // the caller's own payroll history
    public List<PayslipSummaryDTO> getOwnPayslips() {
        return httpApiClient.getList("/api/v1/payslips", PayslipSummaryDTO.class);
    }

    // the rendered payslip PDF as bytes - the backend decides self-view vs.
    // VIEW_PAYSLIP-permitted access server-side
    public byte[] getPayslipPdf(int employeeId, int payrunId) {
        return httpApiClient.getBytes("/api/v1/payslips/" + employeeId + "/" + payrunId + "/pdf");
    }
}
