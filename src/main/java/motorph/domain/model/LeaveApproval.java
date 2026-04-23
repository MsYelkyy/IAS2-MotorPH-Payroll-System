package motorph.domain.model;

import java.time.LocalDateTime;

/**
 * Represents a leave approval record and maps to the leave_approval table in the database.
 */
public class LeaveApproval {

    private Integer leaveApprovalId;
    private int leaveRequestId;
    private int approverAccountId;
    private LocalDateTime approvedAt;
    private String remarks;

    // constructors
    public LeaveApproval(
            Integer leaveApprovalId,
            int leaveRequestId,
            int approverAccountId,
            LocalDateTime approvedAt,
            String remarks) {

        setLeaveApprovalId(leaveApprovalId);
        setLeaveRequestId(leaveRequestId);
        setApproverAccountId(approverAccountId);
        setApprovedAt(approvedAt);
        setRemarks(remarks);
    }

    // getters
    public Integer getLeaveApprovalId() {
        return leaveApprovalId;
    }

    public int getLeaveRequestId() {
        return leaveRequestId;
    }

    public int getApproverAccountId() {
        return approverAccountId;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    // setters
    public void setLeaveApprovalId(Integer leaveApprovalId) {
        if (leaveApprovalId != null && leaveApprovalId <= 0) {
            throw new IllegalArgumentException("Leave approval ID must be positive.");
        }
        this.leaveApprovalId = leaveApprovalId;
    }

    public void setLeaveRequestId(int leaveRequestId) {
        if (leaveRequestId <= 0) {
            throw new IllegalArgumentException("Leave request ID must be positive.");
        }
        this.leaveRequestId = leaveRequestId;
    }

    public void setApproverAccountId(int approverAccountId) {
        if (approverAccountId <= 0) {
            throw new IllegalArgumentException("Approver account ID must be positive.");
        }
        this.approverAccountId = approverAccountId;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void setRemarks(String remarks) {
        this.remarks = clean(remarks);
    }

    // cleans optional text
    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    // returns the leave approval summary
    @Override
    public String toString() {
        return "LeaveApproval{"
                + "leaveApprovalId=" + leaveApprovalId
                + ", leaveRequestId=" + leaveRequestId
                + ", approverAccountId=" + approverAccountId
                + ", approvedAt=" + approvedAt
                + ", remarks='" + remarks + '\''
                + '}';
    }
}