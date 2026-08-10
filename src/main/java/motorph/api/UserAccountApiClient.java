package motorph.api;

import motorph.api.dto.user.AccountCountsDTO;
import motorph.api.dto.user.UserAccountRowDTO;
import motorph.api.dto.user.UserRolesUpdateRequest;
import motorph.domain.value.Role;

import java.util.List;

/**
 * Thin wrapper around the backend's /api/v1/user-accounts endpoints. Status
 * changes never send an acting user - the backend resolves the actor from the
 * bearer token for auditing.
 */
public final class UserAccountApiClient {

    private static final String BASE_PATH = "/api/v1/user-accounts";

    private final HttpApiClient httpApiClient;

    public UserAccountApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<UserAccountRowDTO> getUserAccounts() {
        return httpApiClient.getList(BASE_PATH, UserAccountRowDTO.class);
    }

    public AccountCountsDTO getCounts() {
        return httpApiClient.get(BASE_PATH + "/counts", AccountCountsDTO.class);
    }

    public void lockAccount(int userAccountId) {
        httpApiClient.postNoContent(BASE_PATH + "/" + userAccountId + "/lock", null);
    }

    public void unlockAccount(int userAccountId) {
        httpApiClient.postNoContent(BASE_PATH + "/" + userAccountId + "/unlock", null);
    }

    public void enableAccount(int userAccountId) {
        httpApiClient.postNoContent(BASE_PATH + "/" + userAccountId + "/enable", null);
    }

    public void disableAccount(int userAccountId) {
        httpApiClient.postNoContent(BASE_PATH + "/" + userAccountId + "/disable", null);
    }

    public void updateRoles(int userAccountId, List<Role> roles) {
        List<String> roleNames = roles.stream().map(Role::name).toList();
        httpApiClient.put(BASE_PATH + "/" + userAccountId + "/roles",
                new UserRolesUpdateRequest(roleNames), Void.class);
    }
}
