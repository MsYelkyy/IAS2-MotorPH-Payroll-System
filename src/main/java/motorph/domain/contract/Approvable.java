package motorph.domain.contract;

public interface Approvable {
    void approve(String reviewedBy);

    void reject(String reviewedBy);
}
