package motorph.backend.repository;

import motorph.backend.domain.PayrollResult;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayrollResultRepositoryTest {

    private PreparedStatement statement;
    private PayrollResultRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), anyInt()))
                .thenReturn(statement);

        repository = new PayrollResultRepository(dataSource);
    }

    @Test
    void saveMapsDuplicateKeyToBusinessRuleException()
            throws SQLException {

        SQLException duplicateKey = new SQLException(
                "Duplicate entry for key " +
                "'uq_payroll_result_employee_payrun'",
                "23000",
                1062
        );

        when(statement.executeUpdate()).thenThrow(duplicateKey);

        assertThatThrownBy(() -> repository.save(payrollResult()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(
                        "Payroll already generated for this employee and payrun."
                )
                .hasCause(duplicateKey);
    }

    @Test
    void saveKeepsOtherSqlFailuresAsDataAccessException()
            throws SQLException {

        SQLException databaseFailure = new SQLException(
                "Database unavailable",
                "08001",
                0
        );

        when(statement.executeUpdate()).thenThrow(databaseFailure);

        assertThatThrownBy(() -> repository.save(payrollResult()))
                .isInstanceOf(DataAccessException.class)
                .hasMessage("Failed to save payroll result.")
                .hasCause(databaseFailure);
    }

    private PayrollResult payrollResult() {
        return new PayrollResult(
                null,
                10001,
                24,
                80.0,
                30000.0,
                5000.0,
                25000.0,
                LocalDateTime.of(2026, 10, 12, 6, 0)
        );
    }
}