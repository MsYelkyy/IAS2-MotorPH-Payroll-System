package motorph.domain.value;

/**
 * Enumeration defining the different user roles in the MotorPH payroll system.
 * Each role has specific permissions and features:
 * - ADMIN: Payroll administration and oversight
 * - HR: Employee management, attendance, and leave approvals
 * - IT: System configuration, user management, and security
 * - EMPLOYEE: View personal profile, attendance, and payslips
 * - FINANCE: Payroll processing and report generation
 */
public enum Role {
    ADMIN,
    HR,
    IT,
    EMPLOYEE,
    FINANCE,
}
