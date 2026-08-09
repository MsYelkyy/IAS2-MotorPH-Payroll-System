package motorph.backend.domain.contract;

/**
 * Defines approval actions for a request.
 */
public interface Approvable {

    void approve(int approverAccountId, String remarks);

    void reject(int approverAccountId, String remarks);
}
