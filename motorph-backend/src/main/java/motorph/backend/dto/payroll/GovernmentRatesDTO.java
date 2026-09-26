package motorph.backend.dto.payroll;

import motorph.backend.dto.systemconfig.PagIbigRateDTO;
import motorph.backend.dto.systemconfig.PhilHealthRateDTO;
import motorph.backend.dto.systemconfig.SssRateDTO;
import motorph.backend.dto.systemconfig.WithholdingTaxBracketDTO;

import java.util.List;

/** Carries effective government contribution rates for one payroll computation. */
public class GovernmentRatesDTO {

    private final SssRateDTO sssRate;
    private final PhilHealthRateDTO philHealthRate;
    private final List<PagIbigRateDTO> pagIbigRates;
    private final List<WithholdingTaxBracketDTO> taxBrackets;

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

    public SssRateDTO getSssRate() {
        return sssRate;
    }

    public PhilHealthRateDTO getPhilHealthRate() {
        return philHealthRate;
    }

    public List<PagIbigRateDTO> getPagIbigRates() {
        return pagIbigRates;
    }

    public List<WithholdingTaxBracketDTO> getTaxBrackets() {
        return taxBrackets;
    }
}
