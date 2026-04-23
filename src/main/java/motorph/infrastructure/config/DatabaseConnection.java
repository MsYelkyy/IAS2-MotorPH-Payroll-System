package motorph.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides database connections for the application.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/motorph_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root123";

    // returns a database connection
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to the database.", e);
        }
    }
}