package motorph.domain.contract;

import java.time.LocalDate;
import java.util.List;
import motorph.domain.model.Attendance;
import motorph.domain.model.PayrollBreakdown;

/**
 * Defines payroll operations for a payable model.
 */
public interface Payable {

        // Returns the gross salary.
        double getGrossSalary();

        // Computes the payroll deductions.
        PayrollBreakdown computeDeductions(List<Attendance> attendanceList, LocalDate cutoffDate);
}