package motorph.domain.contract;

import motorph.domain.model.Attendance;
import motorph.domain.model.PayrollBreakdown;
import java.time.LocalDate;
import java.util.List;

import motorph.domain.model.Attendance;

/**
 * Interface defining the payroll contract for all payrollable entities.
 * Specifies required operations: calculating gross salary and computing
 * deductions.
 * Ensures consistent payroll processing across all employee types (Admin, HR,
 * Finance, Employee).
 */
public interface Payable {

        /** Calculates and returns the gross salary for the employee. */
        double getGrossSalary();

        /**
         * Computes deductions (SSS, PhilHealth, Pag-IBIG, tax, late/undertime) for the
         * given month/period.
         */
        PayrollBreakdown computeDeductions(
                        List<Attendance> logs,
                        LocalDate cutoffDate);

}
