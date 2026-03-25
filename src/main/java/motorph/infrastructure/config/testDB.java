package motorph.infrastructure.config;

public class testDB {
    public static void main(String[] args) {
        if (DatabaseConnection.getConnection() != null) {
            System.out.println("Connected to MySQL ✅");
        } else {
            System.out.println("Connection failed ❌");
        }
    }
}