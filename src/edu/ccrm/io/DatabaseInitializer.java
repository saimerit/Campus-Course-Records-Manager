package edu.ccrm.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;

public class DatabaseInitializer {

    private static final String SCRIPT_FILE_PATH = "database_setup.sql";
    private static final String[] EXPECTED_TABLES = {"INSTRUCTORS", "STUDENTS", "COURSES", "ENROLLMENTS"};

    public static void initialize() {
        System.out.println("--- Database Initialization ---");
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("? Database connection successful for user '" + conn.getMetaData().getUserName() + "'.");
            System.out.println("    - Verifying schema...");

            if (verifySchema(conn)) {
                System.out.println("? Schema already exists. Skipping setup script.");
            } else {
                System.out.println("    - Schema not found. Running setup script...");
                runScriptFromFile(SCRIPT_FILE_PATH);

                System.out.println("    - Verifying schema again...");
                if (verifySchema(conn)) {
                    System.out.println("? Schema verification successful. All tables are present.");
                } else {
                    throw new RuntimeException("Schema verification failed after running setup script. Check permissions and SQL script.");
                }
            }
        } catch (SQLException | IOException e) {
            System.err.println("? CRITICAL FAILURE DURING DATABASE INITIALIZATION:");
            System.err.println("    " + e.getMessage());
            throw new RuntimeException("Could not initialize the database.", e);
        }
    }

    private static void runScriptFromFile(String filePath) throws IOException, SQLException {
        Path scriptPath = Path.of(filePath);
        String content = Files.readString(scriptPath);

        content = content.replaceAll("--.*", "").trim();
        StringTokenizer tokenizer = new StringTokenizer(content, ";/", false);

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            while (tokenizer.hasMoreTokens()) {
                String sql = tokenizer.nextToken().trim();
                if (sql.isEmpty()) {
                    continue;
                }

                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 942) {
                    } else {
                        System.err.println("SQL Error on statement: " + sql);
                        throw e;
                    }
                }
            }
        }
    }

    private static boolean verifySchema(Connection conn) throws SQLException {
        for (String tableName : EXPECTED_TABLES) {
            try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
                if (!rs.next()) {
                    System.err.println("    - VERIFICATION FAILED: Table '" + tableName + "' is missing.");
                    return false;
                }
            }
        }
        return true;
    }
}