package motorph.domain.model;

import java.util.List;

import motorph.domain.value.Role;

/**
 * Concrete employee subclass representing an IT/System administrator user.
 * Extends Employee to provide access to system configuration and user
 * management.
 * Exclusive responsibility for user account creation, role assignment, and
 * system security/maintenance.
 * Handles technical aspects while AdminUser focuses on payroll operations.
 */
public class ITUser extends Employee {

    public ITUser(int employeeId, String lastName, String firstName, String birthday,
                  String address, String phoneNumber, String sssNumber, String philhealthNumber,
                  String tinNumber, String pagIbigNumber, String status, String position,
                  String immediateSupervisor, double basicSalary, double riceSubsidy,
                  double phoneAllowance, double clothingAllowance, double grossSemiMonthlyRate,
                  double hourlyRate, String personalEmail, String photoPath) {

        super(employeeId, lastName, firstName, birthday, address, phoneNumber,
                sssNumber, philhealthNumber, tinNumber, pagIbigNumber, status, position,
                immediateSupervisor, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMonthlyRate, hourlyRate,
                personalEmail, photoPath, Role.IT);
    }

    @Override
    public List<String> getMenuItems() {
        return List.of(
                "Dashboard",
                "Manage Accounts", // crud accounts
                "Roles & Permission",
                "User Access Logs",
                "Database Backup",
                "System Configuration");
    }
}
