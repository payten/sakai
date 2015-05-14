package org.sakaiproject.pasystem.impl.popups;

import org.sakaiproject.db.cover.SqlService;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.UUID;
import java.util.Date;


public class PopupManager {

    private static final Logger LOG = LoggerFactory.getLogger(PopupManager.class);

    public String createPopup(String descriptor, Date startDate, Date endDate, InputStream templateContent) {
        try {
            Connection db = SqlService.borrowConnection();
            boolean autocommit = db.getAutoCommit();

            try {
                db.setAutoCommit(false);
                String id = insertSplashScreen(db, descriptor, startDate, endDate);
                insertSplashContent(db, id, templateContent);
                db.commit();

                return id;
            } finally {
                if (autocommit) {
                    db.setAutoCommit(autocommit);
                }
                SqlService.returnConnection(db);
            }

        } catch (SQLException e) {
            throw new PopupException("Popup creation failed", e);
        }
    }


    public void openCampaign(String id) {
        try {
            Connection db = SqlService.borrowConnection();

            try {
                PreparedStatement ps = db.prepareStatement("INSERT INTO PASYSTEM_SPLASH_ASSIGN (uuid, open_campaign) VALUES (?, 1)");
        
                ps.setString(1, id);
                ps.executeUpdate();
                db.commit();
            } finally {
                SqlService.returnConnection(db);
            }

        } catch (SQLException e) {
            throw new PopupException("Failed to mark popup as an open campaign", e);
        }
    }


    public boolean hasCampaign(String descriptor) {
        try {
            Connection db = SqlService.borrowConnection();

            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                ps = db.prepareStatement("SELECT uuid from PASYSTEM_SPLASH_SCREENS WHERE descriptor = ?");
                ps.setString(1, descriptor);

                rs = ps.executeQuery();

                if (rs.next()) {
                    return true;
                }

                return false;
            } finally {
                if (rs != null) { rs.close(); }
                if (ps != null) { ps.close(); }
                SqlService.returnConnection(db);
            }

        } catch (SQLException e) {
            throw new PopupException("Failed to check for matching campaign", e);
        }
    }




    private String insertSplashScreen(Connection db, String descriptor, Date startDate, Date endDate) throws SQLException {
        String id = UUID.randomUUID().toString();
        PreparedStatement ps = db.prepareStatement("INSERT INTO PASYSTEM_SPLASH_SCREENS (uuid, descriptor, start_time, end_time) VALUES (?, ?, ?, ?)");
        
        ps.setString(1, id);
        ps.setString(2, descriptor);
        ps.setLong(3, startDate.getTime());
        ps.setLong(4, endDate.getTime());

        ps.executeUpdate();

        return id;
    }


    private void insertSplashContent(Connection db, String id, InputStream templateContent) throws SQLException {
        PreparedStatement ps = db.prepareStatement("INSERT INTO PASYSTEM_SPLASH_CONTENT (uuid, template_content) VALUES (?, ?)");
        
        ps.setString(1, id);
        ps.setClob(2, new InputStreamReader(templateContent));

        ps.executeUpdate();
    }
}
