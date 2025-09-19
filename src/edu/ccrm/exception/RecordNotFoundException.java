package edu.ccrm.exception;

import java.sql.SQLException;

public class RecordNotFoundException extends RuntimeException {
    public RecordNotFoundException(String message) {
        super(message);
    }

    public RecordNotFoundException(String string, SQLException e) {
        super(string, e);
    }
}

