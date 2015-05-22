package org.sakaiproject.pasystem.impl.popups;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.pasystem.api.Popup;
import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;
import org.sakaiproject.user.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PopupForUser {

    private static final Logger LOG = LoggerFactory.getLogger(PopupForUser.class);

    private User user;
    private String eid;


    public PopupForUser(User currentUser) {
        user = currentUser;

        if (user != null && user.getEid() != null) {
            eid = user.getEid().toLowerCase();
        } else {
            user = null;
        }
    }


    public Popup getPopup() {
        if (user == null) {
            // No user.
            return Popup.createNullPopup();
        }

        String sql = ("SELECT popup.uuid, popup.descriptor, popup.start_time, popup.end_time, popup.open_campaign, content.template_content " +

                      // Find a popup screen
                      " FROM PASYSTEM_POPUP_SCREENS popup" +

                      // And its content
                      " INNER JOIN PASYSTEM_POPUP_CONTENT content on content.uuid = popup.uuid" +

                      // That is either assigned to the current user
                      " LEFT OUTER JOIN PASYSTEM_POPUP_ASSIGN assign " +
                      " on assign.uuid = popup.uuid AND lower(assign.user_eid) = ?" +

                      // Which the current user hasn't yet dismissed
                      " LEFT OUTER JOIN PASYSTEM_POPUP_DISMISSED dismissed " +
                      " on dismissed.uuid = popup.uuid AND lower(dismissed.user_eid) = ?" +

                      " WHERE " +

                      // It's assigned to us or open to call
                      " ((assign.uuid IS NOT NULL) OR (popup.open_campaign = 1)) AND " +

                      // And currently active
                      " popup.start_time <= ? AND " +
                      " ((popup.end_time = 0) OR (popup.end_time > ?)) AND " +

                      // And either hasn't been dismissed yet
                      " (dismissed.state is NULL OR" +

                      // Or was dismissed temporarily, but some time has passed
                      "  (dismissed.state = 'temporary' AND" +
                      "   (? - dismissed.dismiss_time) >= ?))");


        try {
            long now = System.currentTimeMillis();

            return DB.transaction
                ("Find a popup for the current user",
                 new DBAction<Popup>() {
                     public Popup call(DBConnection db) throws SQLException {
                         try (DBResults results = db.run(sql)
                              .param(eid).param(eid)
                              .param(now).param(now).param(now)
                              .param(getTemporaryTimeoutMilliseconds())
                              .executeQuery()) {
                             for (ResultSet result : results) {
                                 Clob contentClob = result.getClob(6);
                                 String templateContent = contentClob.getSubString(1, (int)contentClob.length());

                                 // Got one!
                                 return Popup.create(result.getString(1),
                                                     result.getString(2),
                                                     result.getLong(3),
                                                     result.getLong(4),
                                                     result.getInt(5) == 1,
                                                     templateContent);
                             }

                             // Otherwise, no suitable popup was found
                             return Popup.createNullPopup();
                         }
                     }
                 });
        } catch (Exception e) {
            LOG.error("Error determining active popup", e);
            return Popup.createNullPopup();
        }
    }


    public void acknowledge(String campaign, String acknowledgement) {
        if (user == null) {
            return;
        }

        final String mappedAcknowledgement = ("permanent".equals(acknowledgement) ? "permanent" : "temporary");

        try {
            DB.transaction
                ("Acknowledge campaign for user",
                 new DBAction<Void>() {
                     public Void call(DBConnection db) throws SQLException {
                         deleteExistingEntry(db, campaign);
                         insertNewEntry(db, campaign, mappedAcknowledgement);
                         db.commit();

                         return null;
                     }
                 }
                );
        } catch (Exception e) {
            LOG.error("Error acknowledging popup", e);
        }
    }


    private int getTemporaryTimeoutMilliseconds() {
        return ServerConfigurationService.getInt("popup.temporary-timeout-ms", (24 * 60 * 60 * 1000));
    }


    private boolean deleteExistingEntry(DBConnection db, String campaign) throws SQLException {
        int updatedRows = db.run("DELETE FROM PASYSTEM_POPUP_DISMISSED where lower(user_eid) = ? AND campaign = ?")
            .param(eid)
            .param(campaign)
            .executeUpdate();

        return (updatedRows > 0);
    }


    private boolean insertNewEntry(DBConnection db, String campaign, String acknowledgement) throws SQLException {
        long now = System.currentTimeMillis();
        int updatedRows = db.run("INSERT INTO PASYSTEM_POPUP_DISMISSED (user_eid, campaign, state, dismiss_time) VALUES (?, ?, ?, ?)")
            .param(eid)
            .param(campaign)
            .param(acknowledgement)
            .param(now)
            .executeUpdate();

        return (updatedRows > 0);
    }
}
