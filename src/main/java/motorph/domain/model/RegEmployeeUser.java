package motorph.domain.model;

import java.util.List;

import motorph.domain.value.Role;

/**
 * Concrete employee subclass representing a regular employee user.
 * Extends Employee to provide access to employee-specific features
 * like viewing profile, recording attendance, submitting leave requests, and
 * viewing payslips.
 */
public class RegEmployeeUser extends Employee {

    public RegEmployeeUser(int employeeId, String lastName, String firstName, String birthday,
            String address, String phoneNumber, String sssNumber, String philhealthNumber,
            String tinNumber, String pagIbigNumber, String status, String position,
            String immediateSupervisor, double basicSalary, double riceSubsidy,
            double phoneAllowance, double clothingAllowance, double grossSemiMonthlyRate,
            double hourlyRate, String personalEmail, String photoPath) {

        super(employeeId, lastName, firstName, birthday, address, phoneNumber,
                sssNumber, philhealthNumber, tinNumber, pagIbigNumber, status, position,
                immediateSupervisor, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMonthlyRate, hourlyRate, personalEmail, photoPath, Role.EMPLOYEE);
    }

    @Override
    public List<String> getMenuItems() {
        return List.of(
                "My Profile",
                "Attendance", // time-in, time-out, attendance logs record
                "Leave", // leave request fill up and table
                "View Payslip",
                "Print Payslip");
    }
}
