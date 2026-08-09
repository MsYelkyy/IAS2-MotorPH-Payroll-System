package motorph.backend.repository.report;

import motorph.backend.exception.DataAccessException;
import motorph.backend.dto.report.ticket.TicketReportDTO;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TicketReportRepository {

    private final DataSource dataSource;

    public TicketReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<TicketReportDTO> findTicketReport(Integer departmentId, Integer employeeId, String category,
                                                  String routingTeam, String status, LocalDate startDate,
                                                  LocalDate endDate) {
        String sql = """
                SELECT
                   st.ticket_id,
                   st.employee_id,
                   CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                   d.department_name,
                   st.category,
                   st.routing_team,
                   st.subject,
                   st.status,
                   CONCAT(resolver.first_name, ' ', resolver.last_name) AS resolved_by,
                   st.resolved_at,
                   st.created_at
                FROM support_ticket st
                JOIN employee e ON st.employee_id = e.employee_id
                JOIN department d ON e.department_id = d.department_id
                LEFT JOIN user_account ua ON st.resolved_by_user_account_id = ua.user_account_id
                LEFT JOIN employee resolver ON ua.employee_id = resolver.employee_id
                WHERE (? IS NULL OR e.department_id = ?)
                 AND (? IS NULL OR st.employee_id = ?)
                 AND (? IS NULL OR st.category = ?)
                 AND (? IS NULL OR st.routing_team = ?)
                 AND (? IS NULL OR st.status = ?)
                 AND (? IS NULL OR DATE(st.created_at) >= ?)
                 AND (? IS NULL OR DATE(st.created_at) <= ?)
                ORDER BY st.created_at DESC
                """;

        List<TicketReportDTO> rows = new ArrayList<>();
        String cleanCategory = clean(category);
        String cleanRoutingTeam = clean(routingTeam);
        String cleanStatus = clean(status);
        Date sqlStart = toSqlDate(startDate);
        Date sqlEnd = toSqlDate(endDate);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setNullableInt(stmt, 1, departmentId);
            setNullableInt(stmt, 2, departmentId);
            setNullableInt(stmt, 3, employeeId);
            setNullableInt(stmt, 4, employeeId);
            stmt.setString(5, cleanCategory);
            stmt.setString(6, cleanCategory);
            stmt.setString(7, cleanRoutingTeam);
            stmt.setString(8, cleanRoutingTeam);
            stmt.setString(9, cleanStatus);
            stmt.setString(10, cleanStatus);
            setNullableDate(stmt, 11, sqlStart);
            setNullableDate(stmt, 12, sqlStart);
            setNullableDate(stmt, 13, sqlEnd);
            setNullableDate(stmt, 14, sqlEnd);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapToDTO(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find ticket report.", e);
        }

        return rows;
    }

    private TicketReportDTO mapToDTO(ResultSet rs) throws SQLException {
        Timestamp resolvedAtValue = rs.getTimestamp("resolved_at");

        return new TicketReportDTO(
                rs.getInt("ticket_id"),
                rs.getInt("employee_id"),
                rs.getString("employee_name"),
                rs.getString("department_name"),
                rs.getString("category"),
                rs.getString("routing_team"),
                rs.getString("subject"),
                rs.getString("status"),
                rs.getString("resolved_by"),
                resolvedAtValue == null ? null : resolvedAtValue.toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime()
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
