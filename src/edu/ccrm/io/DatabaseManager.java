package edu.ccrm.io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/xe";
    private static final String DB_USER = "ccrm_user"; // Use the new dedicated user
    private static final String DB_PASSWORD = "ccrm_pass"; // The password for the new user

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("CRITICAL: Oracle JDBC Driver not found! Ensure ojdbc jar is in the classpath.", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}