package motorph.backend.service.report;

import motorph.backend.dto.report.ticket.TicketReportDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.reporting.JasperReportGenerator;
import motorph.backend.repository.report.TicketReportRepository;

import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketReportService {

    private static final String TICKET_REPORT = "/motorph/reports/ticket/ticket_report.jrxml";

    private final TicketReportRepository ticketReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    public TicketReportService(TicketReportRepository ticketReportRepository, JasperReportGenerator jasperReportGenerator) {
        this.ticketReportRepository = ticketReportRepository;
        this.jasperReportGenerator = jasperReportGenerator;
    }

    public List<TicketReportDTO> getTicketReport(Integer departmentId, Integer employeeId, String category,
                                                 String routingTeam, String status, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return ticketReportRepository.findTicketReport(departmentId, employeeId, category, routingTeam, status, startDate, endDate);
    }

    public byte[] generateTicketReportPdf(Integer departmentId, Integer employeeId, String category,
                                          String routingTeam, String status, LocalDate startDate, LocalDate endDate) {
        List<TicketReportDTO> data = getTicketReport(departmentId, employeeId, category, routingTeam, status, startDate, endDate);

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                TICKET_REPORT,
                buildReportParameters(startDate, endDate),
                data
        );

        return jasperReportGenerator.exportToPdfBytes(jasperPrint);
    }

    private Map<String, Object> buildReportParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Ticket Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);
        return parameters;
    }

    // dates are optional here, but must be paired if either is present
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new ValidationException("Both start and end date are required together.");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }
}
