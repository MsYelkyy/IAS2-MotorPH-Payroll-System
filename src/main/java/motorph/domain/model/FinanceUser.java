package motorph.domain.model;

import java.util.List;

import motorph.domain.value.Role;

/**
 * Concrete employee subclass representing a finance department user.
 * Extends Employee to provide access to payroll management features
 * like generating employee payroll, creating payslips, and viewing payroll
 * reports.
 */
public class FinanceUser extends Employee {

    public FinanceUser(int employeeId, String lastName, String firstName, String birthday,
            String address, String phoneNumber, String sssNumber, String philhealthNumber,
            String tinNumber, String pagIbigNumber, String status, String position,
            String immediateSupervisor, double basicSalary, double riceSubsidy,
            double phoneAllowance, double clothingAllowance, double grossSemiMonthlyRate,
            double hourlyRate, String personalEmail, String photoPath) {
        super(employeeId, lastName, firstName, birthday, address, phoneNumber,
                sssNumber, philhealthNumber, tinNumber, pagIbigNumber, status, position,
                immediateSupervisor, basicSalary, riceSubsidy, phoneAllowance,
                clothingAllowance, grossSemiMonthlyRate, hourlyRate, personalEmail, photoPath, Role.FINANCE);
    }

    @Override
    public List<String> getMenuItems() {
        return List.of(
                "Dashboard",
                "Payroll Management", // Generate Employee Payroll", Generate Payslip, View Payroll Reports
                "Print Payslip");
    }
}
