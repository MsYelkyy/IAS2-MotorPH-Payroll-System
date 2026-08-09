package motorph.backend.dto.leave;

/**
 * Carries a leave credit summary row for one leave type, for display in the admin credits panel.
 */
public class LeaveCreditRowDTO {

    private final int leaveTypeId;
    private final String leaveTypeName;
    private final double totalCredits;
    private final double usedCredits;
    private final double remainingCredits;
    private final int year;

    public LeaveCreditRowDTO(
            int leaveTypeId,
            String leaveTypeName,
            double totalCredits,
            double usedCredits,
            double remainingCredits,
            int year) {

        this.leaveTypeId      = leaveTypeId;
        this.leaveTypeName    = leaveTypeName;
        this.totalCredits     = totalCredits;
        this.usedCredits      = usedCredits;
        this.remainingCredits = remainingCredits;
        this.year             = year;
    }

    public int    getLeaveTypeId()       { return leaveTypeId; }
    public String getLeaveTypeName()     { return leaveTypeName; }
    public double getTotalCredits()      { return totalCredits; }
    public double getUsedCredits()       { return usedCredits; }
    public double getRemainingCredits()  { return remainingCredits; }
    public int    getYear()              { return year; }
}
