package motorph.application.service.report;

import motorph.application.dto.report.ticket.TicketReportDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.repository.report.TicketReportRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;

import net.sf.jasperreports.engine.JasperPrint;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles ticket report generation workflows.
 */
public class TicketReportService {

    private static final String TICKET_REPORT =
            "/motorph/reports/ticket/ticket_report.jrxml";

    private final TicketReportRepository ticketReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    // constructors
    public TicketReportService(TicketReportRepository ticketReportRepository,
                               JasperReportGenerator jasperReportGenerator) {
        this.ticketReportRepository = require(ticketReportRepository, "TicketReportRepository is required.");
        this.jasperReportGenerator = require(jasperReportGenerator, "JasperReportGenerator is required.");
    }

    // returns ticket report data
    public List<TicketReportDTO> getTicketReport(Integer departmentId,
                                                 Integer employeeId,
                                                 String category,
                                                 String routingTeam,
                                                 String status,
                                                 LocalDate startDate,
                                                 LocalDate endDate) {
        validateDateRange(startDate, endDate);

        return ticketReportRepository.findTicketReport(
                departmentId,
                employeeId,
                category,
                routingTeam,
                status,
                startDate,
                endDate
        );
    }

    // previews the ticket report
    public void previewTicketReport(Integer departmentId,
                                    Integer employeeId,
                                    String category,
                                    String routingTeam,
                                    String status,
                                    LocalDate startDate,
                                    LocalDate endDate) {

        List<TicketReportDTO> reportData = getTicketReport(
                departmentId,
                employeeId,
                category,
                routingTeam,
                status,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                TICKET_REPORT,
                buildReportParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // exports the ticket report to PDF
    public void exportTicketReportToPdf(Integer departmentId,
                                        Integer employeeId,
                                        String category,
                                        String routingTeam,
                                        String status,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        String outputPath) {

        if (outputPath == null || outputPath.isBlank()) {
            throw new ValidationException("Output path is required.");
        }

        List<TicketReportDTO> reportData = getTicketReport(
                departmentId,
                employeeId,
                category,
                routingTeam,
                status,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                TICKET_REPORT,
                buildReportParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // builds report parameters for JasperReports
    private Map<String, Object> buildReportParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("REPORT_TITLE", "Ticket Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);

        return parameters;
    }

    // validates date range input
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            throw new ValidationException("Both start date and end date are required when filtering by date.");
        }

        if (startDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    // validates required dependency
    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}