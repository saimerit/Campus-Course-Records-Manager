package edu.ccrm.io;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/xe";
    private static final String DB_USER = "ccrm_user";
    private static final String DB_PASSWORD = "ccrm_pass";

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setDriverClassName("oracle.jdbc.OracleDriver");

        // Enterprise Connection Pool Sizing & Resiliency
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(5);
        config.setIdleTimeout(300000);        // 5 minutes idle
        config.setConnectionTimeout(20000);   // 20 seconds connection timeout
        config.setValidationTimeout(3000);     // 3 seconds validation timeout
        config.setMaxLifetime(1800000);       // 30 minutes max lifetime
        config.setLeakDetectionThreshold(10000); // 10 seconds leak detection

        // Oracle-Specific Performance Tunings
        config.addDataSourceProperty("implicitCachingEnabled", "true");
        config.addDataSourceProperty("statementCacheSize", "50");

        dataSource = new HikariDataSource(config);
    }

    /**
     * Retrieves a pooled, resilient database connection.
     * @return Connection from HikariCP Pool
     * @throws SQLException if a database error occurs
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Shuts down the connection pool and releases all database connections.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}