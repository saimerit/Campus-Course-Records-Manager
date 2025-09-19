package edu.ccrm.service;

import edu.ccrm.io.DatabaseManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseAdminService {

    /**
     * Drops all major tables from the database to reset the application's state.
     */
    public void dropAllTables() {
        // The order is important due to foreign key constraints.
        // Drop tables that are referenced by others last.
        String[] tablesToDrop = {"ENROLLMENTS", "COURSES", "STUDENTS", "INSTRUCTORS"};

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String tableName : tablesToDrop) {
                try {
                    stmt.executeUpdate("DROP TABLE " + tableName);
                    System.out.println("Successfully dropped table: " + tableName);
                } catch (SQLException e) {
                    // This error is often expected if the table doesn't exist, so we just print it.
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