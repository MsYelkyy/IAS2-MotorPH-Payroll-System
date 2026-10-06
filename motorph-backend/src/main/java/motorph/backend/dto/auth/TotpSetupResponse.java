package motorph.backend.dto.auth;

/**
 * Response used during authenticator setup.
 * It includes the QR code and a manual setup key.
 * This is only returned before setup is confirmed.
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

    @Override
    public String toString() {
        return "TotpSetupResponse[accountLabel=" + accountLabel + ", secret=***]";
    }
}
