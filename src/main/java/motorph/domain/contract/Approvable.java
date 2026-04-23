package motorph.domain.contract;

/**
 * Defines approval actions for a request.
 */
public interface Approvable {

    // Approves the request.
    void approve(int approverAccountId, String remarks);

    // Rejects the request.
    void reject(int approverAccountId, String remarks);
}