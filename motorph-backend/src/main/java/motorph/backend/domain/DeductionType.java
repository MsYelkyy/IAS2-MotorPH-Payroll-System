package motorph.backend.domain;

/** Represents a deduction type resolved from database configuration. */
public class DeductionType {

    private final int deductionTypeId;
    private final String code;
    private final boolean taxable;

    // constructors
    public DeductionType(int deductionTypeId, String code, boolean taxable) {
        if (deductionTypeId <= 0) {
            throw new IllegalArgumentException("Deduction type ID must be positive.");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Deduction type code is required.");
        }

        this.deductionTypeId = deductionTypeId;
        this.code = code;
        this.taxable = taxable;
    }

    // getters
    public int getDeductionTypeId() {
        return deductionTypeId;
    }

    public String getCode() {
        return code;
    }

    public boolean isTaxable() {
        return taxable;
    }
}
