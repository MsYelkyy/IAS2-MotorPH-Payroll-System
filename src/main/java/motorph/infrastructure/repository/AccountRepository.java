package motorph.infrastructure.repository;

import motorph.domain.model.Account;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for account records.
 */
public class AccountRepository {

    // find active account by username
    public Account findActiveByUsername(String username) {
        String sql = "SELECT * FROM account WHERE username = ? AND is_active = true";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // find account by username
    public Account findByUsername(String username) {
        String sql = "SELECT * FROM account WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // find account by employee id
    public Account findByEmployeeId(int employeeId) {
        String sql = "SELECT * FROM account WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // find all active accounts
    public List<Account> findAllActive() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM account WHERE is_active = true AND is_locked = false";

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

    // find all disabled accounts
    public List<Account> findAllDisabled() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM account WHERE is_active = false";

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

    // find all locked accounts
    public List<Account> findAllLocked() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM account WHERE is_locked = true";

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

    // find all accounts
    public List<Account> findAll() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM account";

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

    // insert account
    public void save(Account account) {
        String sql = "INSERT INTO account " +
                "(employee_id, username, company_email, password_hash, password_salt, role_id, is_locked, is_active, last_login_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, account.getEmployeeId());
            stmt.setString(2, account.getUsername());
            stmt.setString(3, account.getCompanyEmail());
            stmt.setString(4, account.getPasswordHash());
            stmt.setString(5, account.getPasswordSalt());
            stmt.setInt(6, account.getRoleId());
            stmt.setBoolean(7, account.isLocked());
            stmt.setBoolean(8, account.isActive());
            stmt.setTimestamp(9, null);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update role
    public void updateRole(int accountId, int roleId) {
        String sql = "UPDATE account SET role_id = ? WHERE account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            stmt.setInt(2, accountId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update lock status
    public void updateLockStatus(int accountId, boolean isLocked) {
        String sql = "UPDATE account SET is_locked = ? WHERE account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isLocked);
            stmt.setInt(2, accountId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update active status
    public void updateActiveStatus(int accountId, boolean isActive) {
        String sql = "UPDATE account SET is_active = ? WHERE account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, isActive);
            stmt.setInt(2, accountId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update password
    public void updatePassword(int accountId, String hash, String salt) {
        String sql = "UPDATE account SET password_hash = ?, password_salt = ? WHERE account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, hash);
            stmt.setString(2, salt);
            stmt.setInt(3, accountId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update last login
    public void updateLastLogin(int accountId) {
        String sql = "UPDATE account SET last_login_at = NOW() WHERE account_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, accountId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // maps result set to Account
    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getInt("account_id"),
                rs.getInt("employee_id"),
                rs.getString("username"),
                rs.getString("company_email"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                rs.getInt("role_id"),
                rs.getBoolean("is_locked"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("last_login_at")
        );
    }
}