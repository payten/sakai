package org.sakaiproject.pasystem.impl.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;


public class DBResults implements Iterable<ResultSet>, Iterator<ResultSet>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DBResults.class);
    private PreparedStatement originalStatement;
    private ResultSet resultSet;
    private boolean hasRowReady;

    public DBResults(ResultSet rs, PreparedStatement originalStatement) {
        this.resultSet = rs;
        this.originalStatement = originalStatement;
    }

    public void close() throws SQLException {
        resultSet.close();
        originalStatement.close();
    }

    public boolean hasNext() {
        try {
            if (!hasRowReady) {
                hasRowReady = resultSet.next();
            }

            return hasRowReady;
        } catch (SQLException e) {
            LOG.warn("SQLException while calling hasNext", e);
            return false;
        }
    }

    public ResultSet next() {
        if (!hasRowReady) {
            throw new DBException("Read past end of results");
        }

        hasRowReady = false;
        return resultSet;
    }

    public Iterator<ResultSet> iterator() {
        return this;
    }
}
