package motorph.infrastructure.repository;

import motorph.domain.model.Employee;
import motorph.domain.model.ProbEmployeeUser;
import motorph.domain.model.RegEmployeeUser;
import motorph.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for employee records.
 */
public class EmployeeRepository {

    // find employee by id
    public Employee findActiveById(int employeeId) {
        String sql = "SELECT * FROM employee WHERE employee_id = ? AND employment_status = 'ACTIVE'";

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

    // find all active employees
    public List<Employee> findAllActive() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employee WHERE employment_status = 'ACTIVE'";

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

    // find all employees
    public List<Employee> findAllWithInactive() {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT * FROM employee";

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

    // insert employee
    public void save(Employee emp) {
        String sql = "INSERT INTO employee " +
                "(last_name, first_name, birthday, address, phone_number, personal_email, photo_path, " +
                "sss_number, philhealth_number, tin_number, pagibig_number, " +
                "employment_status, position, supervisor, basic_salary, rice_subsidy, " +
                "phone_allowance, clothing_allowance, gross_rate, hourly_rate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, emp.getLastName());
            stmt.setString(2, emp.getFirstName());
            stmt.setString(3, emp.getBirthday());
            stmt.setString(4, emp.getAddress());
            stmt.setString(5, emp.getPhoneNumber());
            stmt.setString(6, emp.getPersonalEmail());
            stmt.setString(7, emp.getPhotoPath());
            stmt.setString(8, emp.getSssNumber());
            stmt.setString(9, emp.getPhilhealthNumber());
            stmt.setString(10, emp.getTinNumber());
            stmt.setString(11, emp.getPagIbigNumber());
            stmt.setString(12, emp.getStatus());
            stmt.setString(13, emp.getPosition());
            stmt.setString(14, emp.getImmediateSupervisor());
            stmt.setDouble(15, emp.getBasicSalary());
            stmt.setDouble(16, emp.getRiceSubsidy());
            stmt.setDouble(17, emp.getPhoneAllowance());
            stmt.setDouble(18, emp.getClothingAllowance());
            stmt.setDouble(19, emp.getGrossSemiMonthlyRate());
            stmt.setDouble(20, emp.getHourlyRate());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // update employee
    public void update(Employee emp) {
        String sql = "UPDATE employee SET " +
                "last_name=?, first_name=?, birthday=?, address=?, phone_number=?, personal_email=?, photo_path=?, " +
                "sss_number=?, philhealth_number=?, tin_number=?, pagibig_number=?, " +
                "employment_status=?, position=?, supervisor=?, basic_salary=?, rice_subsidy=?, " +
                "phone_allowance=?, clothing_allowance=?, gross_rate=?, hourly_rate=? " +
                "WHERE employee_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, emp.getLastName());
            stmt.setString(2, emp.getFirstName());
            stmt.setString(3, emp.getBirthday());
            stmt.setString(4, emp.getAddress());
            stmt.setString(5, emp.getPhoneNumber());
            stmt.setString(6, emp.getPersonalEmail());
            stmt.setString(7, emp.getPhotoPath());
            stmt.setString(8, emp.getSssNumber());
            stmt.setString(9, emp.getPhilhealthNumber());
            stmt.setString(10, emp.getTinNumber());
            stmt.setString(11, emp.getPagIbigNumber());
            stmt.setString(12, emp.getStatus());
            stmt.setString(13, emp.getPosition());
            stmt.setString(14, emp.getImmediateSupervisor());
            stmt.setDouble(15, emp.getBasicSalary());
            stmt.setDouble(16, emp.getRiceSubsidy());
            stmt.setDouble(17, emp.getPhoneAllowance());
            stmt.setDouble(18, emp.getClothingAllowance());
            stmt.setDouble(19, emp.getGrossSemiMonthlyRate());
            stmt.setDouble(20, emp.getHourlyRate());
            stmt.setInt(21, emp.getEmployeeId());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // deactivate employee
    public void deactivate(int employeeId) {
        String sql = "UPDATE employee SET employment_status = 'INACTIVE' WHERE employee_id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // maps result set to employee
    private Employee mapRow(ResultSet rs) throws SQLException {

        String status = rs.getString("employment_status");
        if (status != null) status = status.trim().toUpperCase();

        switch (status) {

            case "PROBATIONARY":
                return new ProbEmployeeUser(
                        rs.getInt("employee_id"),
                        rs.getString("last_name"),
                        rs.getString("first_name"),
                        rs.getString("birthday"),
                        rs.getString("address"),
                        rs.getString("phone_number"),
                        rs.getString("sss_number"),
                        rs.getString("philhealth_number"),
                        rs.getString("tin_number"),
                        rs.getString("pagibig_number"),
                        status,
                        rs.getString("position"),
                        rs.getString("supervisor"),
                        rs.getDouble("basic_salary"),
                        rs.getDouble("rice_subsidy"),
                        rs.getDouble("phone_allowance"),
                        rs.getDouble("clothing_allowance"),
                        rs.getDouble("gross_rate"),
                        rs.getDouble("hourly_rate"),
                        rs.getString("personal_email"),
                        rs.getString("photo_path")
                );

            default:
                return new RegEmployeeUser(
                        rs.getInt("employee_id"),
                        rs.getString("last_name"),
                        rs.getString("first_name"),
                        rs.getString("birthday"),
                        rs.getString("address"),
                        rs.getString("phone_number"),
                        rs.getString("sss_number"),
                        rs.getString("philhealth_number"),
                        rs.getString("tin_number"),
                        rs.getString("pagibig_number"),
                        status,
                        rs.getString("position"),
                        rs.getString("supervisor"),
                        rs.getDouble("basic_salary"),
                        rs.getDouble("rice_subsidy"),
                        rs.getDouble("phone_allowance"),
                        rs.getDouble("clothing_allowance"),
                        rs.getDouble("gross_rate"),
                        rs.getDouble("hourly_rate"),
                        rs.getString("personal_email"),
                        rs.getString("photo_path")
                );
        }
    }
}