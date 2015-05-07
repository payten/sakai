package org.sakaiproject.pasystem.popups.impl;

import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.component.cover.ServerConfigurationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.pasystem.popups.api.Popup;
import org.sakaiproject.pasystem.popups.api.PopupManager;


public class DBPopupManager implements PopupManager {

    private static final Logger LOG = LoggerFactory.getLogger(DBPopupManager.class);

    private User user;
    private String eid;


    public DBPopupManager(User currentUser) {
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
            return PopupImpl.createNullPopup();
        }

        try {
            Connection db = SqlService.borrowConnection();

            try {
                long now = System.currentTimeMillis();

                PreparedStatement ps = db.prepareStatement("SELECT splash.campaign, splash.template_name " +

                                                           // Find a splash screen
                                                           " FROM NYU_T_SPLASH_SCREENS splash" +

                                                           // That is either assigned to the current user, or open to all
                                                           " LEFT OUTER JOIN NYU_T_SPLASH_ASSIGN assign " +
                                                           " on assign.campaign = splash.campaign AND (lower(assign.netid) = ? OR assign.open_campaign = 1)" +

                                                           // Which the current user hasn't yet dismissed
                                                           " LEFT OUTER JOIN NYU_T_SPLASH_DISMISSED dismissed " +
                                                           " on dismissed.campaign = splash.campaign AND lower(dismissed.netid) = ?" +

                                                           " WHERE " +

                                                           // It's assigned to us
                                                           " assign.campaign IS NOT NULL AND " +

                                                           // And currently active
                                                           " splash.start_time <= ? AND " +
                                                           " splash.end_time > ? AND " +

                                                           // And either hasn't been dismissed yet
                                                           " (dismissed.state is NULL OR" +

                                                           // Or was dismissed temporarily, but some time has passed
                                                           "  (dismissed.state = 'temporary' AND" +
                                                           "   (? - dismissed.dismiss_time) >= ?))");

                ps.setString(1, eid);
                ps.setString(2, eid);
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.setLong(5, now);
                ps.setInt(6, getTemporaryTimeoutMilliseconds());

                ResultSet rs = ps.executeQuery();

                try {
                    if (rs.next()) {
                        return PopupImpl.createPopup(rs.getString(1), rs.getString(2));
                    }
                } finally {
                    rs.close();
                }
            } finally {
                SqlService.returnConnection(db);
            }
        } catch (Exception e) {
            LOG.error("Error determining active popup", e);
            return PopupImpl.createNullPopup();
        }

        return PopupImpl.createNullPopup();
    }


    public void acknowledge(String campaign, String acknowledgement) {
        if (user == null) {
            return;
        }

        acknowledgement = ("permanent".equals(acknowledgement) ? "permanent" : "temporary");

        try {
            Connection db = SqlService.borrowConnection();
            boolean autocommit = db.getAutoCommit();

            try {

                db.setAutoCommit(false);
                deleteExistingEntry(db, campaign);
                insertNewEntry(db, campaign, acknowledgement);
                db.commit();
            } finally {
                db.setAutoCommit(autocommit);
                SqlService.returnConnection(db);
            }
        } catch (Exception e) {
            LOG.error("Error acknowledging popup", e);
        }
    }


    private int getTemporaryTimeoutMilliseconds() {
        return ServerConfigurationService.getInt("popup.temporary-timeout-ms", (24 * 60 * 60 * 1000));
    }


    private boolean deleteExistingEntry(Connection db, String campaign) throws SQLException {
        PreparedStatement ps = db.prepareStatement("DELETE FROM NYU_T_SPLASH_DISMISSED where lower(netid) = ? AND campaign = ?");

        try {
            ps.setString(1, eid);
            ps.setString(2, campaign);

            return (ps.executeUpdate() > 0);
        } finally {
            ps.close();
        }
    }


    private boolean insertNewEntry(Connection db, String campaign, String acknowledgement) throws SQLException {
        PreparedStatement ps = db.prepareStatement("INSERT INTO NYU_T_SPLASH_DISMISSED (netid, campaign, state, dismiss_time) VALUES (?, ?, ?, ?)");
        long now = System.currentTimeMillis();

        try {

            ps.setString(1, eid);
            ps.setString(2, campaign);
            ps.setString(3, acknowledgement);
            ps.setLong(4, now);

            return (ps.executeUpdate() > 0);
        } finally {
            ps.close();
        }
    }

}
