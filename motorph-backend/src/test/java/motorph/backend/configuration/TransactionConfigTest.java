package motorph.backend.configuration;

import motorph.backend.repository.UserAccountRepository;
import motorph.backend.service.EmployeeService;
import motorph.backend.service.PayrollService;
import motorph.backend.service.SalaryBenefitService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Repositories open connections with {@code dataSource.getConnection()}; these tests check that, once
 * the data source is wrapped, those connections take part in {@code @Transactional} methods.
 */
class TransactionConfigTest {

    private DataSource pool;
    private Connection physicalConnection;
    private DataSource dataSource;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() throws SQLException {
        pool = mock(DataSource.class);
        physicalConnection = mock(Connection.class);
        when(pool.getConnection()).thenReturn(physicalConnection);
        when(physicalConnection.getAutoCommit()).thenReturn(true);

        dataSource = (DataSource) TransactionConfig.transactionAwareDataSourcePostProcessor()
                .postProcessAfterInitialization(pool, "dataSource");
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void wrapsTheDataSourceOnce() {
        assertThat(dataSource).isInstanceOf(TransactionAwareDataSourceProxy.class);
        assertThat(TransactionConfig.transactionAwareDataSourcePostProcessor()
                .postProcessAfterInitialization(dataSource, "dataSource")).isSameAs(dataSource);
    }

    @Test
    @DisplayName("A failure after two repository writes rolls both back on one connection")
    void failureRollsBackEarlierWrites() throws SQLException {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            write();
            write();
            throw new IllegalStateException("second step failed");
        })).isInstanceOf(IllegalStateException.class);

        verify(pool, times(1)).getConnection();
        verify(physicalConnection, times(2)).prepareStatement("INSERT");
        verify(physicalConnection).rollback();
        verify(physicalConnection, never()).commit();
    }

    @Test
    void successCommitsOnce() throws SQLException {
        transaction.executeWithoutResult(status -> {
            write();
            write();
        });

        verify(pool, times(1)).getConnection();
        verify(physicalConnection).commit();
        verify(physicalConnection, never()).rollback();
    }

    @ParameterizedTest
    @CsvSource({
            "PayrollService, runPayrollForEmployee",
            "PayrollService, generatePayrollForReadyEmployees",
            "EmployeeService, createEmployeeWithSalaryAndBenefits",
            "EmployeeService, updateEmployee",
            "SalaryBenefitService, addSalaryRecord",
            "UserAccountRepository, save"
    })
    void multiStepWritesAreTransactional(String className, String methodName) {
        Class<?> type = switch (className) {
            case "PayrollService" -> PayrollService.class;
            case "EmployeeService" -> EmployeeService.class;
            case "SalaryBenefitService" -> SalaryBenefitService.class;
            default -> UserAccountRepository.class;
        };

        Method method = Arrays.stream(type.getMethods())
                .filter(m -> m.getName().equals(methodName) && !m.isBridge())
                .findFirst()
                .orElseThrow();

        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private void write() {
        try (Connection conn = dataSource.getConnection()) {
            conn.prepareStatement("INSERT");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
