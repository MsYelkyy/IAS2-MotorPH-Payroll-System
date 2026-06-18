package motorph.app;

import javafx.application.Application;
import javafx.stage.Stage;
import motorph.application.service.report.AttendanceReportService;
import motorph.application.service.report.PayrollReportService;
import motorph.infrastructure.repository.report.AttendanceReportRepository;
import motorph.infrastructure.repository.report.PayrollReportRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;

import java.time.LocalDate;

/**
 * Main entry point used temporarily for testing organization payroll report generation.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        PayrollReportService payrollReportService = new PayrollReportService(
                new PayrollReportRepository(),
                new JasperReportGenerator()
        );
        AttendanceReportService attendanceReportService = new AttendanceReportService(
                new AttendanceReportRepository(),
                new JasperReportGenerator()
        );

        payrollReportService.previewOrganizationPayrollReport(
                null,
                null,
                null,
                LocalDate.of(2024, 6, 3),
                LocalDate.of(2024, 12, 31)
        );



        payrollReportService.previewPayrollSummaryReport(
                null,
                null,
                LocalDate.of(2024, 6, 3),
                LocalDate.of(2024, 12, 31)
        );



        payrollReportService.previewDepartmentPayrollReport(
                2,
                null,
                LocalDate.of(2024, 6, 3),
                LocalDate.of(2024, 12, 31)
        );

        payrollReportService.previewDepartmentPayrollReport(
                4,
                null,
                LocalDate.of(2024, 6, 3),
                LocalDate.of(2024, 12, 31)
        );

        attendanceReportService.previewEmployeeTimecardReport(
                10001,
                LocalDate.of(2024, 12, 1),
                LocalDate.of(2024, 12, 31)
        );



        payrollReportService.previewPayslipReport(10001, 11);
        payrollReportService.previewPayslipReport(10001, 12);
    }

    public static void main(String[] args) {
        launch(args);
    }
}