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
 * Checks that audit log timestamps are left to the database (Control 5.2).
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
        repository.save(new UserAccessLog(7, "LOGIN_SUCCESS", "ok", "", ""));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());

        assertThat(sql.getValue()).doesNotContain("logged_at");
        assertThat(sql.getValue()).contains("VALUES (?, ?, ?, ?, ?)");
        verify(statement, never()).setTimestamp(anyInt(), any());
        verify(statement, never()).setObject(anyInt(), any());
        verify(statement).executeUpdate();
    }

    @Test
    void saveBindsTheOtherFiveColumns() throws SQLException {
        repository.save(new UserAccessLog(null, "LOGIN_FAILED", "bad password", "10.0.0.1", "desktop"));

        verify(statement).setNull(1, Types.INTEGER);
        verify(statement).setString(2, "LOGIN_FAILED");
        verify(statement).setString(3, "bad password");
        verify(statement).setString(4, "10.0.0.1");
        verify(statement).setString(5, "desktop");
    }

    @Test
    void newLogHasNoTimestampUntilSaved() {
        UserAccessLog log = new UserAccessLog(7, "LOGOUT", "User logged out.", "", "");

        assertThat(log.getLoggedAt()).isNull();
    }

    @Test
    void savedLogStillRequiresTimestamp() {
        assertThatThrownBy(() -> new UserAccessLog(1, 7, "LOGOUT", null, null, null, null))
                .isInstanceOf(NullPointerException.class);

        LocalDateTime loggedAt = LocalDateTime.of(2026, 10, 1, 9, 30, 0, 123_000_000);
        assertThat(new UserAccessLog(1, 7, "LOGOUT", null, null, null, loggedAt).getLoggedAt())
                .isEqualTo(loggedAt);
    }
}
