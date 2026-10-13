package motorph.ui.reports;

import motorph.domain.value.Role;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Reports payslip option")
class ReportsControllerTest {

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "HR", "FINANCE"})
    @DisplayName("Admin, HR and Finance are offered the Payslip report")
    void rolesWithCrossEmployeeAccessSeeThePayslipReport(Role role) {
        assertTrue(ReportsController.canViewAllPayslips(role));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"IT", "EMPLOYEE"})
    @DisplayName("IT and Employee are not offered the Payslip report")
    void otherRolesDoNotSeeThePayslipReport(Role role) {
        assertFalse(ReportsController.canViewAllPayslips(role));
    }

    @Test
    @DisplayName("No active role means no Payslip report")
    void noRoleMeansNoPayslipReport() {
        assertFalse(ReportsController.canViewAllPayslips(null));
    }
}
