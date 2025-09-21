package edu.ccrm.service;

import edu.ccrm.io.DatabaseManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseAdminService {
    public void dropAllTables() {
        String[] tablesToDrop = {"ENROLLMENTS", "COURSES", "STUDENTS", "INSTRUCTORS"};

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String tableName : tablesToDrop) {
                try {
                    stmt.executeUpdate("DROP TABLE " + tableName);
                    System.out.println("Successfully dropped table: " + tableName);
                } catch (SQLException e) {
                    System.err.println("Info: Could not drop table " + tableName + ". It may not exist. " + e.getMessage());
                }
            }
            System.out.println("Database tables have been cleared.");

        } catch (SQLException e) {
            System.err.println("A critical error occurred while trying to drop database tables.");
            e.printStackTrace();
        }
    }
}