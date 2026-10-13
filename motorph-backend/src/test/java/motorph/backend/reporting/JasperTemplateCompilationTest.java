package motorph.backend.reporting;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JasperReports reads .jrxml templates with the Jackson XML library. We only turned off Spring's
 * XML converter, not that library, so all the report templates should still compile.
 */
class JasperTemplateCompilationTest {

    @ParameterizedTest(name = "{0}")
    @DisplayName("Each shipped report template still compiles")
    @ValueSource(strings = {
            "/motorph/reports/attendance/attendance_report.jrxml",
            "/motorph/reports/attendance/employee_timecard_report.jrxml",
            "/motorph/reports/audit/audit_report.jrxml",
            "/motorph/reports/payroll/organization_payroll_report.jrxml",
            "/motorph/reports/payroll/payroll_summary_report.jrxml",
            "/motorph/reports/payroll/payslip_report.jrxml",
            "/motorph/reports/ticket/ticket_report.jrxml"
    })
    void templateCompiles(String resource) throws Exception {
        try (InputStream template = getClass().getResourceAsStream(resource)) {
            assertThat(template).as("template %s on the classpath", resource).isNotNull();

            JasperReport report = JasperCompileManager.compileReport(template);

            assertThat(report.getName()).isNotBlank();
        }
    }
}
