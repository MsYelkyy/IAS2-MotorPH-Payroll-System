package motorph.api.dto.leave;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the backend's LeaveCreditRowDTO for the admin/self credits panel. */
public class LeaveCreditRowDTO {

    private final int leaveTypeId;
    private final String leaveTypeName;
    private final double totalCredits;
    private final double usedCredits;
    private final double remainingCredits;
    private final int year;

    @JsonCreator
    public LeaveCreditRowDTO(@JsonProperty("leaveTypeId") int leaveTypeId,
                             @JsonProperty("leaveTypeName") String leaveTypeName,
                             @JsonProperty("totalCredits") double totalCredits,
                             @JsonProperty("usedCredits") double usedCredits,
                             @JsonProperty("remainingCredits") double remainingCredits,
                             @JsonProperty("year") int year) {
        this.leaveTypeId = leaveTypeId;
        this.leaveTypeName = leaveTypeName;
        this.totalCredits = totalCredits;
        this.usedCredits = usedCredits;
        this.remainingCredits = remainingCredits;
        this.year = year;
    }

    public int    getLeaveTypeId()       { return leaveTypeId; }
    public String getLeaveTypeName()     { return leaveTypeName; }
    public double getTotalCredits()      { return totalCredits; }
    public double getUsedCredits()       { return usedCredits; }
    public double getRemainingCredits()  { return remainingCredits; }
    public int    getYear()              { return year; }
}
