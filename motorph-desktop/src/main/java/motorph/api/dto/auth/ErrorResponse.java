package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Mirrors the backend's stable {code, message, fieldErrors} error contract. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(String code, String message, List<String> fieldErrors) {

    @JsonCreator
    public ErrorResponse(@JsonProperty("code") String code,
                         @JsonProperty("message") String message,
                         @JsonProperty("fieldErrors") List<String> fieldErrors) {
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }
}
