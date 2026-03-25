package motorph.domain.model;

import java.util.List;

import motorph.domain.value.Role;

/**
 * Represents an admin user with access to administrative features.
 */
public class AdminUser extends Employee {

    // constructor
    public AdminUser(int employeeId, String lastName, String firstName, String birthday,
                     String address, String phoneNumber, String sssNumber, String philhealthNumber,
                     String tinNumber, String pagIbigNumber, String status, String position,
                     String immediateSupervisor, double basicSalary, double riceSubsidy,
                     double phoneAllowance, double clothingAllowance, double grossSemiMonthlyRate,
                     double hourlyRate, String personalEmail, String photoPath) {

        super(employeeId, lastName, firstName, birthday, address, phoneNumber,
                sssNumber, philhealthNumber, tinNumber, pagIbigNumber, status, position,
                immediateSupervisor, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMonthlyRate, hourlyRate,
                personalEmail, photoPath, Role.ADMIN);
    }

    // returns menu items for admin
    @Override
    public List<String> getMenuItems() {
        return List.of(
                "Dashboard",
                "Company Settings",
                "View All Reports");
    }
}