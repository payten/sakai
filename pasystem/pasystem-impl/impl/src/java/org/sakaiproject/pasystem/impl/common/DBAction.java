package org.sakaiproject.pasystem.impl.common;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBAction<E> {
    public E call(Connection db) throws SQLException;
}
