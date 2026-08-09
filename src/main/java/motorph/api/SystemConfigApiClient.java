package motorph.api;

import motorph.api.dto.systemconfig.PagIbigRateDTO;
import motorph.api.dto.systemconfig.PhilHealthRateDTO;
import motorph.api.dto.systemconfig.SssRateDTO;
import motorph.api.dto.systemconfig.WithholdingTaxBracketDTO;

import java.util.List;

/** Thin wrapper around the backend's /api/v1/system-config endpoints. Admin-only. */
public final class SystemConfigApiClient {

    private final HttpApiClient httpApiClient;

    public SystemConfigApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<PhilHealthRateDTO> getAllPhilHealthRates() {
        return httpApiClient.getList("/api/v1/system-config/philhealth-rates", PhilHealthRateDTO.class);
    }

    public void addPhilHealthRate(PhilHealthRateDTO dto) {
        httpApiClient.postNoContent("/api/v1/system-config/philhealth-rates", dto);
    }

    public void updatePhilHealthRate(PhilHealthRateDTO dto) {
        httpApiClient.put("/api/v1/system-config/philhealth-rates", dto, Void.class);
    }

    public List<SssRateDTO> getAllSssRates() {
        return httpApiClient.getList("/api/v1/system-config/sss-rates", SssRateDTO.class);
    }

    public void addSssRate(SssRateDTO dto) {
        httpApiClient.postNoContent("/api/v1/system-config/sss-rates", dto);
    }

    public void updateSssRate(SssRateDTO dto) {
        httpApiClient.put("/api/v1/system-config/sss-rates", dto, Void.class);
    }

    public List<PagIbigRateDTO> getAllPagIbigRates() {
        return httpApiClient.getList("/api/v1/system-config/pagibig-rates", PagIbigRateDTO.class);
    }

    public void addPagIbigRate(PagIbigRateDTO dto) {
        httpApiClient.postNoContent("/api/v1/system-config/pagibig-rates", dto);
    }

    public void updatePagIbigRate(PagIbigRateDTO dto) {
        httpApiClient.put("/api/v1/system-config/pagibig-rates", dto, Void.class);
    }

    public List<WithholdingTaxBracketDTO> getAllTaxBrackets() {
        return httpApiClient.getList("/api/v1/system-config/tax-brackets", WithholdingTaxBracketDTO.class);
    }

    public void addTaxBracket(WithholdingTaxBracketDTO dto) {
        httpApiClient.postNoContent("/api/v1/system-config/tax-brackets", dto);
    }

    public void updateTaxBracket(WithholdingTaxBracketDTO dto) {
        httpApiClient.put("/api/v1/system-config/tax-brackets", dto, Void.class);
    }
}
