package motorph.infrastructure.repository;

import motorph.domain.model.LeaveRequest;
import motorph.domain.value.LeaveStatus;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for leave requests.
 */
public class LeaveRepository {

    // find all leave requests
    public List<LeaveRequest> findAll() {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM leave_request";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find leave by id
    public LeaveRequest findById(int leaveRequestId) {
        String sql = "SELECT * FROM leave_request WHERE leave_request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, leaveRequestId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // find leave by employee
    public List<LeaveRequest> findByEmployeeId(int employeeId) {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM leave_request WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find leave by status
    public List<LeaveRequest> findByStatus(LeaveStatus status) {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM leave_request WHERE request_status = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find pending leave requests
    public List<LeaveRequest> findPending() {
        return findByStatus(LeaveStatus.PENDING);
    }

    // insert leave request
    public void save(LeaveRequest req) {
        String sql = "INSERT INTO leave_request " +
                "(employee_id, leave_type_id, start_date, end_date, leave_hours, reason, request_status, date_submitted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, req.getEmployeeId());
            stmt.setInt(2, Integer.parseInt(req.getLeaveType()));
            stmt.setDate(3, new java.sql.Date(req.getFromDate().getTime()));
            stmt.setDate(4, new java.sql.Date(req.getToDate().getTime()));
            stmt.setDouble(5, 0);
            stmt.setString(6, req.getReason());
            stmt.setString(7, req.getStatus().name());
            stmt.setTimestamp(8, new Timestamp(req.getRequestDate().getTime()));

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update leave status
    public void updateStatus(int leaveRequestId, LeaveStatus status) {
        String sql = "UPDATE leave_request SET request_status = ? WHERE leave_request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, leaveRequestId);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // cancel leave request
    public void cancel(int leaveRequestId) {
        updateStatus(leaveRequestId, LeaveStatus.CANCELLED);
    }

    // maps result set to LeaveRequest
    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        return new LeaveRequest(
                String.valueOf(rs.getInt("leave_request_id")),
                rs.getInt("employee_id"),
                rs.getTimestamp("date_submitted"),
                rs.getDate("start_date"),
                rs.getDate("end_date"),
                rs.getString("reason"),
                String.valueOf(rs.getInt("leave_type_id")),
                LeaveStatus.fromString(rs.getString("request_status")),
                null,
                null
        );
    }
}