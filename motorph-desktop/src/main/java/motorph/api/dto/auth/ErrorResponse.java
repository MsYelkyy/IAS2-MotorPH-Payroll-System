package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Mirrors the backend's stable error contract, including an optional authentication reason. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(String code, String message, List<String> fieldErrors, String reason) {

    @JsonCreator
    public ErrorResponse(@JsonProperty("code") String code,
                         @JsonProperty("message") String message,
                         @JsonProperty("fieldErrors") List<String> fieldErrors,
                         @JsonProperty("reason") String reason) {
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.reason = reason;
    }
}
