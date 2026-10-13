package motorph.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeRepositoryTest {

    private Connection connection;
    private EmployeeRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        repository = new EmployeeRepository(dataSource);
    }

    private String lastSql() throws SQLException {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        return sql.getValue().replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("The employee list is ordered by employee ID in the query, so every dropdown starts with the lowest ID")
    void allEmployeesAreOrderedByEmployeeId() throws SQLException {
        repository.findAllWithInactive();

        assertThat(lastSql()).endsWith("FROM employee ORDER BY employee_id");
    }

    @Test
    @DisplayName("The active employee list is ordered by employee ID in the query")
    void activeEmployeesAreOrderedByEmployeeId() throws SQLException {
        repository.findAllActive();

        assertThat(lastSql()).endsWith("WHERE is_active = 1 ORDER BY employee_id");
    }
}
