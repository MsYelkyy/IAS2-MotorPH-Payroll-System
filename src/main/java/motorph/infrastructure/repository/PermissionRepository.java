package motorph.infrastructure.repository;

import motorph.domain.model.Permission;
import motorph.domain.value.Role;
import motorph.infrastructure.config.DatabaseConnection;
import motorph.domain.value.PermissionKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles database operations for role permission records.
 */
public class PermissionRepository {

    // finds permission keys assigned to a role
    public Set<String> findPermissionKeysByRole(Role role) {
        validateRole(role);

        String sql = """
                SELECT p.permission_key
                FROM role_permission rp
                JOIN role r ON rp.role_id = r.role_id
                JOIN permission p ON rp.permission_id = p.permission_id
                WHERE r.role_name = ?
                """;

        Set<String> permissionKeys = new HashSet<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissionKeys.add(rs.getString("permission_key"));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find permission keys by role.", e);
        }

        return permissionKeys;
    }

    // finds permissions assigned to a role
    public Set<Permission> findPermissionsByRole(Role role) {
        validateRole(role);

        String sql = """
                SELECT p.permission_id,
                       p.permission_key,
                       p.module,
                       p.action,
                       p.record_type,
                       p.description,
                       p.created_at,
                       p.updated_at
                FROM role_permission rp
                JOIN role r ON rp.role_id = r.role_id
                JOIN permission p ON rp.permission_id = p.permission_id
                WHERE r.role_name = ?
                ORDER BY p.module, p.action
                """;

        Set<Permission> permissions = new HashSet<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find permissions by role.", e);
        }

        return permissions;
    }

    // finds all system permissions
    public List<Permission> findAllPermissions() {
        String sql = """
            SELECT permission_id,
                   permission_key,
                   module,
                   action,
                   record_type,
                   description,
                   created_at,
                   updated_at
            FROM permission
            ORDER BY module, action, permission_key
            """;

        List<Permission> permissions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                permissions.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all permissions.", e);
        }

        return permissions;
    }

    // checks if a role has one permission key
    public boolean roleHasPermission(Role role, String permissionKey) {
        validateRole(role);
        validatePermissionKey(permissionKey);

        String sql = """
                SELECT 1
                FROM role_permission rp
                JOIN role r ON rp.role_id = r.role_id
                JOIN permission p ON rp.permission_id = p.permission_id
                WHERE r.role_name = ?
                  AND p.permission_key = ?
                LIMIT 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());
            stmt.setString(2, permissionKey.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check role permission.", e);
        }
    }

    // updates all permissions assigned to a role
    public void updateRolePermissions(Role role, Set<PermissionKey> permissionKeys) {
        validateRole(role);

        if (permissionKeys == null) {
            throw new IllegalArgumentException("Permission keys are required.");
        }

        String deleteSql = """
            DELETE rp
            FROM role_permission rp
            JOIN role r ON rp.role_id = r.role_id
            WHERE r.role_name = ?
            """;

        String insertSql = """
            INSERT INTO role_permission (role_id, permission_id)
            SELECT r.role_id, p.permission_id
            FROM role r
            JOIN permission p ON p.permission_key = ?
            WHERE r.role_name = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, role.name());
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (PermissionKey permissionKey : permissionKeys) {
                    if (permissionKey == null) {
                        continue;
                    }

                    insertStmt.setString(1, permissionKey.name());
                    insertStmt.setString(2, role.name());
                    insertStmt.addBatch();
                }

                insertStmt.executeBatch();
            }

            conn.commit();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update role permissions.", e);
        }
    }

    // maps one result set row to a permission
    private Permission mapRow(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("permission_id"),
                rs.getString("permission_key"),
                rs.getString("module"),
                rs.getString("action"),
                rs.getString("record_type"),
                rs.getString("description"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    // validates role input
    private void validateRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
    }

    // validates permission key input
    private void validatePermissionKey(String permissionKey) {
        if (permissionKey == null || permissionKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission key is required.");
        }
    }
}