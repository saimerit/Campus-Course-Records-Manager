package edu.ccrm.exception;

import java.sql.SQLException;

public class DataIntegrityException extends RuntimeException {
    public DataIntegrityException(String message) {
        super(message);
    }

    public DataIntegrityException(String string, SQLException e) {
        super(string, e);
    }
}