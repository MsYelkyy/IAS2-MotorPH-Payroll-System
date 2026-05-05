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

    private static final String BASE_SELECT = """
            SELECT employee_id,
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
            FROM employee
            """;

    // finds active employee by id
    public Employee findActiveById(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE employee_id = ?
                  AND is_active = 1
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
        String sql = BASE_SELECT + """
                WHERE is_active = 1
                ORDER BY employee_id
                """;

        return executeListQuery(sql, null);
    }

    // finds all employees including inactive
    public List<Employee> findAllWithInactive() {
        String sql = BASE_SELECT + " ORDER BY employee_id";
        return executeListQuery(sql, null);
    }

    // finds personal email by employee id
    public String findPersonalEmailByEmployeeId(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = """
                SELECT personal_email
                FROM employee
                WHERE employee_id = ?
                  AND is_active = 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("personal_email");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find personal email by employee id.", e);
        }

        return null;
    }

    // saves a new employee
    public void save(Employee employee) {
        validateEmployee(employee);

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

            bindEmployeeFields(stmt, employee);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save employee.", e);
        }
    }

    // saves a new employee and returns the generated employee id
    public int saveAndReturnId(Employee employee) {
        validateEmployee(employee);

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

            bindEmployeeFields(stmt, employee);
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

    // updates an existing employee
    public void update(Employee employee) {
        validateEmployee(employee);

        if (employee.getEmployeeId() == null || employee.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Employee ID is required for update.");
        }

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

            bindEmployeeFields(stmt, employee);
            stmt.setInt(15, employee.getEmployeeId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Employee not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update employee.", e);
        }
    }

    // updates employee contact information only
    public void updateContactInfo(int employeeId, String phoneNumber, String address) {
        validatePositive(employeeId, "Employee ID must be positive.");
        validateRequired(phoneNumber, "Phone number is required.");
        validateRequired(address, "Address is required.");

        String sql = """
                UPDATE employee
                SET phone_number = ?,
                    address = ?
                WHERE employee_id = ?
                  AND is_active = 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, phoneNumber.trim());
            stmt.setString(2, address.trim());
            stmt.setInt(3, employeeId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Active employee not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update employee contact information.", e);
        }
    }

    // updates employee personal email
    public void updatePersonalEmail(int employeeId, String personalEmail) {
        validatePositive(employeeId, "Employee ID must be positive.");
        validateRequired(personalEmail, "Personal email is required.");

        String sql = """
                UPDATE employee
                SET personal_email = ?
                WHERE employee_id = ?
                  AND is_active = 1
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, personalEmail.trim());
            stmt.setInt(2, employeeId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Active employee not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update employee personal email.", e);
        }
    }

    // deactivates an employee
    public void deactivate(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = "UPDATE employee SET is_active = 0 WHERE employee_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Employee not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate employee.", e);
        }
    }

    // executes a list query and maps all rows
    private List<Employee> executeListQuery(String sql, StatementBinder binder) {
        List<Employee> employees = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (binder != null) {
                binder.bind(stmt);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    employees.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to query employees.", e);
        }

        return employees;
    }

    // binds employee fields for insert or update
    private void bindEmployeeFields(PreparedStatement stmt, Employee employee) throws SQLException {
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

    // validates employee input
    private void validateEmployee(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee is required.");
        }

        validateRequired(employee.getFirstName(), "First name is required.");
        validateRequired(employee.getLastName(), "Last name is required.");

        if (employee.getEmploymentStatus() == null) {
            throw new IllegalArgumentException("Employment status is required.");
        }

        validatePositive(employee.getPositionId(), "Position ID must be positive.");
        validatePositive(employee.getDepartmentId(), "Department ID must be positive.");

        if (employee.getHireDate() == null) {
            throw new IllegalArgumentException("Hire date is required.");
        }
    }

    // validates positive number input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    // validates required text
    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
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

    // binds prepared statement parameters
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}