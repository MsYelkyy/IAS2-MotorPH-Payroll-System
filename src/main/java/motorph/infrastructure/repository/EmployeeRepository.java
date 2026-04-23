package motorph.infrastructure.repository;

import motorph.domain.model.Employee;
import motorph.domain.value.EmploymentStatus;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for employee records.
 */
public class EmployeeRepository {

    // finds active employee by id
    public Employee findActiveById(int employeeId) {
        String sql = """
                SELECT employee_id, first_name, last_name, birth_date, address, civil_status,
                       phone_number, personal_email, employment_status, position_id,
                       department_id, hire_date, supervisor_id, is_active, photo_path
                FROM employee
                WHERE employee_id = ? AND is_active = 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active employee by id.", e);
        }

        return null;
    }

    // finds all active employees
    public List<Employee> findAllActive() {
        List<Employee> employees = new ArrayList<>();

        String sql = """
                SELECT employee_id, first_name, last_name, birth_date, address, civil_status,
                       phone_number, personal_email, employment_status, position_id,
                       department_id, hire_date, supervisor_id, is_active, photo_path
                FROM employee
                WHERE is_active = 1
                ORDER BY employee_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active employees.", e);
        }

        return employees;
    }

    // finds all employees including inactive
    public List<Employee> findAllWithInactive() {
        List<Employee> employees = new ArrayList<>();

        String sql = """
                SELECT employee_id, first_name, last_name, birth_date, address, civil_status,
                       phone_number, personal_email, employment_status, position_id,
                       department_id, hire_date, supervisor_id, is_active, photo_path
                FROM employee
                ORDER BY employee_id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find employees.", e);
        }

        return employees;
    }

    // saves a new employee
    public void save(Employee employee) {
        String sql = """
                INSERT INTO employee (
                    first_name,
                    last_name,
                    birth_date,
                    address,
                    civil_status,
                    phone_number,
                    personal_email,
                    employment_status,
                    position_id,
                    department_id,
                    hire_date,
                    supervisor_id,
                    is_active,
                    photo_path
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, employee.getFirstName());
            stmt.setString(2, employee.getLastName());
            setNullableDate(stmt, 3, employee.getBirthDate());
            stmt.setString(4, employee.getAddress());
            stmt.setString(5, employee.getCivilStatus());
            stmt.setString(6, employee.getPhoneNumber());
            stmt.setString(7, employee.getPersonalEmail());
            stmt.setString(8, employee.getEmploymentStatus().name());
            stmt.setInt(9, employee.getPositionId());
            stmt.setInt(10, employee.getDepartmentId());
            stmt.setDate(11, Date.valueOf(employee.getHireDate()));
            setNullableInteger(stmt, 12, employee.getSupervisorId());
            stmt.setBoolean(13, employee.isActive());
            stmt.setString(14, employee.getPhotoPath());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save employee.", e);
        }
    }

    // updates an existing employee
    public void update(Employee employee) {
        String sql = """
                UPDATE employee
                SET first_name = ?,
                    last_name = ?,
                    birth_date = ?,
                    address = ?,
                    civil_status = ?,
                    phone_number = ?,
                    personal_email = ?,
                    employment_status = ?,
                    position_id = ?,
                    department_id = ?,
                    hire_date = ?,
                    supervisor_id = ?,
                    is_active = ?,
                    photo_path = ?
                WHERE employee_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, employee.getFirstName());
            stmt.setString(2, employee.getLastName());
            setNullableDate(stmt, 3, employee.getBirthDate());
            stmt.setString(4, employee.getAddress());
            stmt.setString(5, employee.getCivilStatus());
            stmt.setString(6, employee.getPhoneNumber());
            stmt.setString(7, employee.getPersonalEmail());
            stmt.setString(8, employee.getEmploymentStatus().name());
            stmt.setInt(9, employee.getPositionId());
            stmt.setInt(10, employee.getDepartmentId());
            stmt.setDate(11, Date.valueOf(employee.getHireDate()));
            setNullableInteger(stmt, 12, employee.getSupervisorId());
            stmt.setBoolean(13, employee.isActive());
            stmt.setString(14, employee.getPhotoPath());
            stmt.setInt(15, employee.getEmployeeId());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update employee.", e);
        }
    }

    // deactivates an employee
    public void deactivate(int employeeId) {
        String sql = "UPDATE employee SET is_active = 0 WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate employee.", e);
        }
    }

    // maps the result set to an employee
    private Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("employee_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                getLocalDate(rs, "birth_date"),
                rs.getString("address"),
                rs.getString("civil_status"),
                rs.getString("phone_number"),
                rs.getString("personal_email"),
                EmploymentStatus.fromValue(rs.getString("employment_status")),
                rs.getInt("position_id"),
                rs.getInt("department_id"),
                rs.getDate("hire_date").toLocalDate(),
                getNullableInteger(rs, "supervisor_id"),
                rs.getBoolean("is_active"),
                rs.getString("photo_path")
        );
    }

    // saves a new employee and returns the generated employee id
    public int saveAndReturnId(Employee employee) {
        String sql = """
            INSERT INTO employee (
                first_name,
                last_name,
                birth_date,
                address,
                civil_status,
                phone_number,
                personal_email,
                employment_status,
                position_id,
                department_id,
                hire_date,
                supervisor_id,
                is_active,
                photo_path
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, employee.getFirstName());
            stmt.setString(2, employee.getLastName());
            setNullableDate(stmt, 3, employee.getBirthDate());
            stmt.setString(4, employee.getAddress());
            stmt.setString(5, employee.getCivilStatus());
            stmt.setString(6, employee.getPhoneNumber());
            stmt.setString(7, employee.getPersonalEmail());
            stmt.setString(8, employee.getEmploymentStatus().name());
            stmt.setInt(9, employee.getPositionId());
            stmt.setInt(10, employee.getDepartmentId());
            stmt.setDate(11, Date.valueOf(employee.getHireDate()));
            setNullableInteger(stmt, 12, employee.getSupervisorId());
            stmt.setBoolean(13, employee.isActive());
            stmt.setString(14, employee.getPhotoPath());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save employee and return generated ID.", e);
        }

        throw new IllegalStateException("Failed to retrieve generated employee ID.");
    }

    // sets a nullable local date
    private void setNullableDate(PreparedStatement stmt, int index, LocalDate value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.DATE);
            return;
        }

        stmt.setDate(index, Date.valueOf(value));
    }

    // sets a nullable integer
    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }

        stmt.setInt(index, value);
    }

    // gets a nullable local date
    private LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
        Date value = rs.getDate(columnName);
        return value == null ? null : value.toLocalDate();
    }

    // gets a nullable integer
    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}