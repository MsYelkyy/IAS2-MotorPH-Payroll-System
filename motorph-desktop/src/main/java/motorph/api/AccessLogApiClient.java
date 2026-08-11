package motorph.api;

import motorph.api.dto.accesslog.AccessLogCountsDTO;
import motorph.api.dto.accesslog.UserAccessLogDTO;

import java.util.List;

/** Thin wrapper around the backend's read-only /api/v1/access-logs endpoints. */
public final class AccessLogApiClient {

    private static final String BASE_PATH = "/api/v1/access-logs";

    private final HttpApiClient httpApiClient;

    public AccessLogApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public List<UserAccessLogDTO> getAllLogs() {
        return httpApiClient.getList(BASE_PATH, UserAccessLogDTO.class);
    }

    public List<UserAccessLogDTO> getRecent(int limit) {
        return httpApiClient.getList(BASE_PATH + "/recent?limit=" + limit, UserAccessLogDTO.class);
    }

    public AccessLogCountsDTO getCounts() {
        return httpApiClient.get(BASE_PATH + "/counts", AccessLogCountsDTO.class);
    }
}
