package motorph.infrastructure.repository.report;

import motorph.application.exception.DataAccessException;

import motorph.application.dto.report.ticket.TicketReportDTO;
import motorph.infrastructure.config.DatabaseConnection;


import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 * Retrieves ticket report data from the database.
 */
public class TicketReportRepository {


    // finds ticket report data using optional filters
    public List<TicketReportDTO> findTicketReport(Integer departmentId,
                                                  Integer employeeId,
                                                  String category,
                                                  String routingTeam,
                                                  String status,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {


        String sql = """
               SELECT
                  st.ticket_id,
                  st.employee_id,
                  CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                  d.department_name,
                  st.category,
                  st.routing_team,
                  st.subject,
                  st.status,
                  CONCAT(resolver.first_name, ' ', resolver.last_name) AS resolved_by,
                  st.resolved_at,
                  st.created_at
               FROM support_ticket st
               JOIN employee e
                  ON st.employee_id = e.employee_id
               JOIN department d
                  ON e.department_id = d.department_id
               LEFT JOIN user_account ua
                  ON st.resolved_by_user_account_id = ua.user_account_id
               LEFT JOIN employee resolver
                  ON ua.employee_id = resolver.employee_id
               WHERE (? IS NULL OR e.department_id = ?)
                AND (? IS NULL OR st.employee_id = ?)
                AND (? IS NULL OR st.category = ?)
                AND (? IS NULL OR st.routing_team = ?)
                AND (? IS NULL OR st.status = ?)
                AND (? IS NULL OR DATE(st.created_at) >= ?)
                AND (? IS NULL OR DATE(st.created_at) <= ?)
               ORDER BY st.created_at DESC
               """;


        List<TicketReportDTO> tickets = new ArrayList<>();


        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {


            statement.setObject(1, departmentId);
            statement.setObject(2, departmentId);


            statement.setObject(3, employeeId);
            statement.setObject(4, employeeId);


            statement.setString(5, clean(category));
            statement.setString(6, clean(category));


            statement.setString(7, clean(routingTeam));
            statement.setString(8, clean(routingTeam));


            statement.setString(9, clean(status));
            statement.setString(10, clean(status));


            statement.setDate(11, toSqlDate(startDate));
            statement.setDate(12, toSqlDate(startDate));


            statement.setDate(13, toSqlDate(endDate));
            statement.setDate(14, toSqlDate(endDate));


            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tickets.add(mapToDTO(resultSet));
                }
            }


        } catch (Exception e) {
            throw new DataAccessException("Failed to retrieve ticket report data.", e);
        }


        return tickets;
    }


    // maps one result row to DTO
    private TicketReportDTO mapToDTO(ResultSet resultSet) throws Exception {
        return new TicketReportDTO(
                resultSet.getInt("ticket_id"),
                resultSet.getInt("employee_id"),
                resultSet.getString("employee_name"),
                resultSet.getString("department_name"),
                resultSet.getString("category"),
                resultSet.getString("routing_team"),
                resultSet.getString("subject"),
                resultSet.getString("status"),
                resultSet.getString("resolved_by"),
                resultSet.getTimestamp("resolved_at") == null
                        ? null
                        : resultSet.getTimestamp("resolved_at").toLocalDateTime(),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }


    // converts local date to sql date
    private Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }


    // cleans optional text filters
    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}



