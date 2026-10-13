package motorph.ui.finance;

import motorph.api.ApiException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A failed payslip search must say what actually failed, so a permission error is never shown as
 * a missing payslip.
 */
@DisplayName("Finance payslip search messages")
class FinanceViewPayslipControllerTest {

    @Test
    @DisplayName("A 404 is reported as a missing payslip")
    void notFoundMeansNoPayslip() {
        ApiException notFound = new ApiException(404, "NOT_FOUND", "Payslip not found.");

        assertEquals("No persisted payslip exists for this employee and payrun.",
                FinanceViewPayslipController.searchFailureMessage(notFound));
    }

    @Test
    @DisplayName("A 403 shows only the permission message, never the missing-payslip text")
    void forbiddenShowsOnlyThePermissionMessage() {
        ApiException forbidden = new ApiException(403, "FORBIDDEN", "You do not have permission to perform this action.");

        assertEquals("You do not have permission to perform this action.",
                FinanceViewPayslipController.searchFailureMessage(forbidden));
    }

    @Test
    @DisplayName("Other failures keep their own message")
    void otherFailuresKeepTheirMessage() {
        ApiException server = new ApiException(500, "INTERNAL_ERROR", "An internal error occurred.");

        assertEquals("An internal error occurred.", FinanceViewPayslipController.searchFailureMessage(server));
    }
}
