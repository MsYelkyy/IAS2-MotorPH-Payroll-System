package motorph.ui.factory;

import motorph.application.service.report.AttendanceReportService;
import motorph.application.service.report.AuditReportService;
import motorph.application.service.report.PayrollReportService;
import motorph.application.service.report.TicketReportService;
import motorph.infrastructure.reporting.JasperReportGenerator;
import motorph.infrastructure.repository.report.AttendanceReportRepository;
import motorph.infrastructure.repository.report.AuditReportRepository;
import motorph.infrastructure.repository.report.PayrollReportRepository;
import motorph.infrastructure.repository.report.TicketReportRepository;

/** Owns Jasper-backed report services. */
class ReportServiceGroup {

    private final AttendanceReportService attendanceReportService;
    private final AuditReportService auditReportService;
    private final PayrollReportService payrollReportService;
    private final TicketReportService ticketReportService;

    // constructors
    ReportServiceGroup() {
        JasperReportGenerator jasper = new JasperReportGenerator();
        this.attendanceReportService = new AttendanceReportService(new AttendanceReportRepository(), jasper);
        this.auditReportService      = new AuditReportService(new AuditReportRepository(), jasper);
        this.payrollReportService    = new PayrollReportService(new PayrollReportRepository(), jasper);
        this.ticketReportService     = new TicketReportService(new TicketReportRepository(), jasper);
    }

    // getters
    AttendanceReportService getAttendanceReportService() {
        return attendanceReportService;
    }

    AuditReportService getAuditReportService() {
        return auditReportService;
    }

    PayrollReportService getPayrollReportService() {
        return payrollReportService;
    }

    TicketReportService getTicketReportService() {
        return ticketReportService;
    }
}
