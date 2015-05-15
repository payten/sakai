package org.sakaiproject.pasystem.impl.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBConnection {

    private Connection connection;
    private boolean resolved;
    private boolean dirty;


    public DBConnection(Connection connection) {
        this.connection = connection;
        this.dirty = false;
        this.resolved = false;
    }

    public void commit() throws SQLException {
        connection.commit();
        resolved = true;
    }

    public void rollback() throws SQLException {
        connection.rollback();
        resolved = true;
    }

    public void markAsDirty() {
        this.dirty = true;
    }

    public boolean wasResolved() {
        if (dirty) {
            return resolved;
        } else {
            return true;
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public DBPreparedStatement run(String sql) throws SQLException {
        return new DBPreparedStatement(connection.prepareStatement(sql), this);
    }
}
