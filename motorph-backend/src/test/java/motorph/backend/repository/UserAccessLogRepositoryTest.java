package motorph.backend.repository;

import motorph.backend.domain.UserAccessLog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks how audit log rows are written: the database sets logged_at (Control 5.2), and IP, device,
 * and target are stored (Control 5.3).
 */
class UserAccessLogRepositoryTest {

    private Connection connection;
    private PreparedStatement statement;
    private UserAccessLogRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        repository = new UserAccessLogRepository(dataSource);
    }

    @Test
    void saveDoesNotSendLoggedAt() throws SQLException {
        repository.save(new UserAccessLog(7, "LOGIN_SUCCESS", "ok", null, null, null, null));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());

        assertThat(sql.getValue()).doesNotContain("logged_at");
        assertThat(sql.getValue()).contains("VALUES (?, ?, ?, ?, ?, ?, ?)");
        verify(statement, never()).setTimestamp(anyInt(), any());
        verify(statement, never()).setObject(anyInt(), any());
        verify(statement).executeUpdate();
    }

    @Test
    void saveBindsIpDeviceAndTarget() throws SQLException {
        repository.save(new UserAccessLog(7, "EMPLOYEE_UPDATED", "changed", "10.0.0.1",
                "MotorPH-Desktop/1.0 (Windows 11 10.0; amd64; Java 21)", "EMPLOYEE", "5"));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());

        assertThat(sql.getValue()).contains("ip_address", "device_info", "target_type", "target_id");
        verify(statement).setInt(1, 7);
        verify(statement).setString(2, "EMPLOYEE_UPDATED");
        verify(statement).setString(3, "changed");
        verify(statement).setString(4, "10.0.0.1");
        verify(statement).setString(5, "MotorPH-Desktop/1.0 (Windows 11 10.0; amd64; Java 21)");
        verify(statement).setString(6, "EMPLOYEE");
        verify(statement).setString(7, "5");
    }

    @Test
    void blankValuesAreStoredAsNull() throws SQLException {
        repository.save(new UserAccessLog(null, "LOGIN_FAILED", "bad password", "", "  ", null, null));

        verify(statement).setNull(1, Types.INTEGER);
        verify(statement).setString(4, null);
        verify(statement).setString(5, null);
        verify(statement).setString(6, null);
        verify(statement).setString(7, null);
    }

    @Test
    void newLogHasNoTimestampUntilSaved() {
        UserAccessLog log = new UserAccessLog(7, "LOGOUT", "User logged out.", null, null, null, null);

        assertThat(log.getLoggedAt()).isNull();
    }

    @Test
    void savedLogStillRequiresTimestamp() {
        assertThatThrownBy(() -> new UserAccessLog(1, 7, "LOGOUT", null, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);

        LocalDateTime loggedAt = LocalDateTime.of(2026, 10, 1, 9, 30, 0, 123_000_000);
        UserAccessLog log = new UserAccessLog(1, 7, "LOGOUT", null, null, null, loggedAt, "USER_ACCOUNT", "7");

        assertThat(log.getLoggedAt()).isEqualTo(loggedAt);
        assertThat(log.getTargetType()).isEqualTo("USER_ACCOUNT");
        assertThat(log.getTargetId()).isEqualTo("7");
    }

    @Test
    void targetIdNeedsTargetType() {
        assertThatThrownBy(() -> new UserAccessLog(7, "EMPLOYEE_UPDATED", "x", null, null, null, "5"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
