package org.sakaiproject.pasystem.impl.acknowledgements;

import org.sakaiproject.pasystem.api.Popup;
import org.sakaiproject.pasystem.api.Popups;
import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
