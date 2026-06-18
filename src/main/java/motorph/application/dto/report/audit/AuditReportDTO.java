package motorph.application.dto.report.audit;


import java.time.LocalDateTime;


/**
 * Stores audit report row data.
 */
public class AuditReportDTO {


    private final int logId;
    private final Integer userAccountId;
    private final String companyEmail;
    private final String employeeName;
    private final String action;
    private final String details;
    private final String ipAddress;
    private final String deviceInfo;
    private final LocalDateTime loggedAt;


    // constructors
    public AuditReportDTO(int logId,
                          Integer userAccountId,
                          String companyEmail,
                          String employeeName,
                          String action,
                          String details,
                          String ipAddress,
                          String deviceInfo,
                          LocalDateTime loggedAt) {
        this.logId = logId;
        this.userAccountId = userAccountId;
        this.companyEmail = companyEmail;
        this.employeeName = employeeName;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.loggedAt = loggedAt;
    }


    // getters
    public int getLogId() { return logId; }


    public Integer getUserAccountId() { return userAccountId; }


    public String getCompanyEmail() { return companyEmail; }


    public String getEmployeeName() { return employeeName; }


    public String getAction() { return action; }


    public String getDetails() { return details; }


    public String getIpAddress() { return ipAddress; }


    public String getDeviceInfo() { return deviceInfo; }


    public LocalDateTime getLoggedAt() { return loggedAt; }
}

