package motorph.domain.model;

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

    // constructors
    public GovernmentDetails(
            Integer governmentDetailsId,
            int employeeId,
            String sssNumber,
            String tinNumber,
            String pagibigNumber,
            String philhealthNumber) {

        setGovernmentDetailsId(governmentDetailsId);
        setEmployeeId(employeeId);
        setSssNumber(sssNumber);
        setTinNumber(tinNumber);
        setPagibigNumber(pagibigNumber);
        setPhilhealthNumber(philhealthNumber);
    }

    // getters
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

    // setters
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
        this.sssNumber = requireNonBlank(sssNumber, "SSS number is required.");
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = requireNonBlank(tinNumber, "TIN number is required.");
    }

    public void setPagibigNumber(String pagibigNumber) {
        this.pagibigNumber = requireNonBlank(pagibigNumber, "Pag-IBIG number is required.");
    }

    public void setPhilhealthNumber(String philhealthNumber) {
        this.philhealthNumber = requireNonBlank(philhealthNumber, "PhilHealth number is required.");
    }

    // validates required text
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // returns the government details summary
    @Override
    public String toString() {
        return "GovernmentDetails{"
                + "governmentDetailsId=" + governmentDetailsId
                + ", employeeId=" + employeeId
                + ", sssNumber='" + sssNumber + '\''
                + ", tinNumber='" + tinNumber + '\''
                + ", pagibigNumber='" + pagibigNumber + '\''
                + ", philhealthNumber='" + philhealthNumber + '\''
                + '}';
    }
}