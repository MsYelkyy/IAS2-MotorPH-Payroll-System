package motorph.backend.repository;

import motorph.backend.repository.UserAccountRepository.FailedPasswordAttemptResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountRepositoryTest {

    private static final int ACCOUNT_ID = 12;

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement selectStatement;
    private PreparedStatement updateStatement;
    private ResultSet resultSet;
    private UserAccountRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        selectStatement = mock(PreparedStatement.class);
        updateStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        repository = new UserAccountRepository(dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(contains("FOR UPDATE"))).thenReturn(selectStatement);
        when(connection.prepareStatement(contains("UPDATE user_account"))).thenReturn(updateStatement);
        when(selectStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean("is_locked")).thenReturn(false);
        when(updateStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    void recordsAnIncorrectPasswordAgainstItsAccount() throws SQLException {
        when(resultSet.getInt("failed_login_count")).thenReturn(2);

        assertThat(repository.recordFailedPasswordAttempt(ACCOUNT_ID, 5))
                .isEqualTo(FailedPasswordAttemptResult.ATTEMPT_RECORDED);

        verify(updateStatement).setInt(1, 3);
        verify(updateStatement).setBoolean(2, false);
        verify(updateStatement).setInt(3, ACCOUNT_ID);
    }

    @Test
    void appliesLockOnTheThresholdAttempt() throws SQLException {
        when(resultSet.getInt("failed_login_count")).thenReturn(4);

        assertThat(repository.recordFailedPasswordAttempt(ACCOUNT_ID, 5))
                .isEqualTo(FailedPasswordAttemptResult.LOCK_APPLIED);

        verify(updateStatement).setInt(1, 5);
        verify(updateStatement).setBoolean(2, true);
    }

    @Test
    void doesNotIncrementAnAlreadyLockedAccount() throws SQLException {
        when(resultSet.getBoolean("is_locked")).thenReturn(true);

        assertThat(repository.recordFailedPasswordAttempt(ACCOUNT_ID, 5))
                .isEqualTo(FailedPasswordAttemptResult.LOCKED);

        verify(connection, never()).prepareStatement(contains("UPDATE user_account"));
    }

    @Test
    void successfulPasswordCanClearAnAlreadyZeroCounter() throws SQLException {
        when(updateStatement.executeUpdate()).thenReturn(0);

        assertThat(repository.resetFailedPasswordAttempts(ACCOUNT_ID)).isTrue();

        verify(updateStatement).setInt(1, ACCOUNT_ID);
    }

    @Test
    void doesNotResetAnAutomaticallyLockedAccountBeforeItIsUnlocked() throws SQLException {
        when(resultSet.getBoolean("is_locked")).thenReturn(true);

        assertThat(repository.resetFailedPasswordAttempts(ACCOUNT_ID)).isFalse();

        verify(connection, never()).prepareStatement(contains("UPDATE user_account"));
    }

    @Test
    void itUnlockResetsFailureCount() throws SQLException {
        repository.updateLockStatus(ACCOUNT_ID, false);

        verify(updateStatement).setBoolean(1, false);
        verify(updateStatement).setBoolean(2, false);
        verify(updateStatement).setInt(3, ACCOUNT_ID);
    }
}
