package motorph.infrastructure.repository;

import motorph.domain.model.UserAccessLog;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for user access logs.
 */
public class UserAccessLogRepository {

    // insert log
    public void save(UserAccessLog log) {
        String sql = "INSERT INTO user_access_logs " +
                "(timestamp, employee_id, role_id, action, details, ip_address, device_info) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(log.getTimestamp()));
            stmt.setInt(2, log.getEmployeeId());
            stmt.setInt(3, log.getRoleId());
            stmt.setString(4, log.getAction());
            stmt.setString(5, log.getDetails());
            stmt.setString(6, log.getIpAddress());
            stmt.setString(7, log.getDeviceInfo());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // find all logs
    public List<UserAccessLog> findAll() {
        List<UserAccessLog> list = new ArrayList<>();
        String sql = "SELECT * FROM user_access_logs ORDER BY timestamp DESC";

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

    // find logs by employee
    public List<UserAccessLog> findByEmployeeId(int employeeId) {
        List<UserAccessLog> list = new ArrayList<>();
        String sql = "SELECT * FROM user_access_logs WHERE employee_id = ? ORDER BY timestamp DESC";

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

    // find logs by role
    public List<UserAccessLog> findByRoleId(int roleId) {
        List<UserAccessLog> list = new ArrayList<>();
        String sql = "SELECT * FROM user_access_logs WHERE role_id = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find logs by action
    public List<UserAccessLog> findByAction(String action) {
        List<UserAccessLog> list = new ArrayList<>();
        String sql = "SELECT * FROM user_access_logs WHERE action = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, action);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // search logs by keyword
    public List<UserAccessLog> search(String keyword) {
        List<UserAccessLog> list = new ArrayList<>();
        String sql = "SELECT * FROM user_access_logs " +
                "WHERE action LIKE ? OR details LIKE ? " +
                "ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // find logs within date range
    public List<UserAccessLog> findByDateRange(Timestamp start, Timestamp end) {
        List<UserAccessLog> list = new ArrayList<>();
        String sql = "SELECT * FROM user_access_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // delete log (optional)
    public void delete(int logId) {
        String sql = "DELETE FROM user_access_logs WHERE log_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, logId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // maps database row to UserAccessLog object
    private UserAccessLog mapRow(ResultSet rs) throws SQLException {
        return new UserAccessLog(
                rs.getTimestamp("timestamp").toLocalDateTime(),
                rs.getInt("employee_id"),
                rs.getInt("role_id"),
                rs.getString("action"),
                rs.getString("details"),
                rs.getString("ip_address"),
                rs.getString("device_info")
        );
    }
}