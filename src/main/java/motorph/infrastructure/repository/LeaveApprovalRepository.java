package motorph.infrastructure.repository;

import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Handles database operations for leave approval audit records.
 */
public class LeaveApprovalRepository {

    // saves a leave approval action
    public void save(int leaveRequestId, int approvedBy, String action, String remarks) {
        validatePositive(leaveRequestId, "Leave request ID must be positive.");
        validatePositive(approvedBy, "Approver user account ID must be positive.");
        validateAction(action);

        String sql = """
                INSERT INTO leave_approval (
                    leave_request_id,
                    approved_by,
                    action,
                    action_at,
                    remarks
                )
                VALUES (?, ?, ?, NOW(), ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leaveRequestId);
            stmt.setInt(2, approvedBy);
            stmt.setString(3, action.trim().toUpperCase());
            stmt.setString(4, clean(remarks));

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save leave approval action.", e);
        }
    }

    // validates positive number input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // validates leave approval action
    private void validateAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("Leave approval action is required.");
        }

        String cleanAction = action.trim().toUpperCase();

        if (!cleanAction.equals("APPROVED")
                && !cleanAction.equals("REJECTED")
                && !cleanAction.equals("CANCELLED")
                && !cleanAction.equals("UPDATED")) {
            throw new IllegalArgumentException("Invalid leave approval action: " + action);
        }
    }

    // cleans optional text
    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}