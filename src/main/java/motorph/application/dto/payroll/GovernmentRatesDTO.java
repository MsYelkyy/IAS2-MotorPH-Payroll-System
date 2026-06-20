package motorph.application.dto.payroll;

import motorph.application.dto.systemconfig.PagIbigRateDTO;
import motorph.application.dto.systemconfig.PhilHealthRateDTO;
import motorph.application.dto.systemconfig.SssRateDTO;
import motorph.application.dto.systemconfig.WithholdingTaxBracketDTO;

import java.util.List;

/** Carries effective government contribution rates for one payroll computation. */
public class GovernmentRatesDTO {

    private final SssRateDTO sssRate;
    private final PhilHealthRateDTO philHealthRate;
    private final List<PagIbigRateDTO> pagIbigRates;
    private final List<WithholdingTaxBracketDTO> taxBrackets;

    // constructors
    public GovernmentRatesDTO(
            SssRateDTO sssRate,
            PhilHealthRateDTO philHealthRate,
            List<PagIbigRateDTO> pagIbigRates,
            List<WithholdingTaxBracketDTO> taxBrackets) {

        this.sssRate = sssRate;
        this.philHealthRate = philHealthRate;
        this.pagIbigRates = pagIbigRates;
        this.taxBrackets = taxBrackets;
    }

    // getters/setters
    public SssRateDTO getSssRate() {
        return sssRate;
    }

    // getters/setters
    public PhilHealthRateDTO getPhilHealthRate() {
        return philHealthRate;
    }

    // getters/setters
    public List<PagIbigRateDTO> getPagIbigRates() {
        return pagIbigRates;
    }

    // getters/setters
    public List<WithholdingTaxBracketDTO> getTaxBrackets() {
        return taxBrackets;
    }
}
