package org.sakaiproject.pasystem.impl.common;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBAction<E> {
    public E call(DBConnection db) throws SQLException;
}
