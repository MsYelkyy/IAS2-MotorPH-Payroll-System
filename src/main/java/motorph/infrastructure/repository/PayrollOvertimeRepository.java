package motorph.infrastructure.repository;

import motorph.application.exception.DataAccessException;

import motorph.domain.model.PayrollOvertime;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for payroll overtime records.
 */
public class PayrollOvertimeRepository {

    // finds payroll overtime by id
    public PayrollOvertime findById(int payrollOvertimeId) {
        validatePositive(payrollOvertimeId, "Payroll overtime ID must be positive.");

        String sql = """
                SELECT po.payroll_overtime_id,
                       po.payroll_result_id,
                       otr.overtime_type_id,
                       po.hours_worked,
                       po.hourly_rate,
                       po.amount
                FROM payroll_overtime po
                JOIN overtime_request otr
                  ON po.overtime_request_id = otr.overtime_request_id
                WHERE po.payroll_overtime_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollOvertimeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll overtime by id.", e);
        }

        return null;
    }

    // finds payroll overtime records by payroll result id
    public List<PayrollOvertime> findByPayrollResultId(int payrollResultId) {
        validatePositive(payrollResultId, "Payroll result ID must be positive.");

        List<PayrollOvertime> overtimeRecords = new ArrayList<>();

        String sql = """
                SELECT po.payroll_overtime_id,
                       po.payroll_result_id,
                       otr.overtime_type_id,
                       po.hours_worked,
                       po.hourly_rate,
                       po.amount
                FROM payroll_overtime po
                JOIN overtime_request otr
                  ON po.overtime_request_id = otr.overtime_request_id
                WHERE po.payroll_result_id = ?
                ORDER BY po.payroll_overtime_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollResultId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    overtimeRecords.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll overtime records by payroll result id.", e);
        }

        return overtimeRecords;
    }

    // finds payroll overtime by overtime request id
    public PayrollOvertime findByOvertimeRequestId(int overtimeRequestId) {
        validatePositive(overtimeRequestId, "Overtime request ID must be positive.");

        String sql = """
                SELECT po.payroll_overtime_id,
                       po.payroll_result_id,
                       otr.overtime_type_id,
                       po.hours_worked,
                       po.hourly_rate,
                       po.amount
                FROM payroll_overtime po
                JOIN overtime_request otr
                  ON po.overtime_request_id = otr.overtime_request_id
                WHERE po.overtime_request_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, overtimeRequestId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find payroll overtime by overtime request id.", e);
        }

        return null;
    }

    // checks if payroll overtime already exists for overtime request
    public boolean existsByOvertimeRequestId(int overtimeRequestId) {
        validatePositive(overtimeRequestId, "Overtime request ID must be positive.");

        String sql = """
                SELECT COUNT(*)
                FROM payroll_overtime
                WHERE overtime_request_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, overtimeRequestId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to check payroll overtime existence.", e);
        }
    }

    // saves a payroll overtime record
    public void save(PayrollOvertime payrollOvertime, int overtimeRequestId) {
        validatePayrollOvertime(payrollOvertime);
        validatePositive(overtimeRequestId, "Overtime request ID must be positive.");

        String sql = """
                INSERT INTO payroll_overtime (
                    payroll_result_id,
                    overtime_request_id,
                    hours_worked,
                    hourly_rate,
                    amount
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, payrollOvertime.getPayrollResultId());
            stmt.setInt(2, overtimeRequestId);
            stmt.setDouble(3, payrollOvertime.getHoursWorked());
            stmt.setDouble(4, payrollOvertime.getHourlyRate());
            stmt.setDouble(5, payrollOvertime.getAmount());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save payroll overtime.", e);
        }
    }

    // maps result set to payroll overtime
    private PayrollOvertime mapRow(ResultSet rs) throws SQLException {
        return new PayrollOvertime(
                rs.getInt("payroll_overtime_id"),
                rs.getInt("payroll_result_id"),
                rs.getInt("overtime_type_id"),
                rs.getDouble("hours_worked"),
                rs.getDouble("hourly_rate"),
                rs.getDouble("amount")
        );
    }

    // validates payroll overtime input
    private void validatePayrollOvertime(PayrollOvertime payrollOvertime) {
        if (payrollOvertime == null) {
            throw new IllegalArgumentException("Payroll overtime is required.");
        }
    }

    // validates required positive id
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}