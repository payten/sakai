package org.sakaiproject.pasystem.impl.acknowledgements;

import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;

import java.sql.SQLException;
import java.util.UUID;


public class AcknowledgementStorage {

    public enum NotificationType {
        BANNER,
        POPUP
    }

    private String tableName;

    public AcknowledgementStorage(NotificationType type) {
        tableName = "PASYSTEM_" + type + "_dismissed";
    }


    public void acknowledge(final String uuid, final String userEid, final String acknowledgementType) {
        DB.transaction
            ("Acknowledge a notification on behalf of a user",
             new DBAction<Void>() {
                 public Void call(DBConnection db) throws SQLException {
                     String state = ("temporary".equals(acknowledgementType)) ? "temporary" : "permanent";

                     db.run("INSERT INTO " + tableName + " (uuid, user_eid, state, dismiss_time) values (?, ?, ?, ?)")
                         .param(uuid)
                         .param(userEid)
                         .param(state)
                         .param(System.currentTimeMillis())
                         .executeUpdate();

                     db.commit();
                     return null;
                 }
             }
            );
    }
}
