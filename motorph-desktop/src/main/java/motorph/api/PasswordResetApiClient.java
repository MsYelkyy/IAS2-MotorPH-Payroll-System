package motorph.api;

import motorph.api.dto.auth.CredentialsPreview;
import motorph.api.dto.user.PasswordResetCountsDTO;
import motorph.api.dto.user.PasswordResetRequestRowDTO;
import motorph.domain.value.PasswordResetStatus;

import java.util.List;

/**
 * Thin wrapper around the backend's /api/v1/password-reset-requests
 * endpoints. Process/cancel never send an acting employee - the backend
 * resolves the resolver from the bearer token.
 */
public final class PasswordResetApiClient {

    private static final String BASE_PATH = "/api/v1/password-reset-requests";

    private final HttpApiClient httpApiClient;

    public PasswordResetApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<PasswordResetRequestRowDTO> getRequests() {
        return httpApiClient.getList(BASE_PATH, PasswordResetRequestRowDTO.class);
    }

    public List<PasswordResetRequestRowDTO> getRequestsByStatus(PasswordResetStatus status) {
        return httpApiClient.getList(BASE_PATH + "?status=" + status.name(), PasswordResetRequestRowDTO.class);
    }

    public PasswordResetCountsDTO getCounts() {
        return httpApiClient.get(BASE_PATH + "/counts", PasswordResetCountsDTO.class);
    }

    /** Returns the dev-only delivery preview, or null once real mail delivery exists. */
    public CredentialsPreview processRequest(int resetRequestId) {
        return httpApiClient.post(BASE_PATH + "/" + resetRequestId + "/process", null, CredentialsPreview.class);
    }

    public void cancelRequest(int resetRequestId) {
        httpApiClient.postNoContent(BASE_PATH + "/" + resetRequestId + "/cancel", null);
    }
}
