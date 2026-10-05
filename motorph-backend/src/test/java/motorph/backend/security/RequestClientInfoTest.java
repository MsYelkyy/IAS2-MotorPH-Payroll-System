package motorph.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks how the client IP and User-Agent are read for the audit log (Control 5.3).
 */
class RequestClientInfoTest {

    private final RequestClientInfo clientInfo = new RequestClientInfo();

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void readsRemoteAddressAndUserAgent() {
        MockHttpServletRequest request = request("192.168.1.25");
        request.addHeader("User-Agent", "MotorPH-Desktop/1.0 (Windows 11 10.0; amd64; Java 21)");
        bind(request);

        assertThat(clientInfo.getIpAddress()).isEqualTo("192.168.1.25");
        assertThat(clientInfo.getDeviceInfo()).isEqualTo("MotorPH-Desktop/1.0 (Windows 11 10.0; amd64; Java 21)");
    }

    @Test
    void ignoresXForwardedFor() {
        MockHttpServletRequest request = request("192.168.1.25");
        request.addHeader("X-Forwarded-For", "8.8.8.8");
        request.addHeader("X-Real-IP", "8.8.4.4");
        bind(request);

        assertThat(clientInfo.getIpAddress()).isEqualTo("192.168.1.25");
    }

    @Test
    void replacesControlCharactersInUserAgent() {
        MockHttpServletRequest request = request("10.0.0.1");
        request.addHeader("User-Agent", "Evil\r\nLOGIN_SUCCESS admin\tx");
        bind(request);

        assertThat(clientInfo.getDeviceInfo())
                .isEqualTo("Evil  LOGIN_SUCCESS admin x")
                .doesNotContain("\r", "\n", "\t");
    }

    @Test
    void cutsLongUserAgentToColumnSize() {
        MockHttpServletRequest request = request("10.0.0.1");
        request.addHeader("User-Agent", "a".repeat(400));
        bind(request);

        assertThat(clientInfo.getDeviceInfo()).hasSize(RequestClientInfo.MAX_DEVICE_LENGTH);
    }

    @Test
    void missingUserAgentIsNull() {
        bind(request("10.0.0.1"));

        assertThat(clientInfo.getDeviceInfo()).isNull();
    }

    @Test
    void blankUserAgentIsNull() {
        MockHttpServletRequest request = request("10.0.0.1");
        request.addHeader("User-Agent", "   ");
        bind(request);

        assertThat(clientInfo.getDeviceInfo()).isNull();
    }

    @Test
    void outsideARequestBothAreNull() {
        assertThat(clientInfo.getIpAddress()).isNull();
        assertThat(clientInfo.getDeviceInfo()).isNull();
    }

    @Test
    void readsIpv6Address() {
        bind(request("0:0:0:0:0:0:0:1"));

        assertThat(clientInfo.getIpAddress()).isEqualTo("0:0:0:0:0:0:0:1");
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private void bind(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
