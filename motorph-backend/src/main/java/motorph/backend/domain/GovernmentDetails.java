package motorph.backend.domain;

/**
 * Represents an employee government record and maps to the government_details table in the database.
 */
public class GovernmentDetails {

    private Integer governmentDetailsId;
    private int employeeId;
    private String sssNumber;
    private String tinNumber;
    private String pagibigNumber;
    private String philhealthNumber;

    public GovernmentDetails(Integer governmentDetailsId, int employeeId, String sssNumber,
                             String tinNumber, String pagibigNumber, String philhealthNumber) {
        setGovernmentDetailsId(governmentDetailsId);
        setEmployeeId(employeeId);
        setSssNumber(sssNumber);
        setTinNumber(tinNumber);
        setPagibigNumber(pagibigNumber);
        setPhilhealthNumber(philhealthNumber);
    }

    public Integer getGovernmentDetailsId() {
        return governmentDetailsId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getSssNumber() {
        return sssNumber;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public String getPagibigNumber() {
        return pagibigNumber;
    }

    public String getPhilhealthNumber() {
        return philhealthNumber;
    }

    public void setGovernmentDetailsId(Integer governmentDetailsId) {
        if (governmentDetailsId != null && governmentDetailsId <= 0) {
            throw new IllegalArgumentException("Government details ID must be positive.");
        }
        this.governmentDetailsId = governmentDetailsId;
    }

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.employeeId = employeeId;
    }

    public void setSssNumber(String sssNumber) {
        this.sssNumber = requireValidNumber(sssNumber, "SSS number");
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = requireValidNumber(tinNumber, "TIN number");
    }

    public void setPagibigNumber(String pagibigNumber) {
        this.pagibigNumber = requireValidNumber(pagibigNumber, "Pag-IBIG number");
    }

    public void setPhilhealthNumber(String philhealthNumber) {
        this.philhealthNumber = requireValidNumber(philhealthNumber, "PhilHealth number");
    }

    private String requireValidNumber(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String trimmed = value.trim();
        if (!trimmed.matches("[0-9-]{6,}")) {
            throw new IllegalArgumentException(label + " format is invalid.");
        }
        return trimmed;
    }
}
