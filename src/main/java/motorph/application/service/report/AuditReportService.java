package motorph.application.service.report;


import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import motorph.application.dto.report.audit.AuditReportDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.reporting.JasperReportGenerator;
import motorph.infrastructure.repository.report.AuditReportRepository;
import net.sf.jasperreports.engine.JasperPrint;


/**
* Handles audit report generation workflows.
*/
public class AuditReportService {


   private static final String AUDIT_REPORT =
           "/motorph/reports/audit/audit_report.jrxml";


   private final AuditReportRepository auditReportRepository;
   private final JasperReportGenerator jasperReportGenerator;


   // constructors
   public AuditReportService(AuditReportRepository auditReportRepository,
                             JasperReportGenerator jasperReportGenerator) {
       this.auditReportRepository = require(auditReportRepository, "AuditReportRepository is required.");
       this.jasperReportGenerator = require(jasperReportGenerator, "JasperReportGenerator is required.");
   }


   // returns audit report data
   public List<AuditReportDTO> getAuditReport(Integer userAccountId,
                                              String action,
                                              LocalDate startDate,
                                              LocalDate endDate) {
       validateDateRange(startDate, endDate);


       return auditReportRepository.findAuditReport(
               userAccountId,
               action,
               startDate,
               endDate
       );
   }


   // previews the audit report
   public void previewAuditReport(Integer userAccountId,
                                  String action,
                                  LocalDate startDate,
                                  LocalDate endDate) {


       List<AuditReportDTO> reportData = getAuditReport(
               userAccountId,
               action,
               startDate,
               endDate
       );


       JasperPrint jasperPrint = jasperReportGenerator.generateReport(
               AUDIT_REPORT,
               buildReportParameters(startDate, endDate),
               reportData
       );


       jasperReportGenerator.showPreview(jasperPrint);
   }


   // exports the audit report to PDF
   public void exportAuditReportToPdf(Integer userAccountId,
                                      String action,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      String outputPath) {


       if (outputPath == null || outputPath.isBlank()) {
           throw new ValidationException("Output path is required.");
       }


       List<AuditReportDTO> reportData = getAuditReport(
               userAccountId,
               action,
               startDate,
               endDate
       );


       JasperPrint jasperPrint = jasperReportGenerator.generateReport(
               AUDIT_REPORT,
               buildReportParameters(startDate, endDate),
               reportData
       );


       jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
   }


   // builds report parameters for JasperReports
   private Map<String, Object> buildReportParameters(LocalDate startDate, LocalDate endDate) {
       Map<String, Object> parameters = new HashMap<>();


       parameters.put("REPORT_TITLE", "Audit Trail Report");
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

