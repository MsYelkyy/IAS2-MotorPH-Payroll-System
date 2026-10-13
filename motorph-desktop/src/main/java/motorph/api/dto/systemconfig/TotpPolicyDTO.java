package motorph.api.dto.systemconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the authenticator-code policy the backend enforces. */
public record TotpPolicyDTO(
        int codeLength,
        int periodSeconds,
        int maxAttempts
) {

    @JsonCreator
    public TotpPolicyDTO(@JsonProperty("codeLength") int codeLength,
                         @JsonProperty("periodSeconds") int periodSeconds,
                         @JsonProperty("maxAttempts") int maxAttempts) {
        this.codeLength = codeLength;
        this.periodSeconds = periodSeconds;
        this.maxAttempts = maxAttempts;
    }
}
