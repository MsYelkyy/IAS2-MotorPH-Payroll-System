package motorph.domain.model;


import motorph.domain.model.LeaveCredit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests leave credit balance rules and validation.
 */
@DisplayName("LeaveCredit Tests")
class LeaveCreditTest {


    // creates leave credit with valid data
    @Test
    @DisplayName("constructor should create leave credit with valid data")
    void constructorShouldCreateLeaveCreditWithValidData() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 8, 2026);


        assertEquals(10001, leaveCredit.getEmployeeId());
        assertEquals(40, leaveCredit.getTotalCredits(), 0.01);
        assertEquals(8, leaveCredit.getUsedCredits(), 0.01);
        assertEquals(32, leaveCredit.getRemainingCredits(), 0.01);
    }


    // calculates remaining credits
    @Test
    @DisplayName("getRemainingCredits should return total minus used credits")
    void getRemainingCreditsShouldReturnTotalMinusUsedCredits() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 12, 2026);


        assertEquals(28, leaveCredit.getRemainingCredits(), 0.01);
    }


    // checks remaining credits
    @Test
    @DisplayName("hasRemainingCredits should return true when balance exists")
    void hasRemainingCreditsShouldReturnTrueWhenBalanceExists() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 39, 2026);


        assertTrue(leaveCredit.hasRemainingCredits());
    }


    // checks no remaining credits
    @Test
    @DisplayName("hasRemainingCredits should return false when balance is zero")
    void hasRemainingCreditsShouldReturnFalseWhenBalanceIsZero() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 40, 2026);


        assertFalse(leaveCredit.hasRemainingCredits());
    }


    // confirms leave request can be covered
    @Test
    @DisplayName("canCover should return true when requested hours are within balance")
    void canCoverShouldReturnTrueWhenRequestedHoursAreWithinBalance() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 8, 2026);


        assertTrue(leaveCredit.canCover(32));
    }


    // rejects leave request beyond balance
    @Test
    @DisplayName("canCover should return false when requested hours exceed balance")
    void canCoverShouldReturnFalseWhenRequestedHoursExceedBalance() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 8, 2026);


        assertFalse(leaveCredit.canCover(33));
    }


    // updates used credits
    @Test
    @DisplayName("useCredits should add used hours and reduce remaining balance")
    void useCreditsShouldAddUsedHoursAndReduceRemainingBalance() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 8, 2026);


        leaveCredit.useCredits(8);


        assertEquals(16, leaveCredit.getUsedCredits(), 0.01);
        assertEquals(24, leaveCredit.getRemainingCredits(), 0.01);
    }


    // rejects use credits beyond remaining balance
    @Test
    @DisplayName("useCredits should reject hours greater than remaining credits")
    void useCreditsShouldRejectHoursGreaterThanRemainingCredits() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 35, 2026);


        assertThrows(IllegalArgumentException.class,
                () -> leaveCredit.useCredits(6));
    }


    // rejects negative requested hours
    @Test
    @DisplayName("canCover should reject negative requested hours")
    void canCoverShouldRejectNegativeRequestedHours() {
        LeaveCredit leaveCredit = createLeaveCredit(1, 10001, 1, 40, 8, 2026);


        assertThrows(IllegalArgumentException.class,
                () -> leaveCredit.canCover(-1));
    }


    // rejects used credits greater than total credits
    @Test
    @DisplayName("constructor should reject used credits greater than total credits")
    void constructorShouldRejectUsedCreditsGreaterThanTotalCredits() {
        assertThrows(IllegalArgumentException.class,
                () -> createLeaveCredit(1, 10001, 1, 40, 41, 2026));
    }


    // rejects invalid employee id
    @Test
    @DisplayName("constructor should reject invalid employee id")
    void constructorShouldRejectInvalidEmployeeId() {
        assertThrows(IllegalArgumentException.class,
                () -> createLeaveCredit(1, 0, 1, 40, 8, 2026));
    }


    // rejects negative total credits
    @Test
    @DisplayName("constructor should reject negative total credits")
    void constructorShouldRejectNegativeTotalCredits() {
        assertThrows(IllegalArgumentException.class,
                () -> createLeaveCredit(1, 10001, 1, -1, 0, 2026));
    }


    // creates leave credit test data
    private LeaveCredit createLeaveCredit(Integer leaveCreditId,
                                          int employeeId,
                                          int leaveTypeId,
                                          double totalCredits,
                                          double usedCredits,
                                          int year) {
        return new LeaveCredit(
                leaveCreditId,
                employeeId,
                leaveTypeId,
                totalCredits,
                usedCredits,
                year
        );
    }
}
