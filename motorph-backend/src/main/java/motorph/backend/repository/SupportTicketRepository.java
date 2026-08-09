package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.SupportTicket;
import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.domain.value.SupportTicketStatus;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for support ticket records.
 */
@Repository
public class SupportTicketRepository {

    private final DataSource dataSource;

    public SupportTicketRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(SupportTicket ticket) {
        String sql = """
                INSERT INTO support_ticket (
                    employee_id,
                    category,
                    routing_team,
                    subject,
                    description,
                    status,
                    resolved_by_user_account_id,
                    resolved_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            bindTicketInsert(stmt, ticket);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save support ticket.", e);
        }
    }

    public SupportTicket findById(int ticketId) {
        String sql = baseSelect() + " WHERE ticket_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find support ticket by id.", e);
        }

        return null;
    }

    public List<SupportTicket> findByEmployeeId(int employeeId) {
        String sql = baseSelect() + """
                WHERE employee_id = ?
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, employeeId));
    }

    public List<SupportTicket> findByRoutingTeam(RoutingTeam routingTeam) {
        String sql = baseSelect() + """
                WHERE routing_team = ?
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setString(1, routingTeam.name()));
    }

    public List<SupportTicket> findAll() {
        String sql = baseSelect() + " ORDER BY created_at DESC";
        return executeListQuery(sql, null);
    }

    public List<SupportTicket> findOpenTickets() {
        String sql = baseSelect() + """
                WHERE status = 'OPEN'
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, null);
    }

    public void markResolved(int ticketId, int resolvedByUserAccountId) {
        String sql = """
                UPDATE support_ticket
                SET status = 'RESOLVED',
                    resolved_by_user_account_id = ?,
                    resolved_at = NOW()
                WHERE ticket_id = ?
                  AND status = 'OPEN'
                """;

        executeUpdate(sql, stmt -> {
            stmt.setInt(1, resolvedByUserAccountId);
            stmt.setInt(2, ticketId);
        });
    }

    public void markCancelled(int ticketId) {
        String sql = """
                UPDATE support_ticket
                SET status = 'CANCELLED'
                WHERE ticket_id = ?
                  AND status = 'OPEN'
                """;

        executeUpdate(sql, stmt -> stmt.setInt(1, ticketId));
    }

    private String baseSelect() {
        return """
                SELECT ticket_id,
                       employee_id,
                       category,
                       routing_team,
                       subject,
                       description,
                       status,
                       resolved_by_user_account_id,
                       resolved_at,
                       created_at,
                       updated_at
                FROM support_ticket
                """;
    }

    private void bindTicketInsert(PreparedStatement stmt, SupportTicket ticket) throws SQLException {
        stmt.setInt(1, ticket.getEmployeeId());
        stmt.setString(2, ticket.getCategory().name());
        stmt.setString(3, ticket.getRoutingTeam().name());
        stmt.setString(4, ticket.getSubject());
        stmt.setString(5, ticket.getDescription());
        stmt.setString(6, ticket.getStatus().name());
        setNullableInteger(stmt, 7, ticket.getResolvedByUserAccountId());
        setNullableTimestamp(stmt, 8, ticket.getResolvedAt());
        stmt.setTimestamp(9, Timestamp.valueOf(ticket.getCreatedAt()));
        setNullableTimestamp(stmt, 10, ticket.getUpdatedAt());
    }

    private SupportTicket mapRow(ResultSet rs) throws SQLException {
        return new SupportTicket(
                rs.getInt("ticket_id"),
                rs.getInt("employee_id"),
                SupportTicketCategory.fromValue(rs.getString("category")),
                RoutingTeam.fromValue(rs.getString("routing_team")),
                rs.getString("subject"),
                rs.getString("description"),
                SupportTicketStatus.fromValue(rs.getString("status")),
                getNullableInteger(rs, "resolved_by_user_account_id"),
                getNullableDateTime(rs, "resolved_at"),
                getNullableDateTime(rs, "created_at"),
                getNullableDateTime(rs, "updated_at")
        );
    }

    private List<SupportTicket> executeListQuery(String sql, StatementBinder binder) {
        List<SupportTicket> tickets = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query support tickets.", e);
        }

        return tickets;
    }

    private void executeUpdate(String sql, StatementBinder binder) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            binder.bind(stmt);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Open support ticket not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update support ticket.", e);
        }
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    private void setNullableTimestamp(PreparedStatement stmt, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime getNullableDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toLocalDateTime();
    }

    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}
