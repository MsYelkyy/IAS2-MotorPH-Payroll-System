package motorph.backend.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Gets the client IP address and User-Agent from the current request for audit logging.
 *
 * <p>The IP address is taken directly from the connection received by the server.
 * X-Forwarded-For is ignored because the backend does not use a trusted proxy and
 * the header can be changed by the client. The User-Agent is recorded only as
 * additional device information and is not used to verify the client's identity.
 */
@Component
public class RequestClientInfo {

    static final int MAX_IP_LENGTH = 45;
    static final int MAX_DEVICE_LENGTH = 255;

     /**Returns the remote address of the current request,
      * or {@code null} outside a request.
    */
     public String getIpAddress() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : limit(sanitize(request.getRemoteAddr()), MAX_IP_LENGTH);
    }

    /**
     * Returns the User-Agent of the current request,
     * or {@code null} if there is none.
     */
    public String getDeviceInfo() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : limit(sanitize(request.getHeader(HttpHeaders.USER_AGENT)), MAX_DEVICE_LENGTH);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }

        return null;
    }

    // Control characters are replaced so a value cannot break or fake log lines.
    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.replaceAll("\\p{Cntrl}", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
