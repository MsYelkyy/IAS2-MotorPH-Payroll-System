package motorph.api.dto.employee;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire copy of the backend's GovernmentDetails domain model, mirroring the
 * desktop's original motorph.domain.model.GovernmentDetails getter names.
 */
public class GovernmentDetailsDTO {

    private final Integer governmentDetailsId;
    private final int employeeId;
    private final String sssNumber;
    private final String tinNumber;
    private final String pagibigNumber;
    private final String philhealthNumber;

    @JsonCreator
    public GovernmentDetailsDTO(@JsonProperty("governmentDetailsId") Integer governmentDetailsId,
                                @JsonProperty("employeeId") int employeeId,
                                @JsonProperty("sssNumber") String sssNumber,
                                @JsonProperty("tinNumber") String tinNumber,
                                @JsonProperty("pagibigNumber") String pagibigNumber,
                                @JsonProperty("philhealthNumber") String philhealthNumber) {
        this.governmentDetailsId = governmentDetailsId;
        this.employeeId = employeeId;
        this.sssNumber = sssNumber;
        this.tinNumber = tinNumber;
        this.pagibigNumber = pagibigNumber;
        this.philhealthNumber = philhealthNumber;
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
}
