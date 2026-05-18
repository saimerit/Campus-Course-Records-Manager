package edu.ccrm.io;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/xe";
    private static final String DB_USER = "ccrm_user";
    private static final String DB_PASSWORD = "ccrm_pass";

    private static final int MAX_POOL_SIZE = 15;
    private static final BlockingQueue<Connection> pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);

    public static Connection getConnection() throws SQLException {
        Connection conn = pool.poll();
        if (conn != null) {
            try {
                if (conn.isClosed() || !conn.isValid(2)) {
                    try { conn.close(); } catch (SQLException ignored) {}
                    conn = null;
                }
            } catch (SQLException e) {
                conn = null;
            }
        }
        if (conn == null) {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }

        final Connection rawConnection = conn;
        return (Connection) Proxy.newProxyInstance(
            DatabaseManager.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("close".equals(method.getName())) {
                        try {
                            if (!rawConnection.isClosed() && rawConnection.isValid(2)) {
                                // Reset auto-commit state before returning to pool
                                if (!rawConnection.getAutoCommit()) {
                                    rawConnection.setAutoCommit(true);
                                }
                                if (!pool.offer(rawConnection)) {
                                    rawConnection.close();
                                }
                            } else {
                                rawConnection.close();
                            }
                        } catch (SQLException e) {
                            try { rawConnection.close(); } catch (SQLException ignored) {}
                        }
                        return null;
                    }
                    try {
                        return method.invoke(rawConnection, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
            }
        );
    }
}