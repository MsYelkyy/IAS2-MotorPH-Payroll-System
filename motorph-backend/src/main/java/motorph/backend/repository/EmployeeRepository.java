package motorph.backend.repository;

import motorph.backend.exception.DataAccessException;

import motorph.backend.domain.Employee;
import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
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
@Repository
public class EmployeeRepository {

    /** Holds the joined employee profile data including resolved names. */
    public static class ProfileQueryResult {

        private final Employee employee;
        private final String positionName;
        private final String departmentName;
        private final String supervisorName;

        public ProfileQueryResult(Employee employee, String positionName, String departmentName, String supervisorName) {
            this.employee = employee;
            this.positionName = positionName;
            this.departmentName = departmentName;
            this.supervisorName = supervisorName;
        }

        public Employee getEmployee() {
            return employee;
        }

        public String getPositionName() {
            return positionName;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public String getSupervisorName() {
            return supervisorName;
        }
    }

    private static final String BASE_SELECT = """
            SELECT employee_id,
                   first_name,
                   last_name,
                   birth_date,
                   gender,
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

    private final DataSource dataSource;

    public EmployeeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Finds an employee by ID regardless of active status.
     */
    public Employee findAnyById(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = BASE_SELECT + "WHERE employee_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find employee by id.", e);
        }

        return null;
    }

    public Employee findActiveById(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = BASE_SELECT + """
                WHERE employee_id = ?
                  AND is_active = 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find active employee by id.", e);
        }

        return null;
    }

    public List<Employee> findAllActive() {
        String sql = BASE_SELECT + """
                WHERE is_active = 1
                ORDER BY employee_id
                """;

        return executeListQuery(sql);
    }

    public List<Employee> findAllWithInactive() {
        String sql = BASE_SELECT + " ORDER BY employee_id";
        return executeListQuery(sql);
    }

    public String findPersonalEmailByEmployeeId(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = """
                SELECT personal_email
                FROM employee
                WHERE employee_id = ?
                  AND is_active = 1
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("personal_email");
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find personal email by employee id.", e);
        }

        return null;
    }

    /**
     * Finds an active employee's profile, including the position, department, and supervisor names.
     */
    public ProfileQueryResult findProfileById(int employeeId) {
        return findProfileQuery(employeeId, true);
    }

    /**
     * Same as {@link #findProfileById(int)}, but also returns inactive employees.
     */
    public ProfileQueryResult findProfileByIdAny(int employeeId) {
        return findProfileQuery(employeeId, false);
    }

    private ProfileQueryResult findProfileQuery(int employeeId, boolean activeOnly) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = """
                SELECT e.employee_id,
                       e.first_name,
                       e.last_name,
                       e.birth_date,
                       e.gender,
                       e.address,
                       e.civil_status,
                       e.phone_number,
                       e.personal_email,
                       e.employment_status,
                       e.position_id,
                       e.department_id,
                       e.hire_date,
                       e.supervisor_id,
                       e.is_active,
                       e.photo_path,
                       p.position_name,
                       d.department_name,
                       CONCAT(sup.first_name, ' ', sup.last_name) AS supervisor_name
                FROM employee e
                LEFT JOIN position p ON e.position_id = p.position_id
                LEFT JOIN department d ON e.department_id = d.department_id
                LEFT JOIN employee sup ON e.supervisor_id = sup.employee_id
                WHERE e.employee_id = ?
                """
                + (activeOnly ? "  AND e.is_active = 1" : "");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Employee employee = mapRow(rs);
                    String positionName   = rs.getString("position_name");
                    String departmentName = rs.getString("department_name");
                    String supervisorName = rs.getString("supervisor_name");
                    return new ProfileQueryResult(employee, positionName, departmentName, supervisorName);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to find employee profile.", e);
        }

        return null;
    }

    public int saveAndReturnId(Employee employee) {
        String sql = """
                INSERT INTO employee (
                    first_name,
                    last_name,
                    birth_date,
                    gender,
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            bindEmployeeFields(stmt, employee);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to save employee and return generated ID.", e);
        }

        throw new IllegalStateException("Failed to retrieve generated employee ID.");
    }

    public void update(Employee employee) {
        if (employee.getEmployeeId() == null || employee.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Employee ID is required for update.");
        }

        String sql = """
                UPDATE employee
                SET first_name = ?,
                    last_name = ?,
                    birth_date = ?,
                    gender = ?,
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            bindEmployeeFields(stmt, employee);
            stmt.setInt(16, employee.getEmployeeId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Employee not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to update employee.", e);
        }
    }

    public void deactivate(int employeeId) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = "UPDATE employee SET is_active = 0 WHERE employee_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new IllegalArgumentException("Employee not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to deactivate employee.", e);
        }
    }

    private void bindEmployeeFields(PreparedStatement stmt, Employee employee) throws SQLException {
        stmt.setString(1, employee.getFirstName());
        stmt.setString(2, employee.getLastName());
        setNullableDate(stmt, 3, employee.getBirthDate());
        stmt.setString(4, employee.getGender().name());
        stmt.setString(5, employee.getAddress());
        stmt.setString(6, employee.getCivilStatus());
        stmt.setString(7, employee.getPhoneNumber());
        stmt.setString(8, employee.getPersonalEmail());
        stmt.setString(9, employee.getEmploymentStatus().name());
        stmt.setInt(10, employee.getPositionId());
        stmt.setInt(11, employee.getDepartmentId());
        stmt.setDate(12, Date.valueOf(employee.getHireDate()));
        setNullableInteger(stmt, 13, employee.getSupervisorId());
        stmt.setBoolean(14, employee.isActive());
        stmt.setString(15, employee.getPhotoPath());
    }

    private List<Employee> executeListQuery(String sql) {
        List<Employee> employees = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                employees.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to query employees.", e);
        }

        return employees;
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("employee_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                getLocalDate(rs, "birth_date"),
                Gender.fromValue(rs.getString("gender")),
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

    /**
     * Updates the contact fields an employee is allowed to change on their own profile.
     */
    public void updateContactInfo(int employeeId, String phoneNumber, String address) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = """
                UPDATE employee
                SET phone_number = ?,
                    address = ?
                WHERE employee_id = ?
                  AND is_active = 1
                """;

        executeEmployeeUpdate(sql, "Failed to update employee contact info.", stmt -> {
            stmt.setString(1, phoneNumber.trim());
            stmt.setString(2, address.trim());
            stmt.setInt(3, employeeId);
        });
    }

    /**
     * Stores the filename of the employee's profile photo.
     */
    public void updatePhotoPath(int employeeId, String photoPath) {
        validatePositive(employeeId, "Employee ID must be positive.");

        String sql = """
                UPDATE employee
                SET photo_path = ?
                WHERE employee_id = ?
                  AND is_active = 1
                """;

        executeEmployeeUpdate(sql, "Failed to update employee photo path.", stmt -> {
            stmt.setString(1, photoPath);
            stmt.setInt(2, employeeId);
        });
    }

    private void executeEmployeeUpdate(String sql, String errorMessage, StatementBinder binder) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            binder.bind(stmt);

            if (stmt.executeUpdate() == 0) {
                throw new IllegalArgumentException("Active employee not found.");
            }

        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        }
    }

    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }

    private LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
        Date value = rs.getDate(columnName);
        return value == null ? null : value.toLocalDate();
    }

    private void setNullableDate(PreparedStatement stmt, int index, LocalDate value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.DATE);
            return;
        }
        stmt.setDate(index, Date.valueOf(value));
    }

    private void setNullableInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.INTEGER);
            return;
        }
        stmt.setInt(index, value);
    }

    private Integer getNullableInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}
