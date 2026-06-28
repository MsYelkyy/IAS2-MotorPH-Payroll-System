package motorph.application.service.report;


import motorph.application.dto.report.payroll.OrganizationPayrollReportDTO;
import motorph.application.dto.report.payroll.PayrollSummaryReportDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.reporting.JasperReportGenerator;
import motorph.infrastructure.repository.report.PayrollReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests payroll report generation using mocked dependencies.
 */
@DisplayName("PayrollReportService Tests")
class PayrollReportServiceTest {


    // generates an organization payroll report for a valid date range
    @Test
    @DisplayName("Positive - getOrganizationPayrollReport should return report rows")
    void getOrganizationPayrollReportShouldReturnRows() {
        PayrollReportRepository repository = mock(PayrollReportRepository.class);
        when(repository.findOrganizationPayrollReport(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mock(OrganizationPayrollReportDTO.class)));

        PayrollReportService service = new PayrollReportService(
                repository, mock(JasperReportGenerator.class));

        List<OrganizationPayrollReportDTO> rows = service.getOrganizationPayrollReport(
                null, null, null, null,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));

        assertEquals(1, rows.size());
    }


    // generates a payroll summary report for all departments
    @Test
    @DisplayName("Positive - getPayrollSummaryReport should return a summary for all employees")
    void getPayrollSummaryReportShouldReturnSummary() {
        PayrollReportRepository repository = mock(PayrollReportRepository.class);
        when(repository.findPayrollSummaryReport(any(), any(), any(), any()))
                .thenReturn(List.of(
                        mock(PayrollSummaryReportDTO.class),
                        mock(PayrollSummaryReportDTO.class)));

        PayrollReportService service = new PayrollReportService(
                repository, mock(JasperReportGenerator.class));

        List<PayrollSummaryReportDTO> summary = service.getPayrollSummaryReport(
                null, null,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30));

        assertEquals(2, summary.size());
    }


    // returns an empty report when there is no data, without failing
    @Test
    @DisplayName("Edge Case - report should return empty data without error")
    void getPayrollSummaryReportShouldReturnEmptyWithoutError() {
        PayrollReportRepository repository = mock(PayrollReportRepository.class);
        when(repository.findPayrollSummaryReport(any(), any(), any(), any()))
                .thenReturn(List.of());

        PayrollReportService service = new PayrollReportService(
                repository, mock(JasperReportGenerator.class));

        List<PayrollSummaryReportDTO> summary = service.getPayrollSummaryReport(
                null, null, null, null);

        assertNotNull(summary);
        assertTrue(summary.isEmpty());
    }


    // rejects a payslip report when the data cannot be found
    @Test
    @DisplayName("Negative - getPayslipReport should reject missing payslip data")
    void getPayslipReportShouldRejectMissingData() {
        PayrollReportRepository repository = mock(PayrollReportRepository.class);
        when(repository.findPayslipReport(10001, 1)).thenReturn(null);

        PayrollReportService service = new PayrollReportService(
                repository, mock(JasperReportGenerator.class));

        assertThrows(
                ValidationException.class,
                () -> service.getPayslipReport(10001, 1)
        );
    }


    // rejects an invalid date range
    @Test
    @DisplayName("Negative - report should reject an invalid date range")
    void reportShouldRejectInvalidDateRange() {
        PayrollReportService service = new PayrollReportService(
                mock(PayrollReportRepository.class), mock(JasperReportGenerator.class));

        assertThrows(
                ValidationException.class,
                () -> service.getPayrollSummaryReport(
                        null, null,
                        LocalDate.of(2025, 6, 30), LocalDate.of(2025, 6, 1))
        );
    }
}
