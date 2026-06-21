package motorph.infrastructure.repository;

import motorph.domain.model.SupportTicket;
import motorph.domain.value.RoutingTeam;
import motorph.domain.value.SupportTicketCategory;
import motorph.domain.value.SupportTicketStatus;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for support ticket records.
 */
public class SupportTicketRepository implements Repository<SupportTicket, Integer> {

    // saves a new support ticket
    @Override
    public void save(SupportTicket ticket) {
        validateTicket(ticket);

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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            bindTicketInsert(stmt, ticket);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save support ticket.", e);
        }
    }

    // finds a support ticket by id
    @Override
    public SupportTicket findById(Integer ticketId) {
        validatePositive(ticketId, "Ticket ID must be positive.");

        String sql = baseSelect() + """
                WHERE ticket_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ticketId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find support ticket by id.", e);
        }

        return null;
    }

    // finds support tickets by employee id
    public List<SupportTicket> findByEmployeeId(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = baseSelect() + """
                WHERE employee_id = ?
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setInt(1, employeeId));
    }

    // finds support tickets by routing team
    public List<SupportTicket> findByRoutingTeam(RoutingTeam routingTeam) {
        validateRoutingTeam(routingTeam);

        String sql = baseSelect() + """
                WHERE routing_team = ?
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, stmt -> stmt.setString(1, routingTeam.name()));
    }

    // finds all support tickets across every routing team
    @Override
    public List<SupportTicket> findAll() {
        String sql = baseSelect() + """
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, null);
    }

    // finds all open support tickets
    public List<SupportTicket> findOpenTickets() {
        String sql = baseSelect() + """
                WHERE status = 'OPEN'
                ORDER BY created_at DESC
                """;

        return executeListQuery(sql, null);
    }

    // marks a support ticket as resolved
    public void markResolved(int ticketId, int resolvedByUserAccountId) {
        validatePositive(ticketId, "Ticket ID must be positive.");
        validatePositive(resolvedByUserAccountId, "Resolved by user account ID must be positive.");

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

    // marks a support ticket as canceled
    public void markCancelled(int ticketId) {
        validatePositive(ticketId, "Ticket ID must be positive.");

        String sql = """
                UPDATE support_ticket
                SET status = 'CANCELLED'
                WHERE ticket_id = ?
                  AND status = 'OPEN'
                """;

        executeUpdate(sql, stmt -> stmt.setInt(1, ticketId));
    }

    // builds base select query
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

    // binds insert parameters
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

    // maps one result set row to support ticket
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

    // executes a list query
    private List<SupportTicket> executeListQuery(String sql, StatementBinder binder) {
        List<SupportTicket> tickets = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
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
            throw new RuntimeException("Failed to query support tickets.", e);
        }

        return tickets;
    }

    // executes an update statement
    private void executeUpdate(String sql, StatementBinder binder) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            binder.bind(stmt);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Open support ticket not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update support ticket.", e);
        }
    }

    // validates support ticket input
    private void validateTicket(SupportTicket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Support ticket is required.");
        }
    }

    // validates routing team input
    private void validateRoutingTeam(RoutingTeam routingTeam) {
        if (routingTeam == null) {
            throw new IllegalArgumentException("Routing team is required.");
        }
    }

    // validates positive number input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // sets nullable integer value
    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    // sets nullable timestamp value
    private void setNullableTimestamp(PreparedStatement stmt, int index, java.time.LocalDateTime value)
            throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.TIMESTAMP);
            return;
        }

        stmt.setTimestamp(index, Timestamp.valueOf(value));
    }

    // gets nullable integer value
    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    // gets nullable date time value
    private java.time.LocalDateTime getNullableDateTime(ResultSet rs, String columnName) throws SQLException {
        Timestamp value = rs.getTimestamp(columnName);
        return value == null ? null : value.toLocalDateTime();
    }

    // binds prepared statement values
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}