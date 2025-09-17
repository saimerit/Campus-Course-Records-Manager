package edu.ccrm.io;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/CampusDB";
    private static final String DB_USER = "System";
    private static final String DB_PASSWORD = "Sentry09*";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}