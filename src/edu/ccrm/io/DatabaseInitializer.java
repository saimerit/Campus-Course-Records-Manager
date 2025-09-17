package edu.ccrm.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class DatabaseInitializer {

    // Define the tables that are absolutely required for the app to run
    private static final List<String> REQUIRED_TABLES = Arrays.asList("INSTRUCTORS", "STUDENTS", "COURSES", "ENROLLMENTS");

    public static void initialize() {
        System.out.println("--- Database Initialization ---");
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("✅ Database connection successful for user '" + conn.getMetaData().getUserName() + "'.");

            System.out.println("   - Running schema setup script...");
            runScriptFromFile(conn);

            System.out.println("   - Verifying schema...");
            if (!isSchemaComplete(conn)) {
                // This is a critical failure. The tables were not created.
                throw new RuntimeException("Schema verification failed! Tables were not created correctly. Check permissions and SQL script.");
            }

            System.out.println("✅ Database schema is ready.");

        } catch (Exception e) {
            System.err.println("❌ CRITICAL FAILURE DURING DATABASE INITIALIZATION:");
            System.err.println("   " + e.getMessage());
            // Stop the application from starting if the database is not ready
            throw new RuntimeException("Could not initialize the database.", e);
        }
        System.out.println("-----------------------------");
    }

    private static void runScriptFromFile(Connection conn) {
        File scriptFile = new File("database_setup.sql");
        if (!scriptFile.exists()) {
            throw new RuntimeException("CRITICAL: 'database_setup.sql' not found in project root.");
        }

        try (Scanner scanner = new Scanner(scriptFile).useDelimiter(";")) {
            while (scanner.hasNext()) {
                String sql = scanner.next().trim();
                if (!sql.isEmpty() && !sql.toLowerCase().startsWith("--")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    } catch (SQLException e) {
                        // ORA-00942 on a DROP command is safe to ignore (table didn't exist)
                        if (e.getErrorCode() != 942 || !sql.trim().toLowerCase().startsWith("drop")) {
                            throw new SQLException("Failed to execute statement: " + sql, e);
                        }
                    }
                }
            }
        } catch (FileNotFoundException | SQLException e) {
            throw new RuntimeException("Error executing database setup script.", e);
        }
    }

    private static boolean isSchemaComplete(Connection conn) throws SQLException {
        for (String tableName : REQUIRED_TABLES) {
            if (!tableExists(conn, tableName)) {
                System.err.println("   - VERIFICATION FAILED: Table '" + tableName + "' is missing.");
                return false;
            }
        }
        return true;
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        ResultSet rs = conn.getMetaData().getTables(null, null, tableName.toUpperCase(), null);
        boolean exists = rs.next();
        rs.close();
        return exists;
    }
}