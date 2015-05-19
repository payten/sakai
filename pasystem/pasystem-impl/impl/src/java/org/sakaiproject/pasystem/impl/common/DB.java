package org.sakaiproject.pasystem.impl.common;

import java.sql.Connection;
import java.sql.SQLException;
import org.sakaiproject.db.cover.SqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DB {

    private static final Logger LOG = LoggerFactory.getLogger(DB.class);


    public static <E> E transaction(DBAction<E> action) throws DBException {
        return transaction(action.toString(), action);
    }


    public static <E> E transaction(String actionDescription, DBAction<E> action) throws DBException {
        try {
            Connection db = SqlService.borrowConnection();
            DBConnection dbc = new DBConnection(db);
            boolean autocommit = db.getAutoCommit();

            try {
                db.setAutoCommit(false);
                
                return action.call(dbc);
            } finally {

                if (!dbc.wasResolved()) {
                    LOG.warn("**************\nDB Transaction was neither committed nor rolled back.  Committing for you.");
                    new Throwable().printStackTrace();
                    dbc.commit();
                }

                if (autocommit) {
                    db.setAutoCommit(autocommit);
                }
                SqlService.returnConnection(db);
            }

        } catch (SQLException e) {
            throw new DBException("Failure in database action: " + actionDescription, e);
        }
    }
}
