package motorph.domain.model;

import java.util.List;

import motorph.domain.value.Role;

/**
 * Concrete employee subclass representing a Human Resources department user.
 * Extends Employee to provide access to HR features like
 * employee management, attendance tracking, and leave request approval.
 */
public class HrUser extends Employee {

    public HrUser(int employeeId, String lastName, String firstName, String birthday,
                     String address, String phoneNumber, String sssNumber, String philhealthNumber,
                     String tinNumber, String pagIbigNumber, String status, String position,
                     String immediateSupervisor, double basicSalary, double riceSubsidy,
                     double phoneAllowance, double clothingAllowance, double grossSemiMonthlyRate,
                     double hourlyRate, String personalEmail, String photoPath) {

        super(employeeId, lastName, firstName, birthday, address, phoneNumber,
                sssNumber, philhealthNumber, tinNumber, pagIbigNumber, status, position,
                immediateSupervisor, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMonthlyRate, hourlyRate,
                personalEmail, photoPath, Role.HR);
    }

    @Override
    public List<String> getMenuItems() {
        return List.of(
                "Dashboard",
                "Attendance Overview",
                "Employee Management",
                "Leave Management");
    }
}
