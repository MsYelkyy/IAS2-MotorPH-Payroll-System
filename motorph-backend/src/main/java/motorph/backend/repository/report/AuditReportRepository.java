package motorph.backend.repository.report;

import motorph.backend.exception.DataAccessException;
import motorph.backend.dto.report.audit.AuditReportDTO;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditReportRepository {

    private final DataSource dataSource;

    public AuditReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<AuditReportDTO> findAuditReport(Integer userAccountId, String action, LocalDate startDate, LocalDate endDate) {
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
                LEFT JOIN user_account ua ON ual.user_account_id = ua.user_account_id
                LEFT JOIN employee e ON ua.employee_id = e.employee_id
                WHERE (? IS NULL OR ual.user_account_id = ?)
                 AND (? IS NULL OR ual.action = ?)
                 AND (? IS NULL OR DATE(ual.logged_at) >= ?)
                 AND (? IS NULL OR DATE(ual.logged_at) <= ?)
                ORDER BY ual.logged_at DESC
                """;

        List<AuditReportDTO> rows = new ArrayList<>();
        String cleanAction = clean(action);
        Date sqlStart = toSqlDate(startDate);
        Date sqlEnd = toSqlDate(endDate);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableInt(stmt, 1, userAccountId);
            setNullableInt(stmt, 2, userAccountId);
            stmt.setString(3, cleanAction);
            stmt.setString(4, cleanAction);
            setNullableDate(stmt, 5, sqlStart);
            setNullableDate(stmt, 6, sqlStart);
            setNullableDate(stmt, 7, sqlEnd);
            setNullableDate(stmt, 8, sqlEnd);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapToDTO(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find audit report.", e);
        }

        return rows;
    }

    private AuditReportDTO mapToDTO(ResultSet rs) throws SQLException {
        Integer userAccountId = rs.getObject("user_account_id", Integer.class);

        return new AuditReportDTO(
                rs.getInt("log_id"),
                userAccountId,
                rs.getString("company_email"),
                rs.getString("employee_name"),
                rs.getString("action"),
                rs.getString("details"),
                rs.getString("ip_address"),
                rs.getString("device_info"),
                rs.getTimestamp("logged_at").toLocalDateTime()
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
        } else {
            stmt.setInt(index, value);
        }
    }

    private void setNullableDate(PreparedStatement stmt, int index, Date value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.DATE);
        } else {
            stmt.setDate(index, value);
        }
    }
}
