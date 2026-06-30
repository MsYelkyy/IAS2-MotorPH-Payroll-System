package motorph.infrastructure.repository.report;

import motorph.application.exception.DataAccessException;

import motorph.application.dto.report.audit.AuditReportDTO;
import motorph.infrastructure.config.DatabaseConnection;


import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 * Retrieves audit report data from the database.
 */
public class AuditReportRepository {


    // finds audit logs using optional filters
    public List<AuditReportDTO> findAuditReport(Integer userAccountId,
                                                String action,
                                                LocalDate startDate,
                                                LocalDate endDate) {


        String sql = """
               SELECT
                  ual.log_id,
                  ual.user_account_id,
                  ua.company_email,
                  CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                  ual.action,
                  ual.details,
                  ual.ip_address,
                  ual.device_info,
                  ual.logged_at
               FROM user_access_logs ual
               LEFT JOIN user_account ua
                  ON ual.user_account_id = ua.user_account_id
               LEFT JOIN employee e
                  ON ua.employee_id = e.employee_id
               WHERE (? IS NULL OR ual.user_account_id = ?)
                AND (? IS NULL OR ual.action = ?)
                AND (? IS NULL OR DATE(ual.logged_at) >= ?)
                AND (? IS NULL OR DATE(ual.logged_at) <= ?)
               ORDER BY ual.logged_at DESC
               """;


        List<AuditReportDTO> auditLogs = new ArrayList<>();


        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {


            statement.setObject(1, userAccountId);
            statement.setObject(2, userAccountId);


            statement.setString(3, clean(action));
            statement.setString(4, clean(action));


            statement.setDate(5, toSqlDate(startDate));
            statement.setDate(6, toSqlDate(startDate));


            statement.setDate(7, toSqlDate(endDate));
            statement.setDate(8, toSqlDate(endDate));


            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auditLogs.add(mapToDTO(resultSet));
                }
            }


        } catch (Exception e) {
            throw new DataAccessException("Failed to retrieve audit report data.", e);
        }


        return auditLogs;
    }


    // maps one result row to DTO
    private AuditReportDTO mapToDTO(ResultSet resultSet) throws Exception {
        return new AuditReportDTO(
                resultSet.getInt("log_id"),
                resultSet.getObject("user_account_id", Integer.class),
                resultSet.getString("company_email"),
                resultSet.getString("employee_name"),
                resultSet.getString("action"),
                resultSet.getString("details"),
                resultSet.getString("ip_address"),
                resultSet.getString("device_info"),
                resultSet.getTimestamp("logged_at").toLocalDateTime()
        );
    }


    // converts local date to sql date
    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }


    // cleans optional text filters
    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

