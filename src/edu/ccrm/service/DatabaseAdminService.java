package edu.ccrm.service;

import edu.ccrm.io.DatabaseManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseAdminService {

    /**
     * Clears all data from all tables WITHOUT dropping them.
     * Preserves the schema so the application can continue running.
     */
    public void clearAllData() {
        // Order matters due to FK constraints: dependents first
        String[] tablesToClear = {"ENROLLMENTS", "COURSES", "STUDENTS", "INSTRUCTORS"};

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String tableName : tablesToClear) {
                try {
                    stmt.executeUpdate("DELETE FROM " + tableName);
                    System.out.println("Successfully cleared data from: " + tableName);
                } catch (SQLException e) {
                    System.err.println("Could not clear table " + tableName + ": " + e.getMessage());
                }
            }
            System.out.println("All data cleared. Schema is intact.");

        } catch (SQLException e) {
            System.err.println("A critical error occurred while clearing data.");
            e.printStackTrace();
        }
    }
}