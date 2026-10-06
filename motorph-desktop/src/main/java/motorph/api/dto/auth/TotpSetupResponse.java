package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The one response that carries an authenticator secret: a Base64 PNG QR code
 * and the same secret as a manual-entry key. The setup screen shows it and
 * drops it as soon as enrolment ends.
 */
public record TotpSetupResponse(
        String qrCodePngBase64,
        String manualEntryKey,
        String issuer,
        String accountLabel,
        int digits,
        int periodSeconds,
        long expiresInSeconds
) {

    @JsonCreator
    public TotpSetupResponse(@JsonProperty("qrCodePngBase64") String qrCodePngBase64,
                             @JsonProperty("manualEntryKey") String manualEntryKey,
                             @JsonProperty("issuer") String issuer,
                             @JsonProperty("accountLabel") String accountLabel,
                             @JsonProperty("digits") int digits,
                             @JsonProperty("periodSeconds") int periodSeconds,
                             @JsonProperty("expiresInSeconds") long expiresInSeconds) {
        this.qrCodePngBase64 = qrCodePngBase64;
        this.manualEntryKey = manualEntryKey;
        this.issuer = issuer;
        this.accountLabel = accountLabel;
        this.digits = digits;
        this.periodSeconds = periodSeconds;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Override
    public String toString() {
        return "TotpSetupResponse[accountLabel=" + accountLabel + ", secret=***]";
    }
}
