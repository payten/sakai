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

import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;


public class PopupManager {

    private static final Logger LOG = LoggerFactory.getLogger(PopupManager.class);


    public String createPopup(String descriptor, Date startDate, Date endDate, InputStream templateContent) {
        return DB.transaction
            ("Popup creation",
             new DBAction<String>() {
                 public String call(Connection db) throws SQLException {
                     String id = insertSplashScreen(db, descriptor, startDate, endDate);
                     insertSplashContent(db, id, templateContent);
                     db.commit();

                     return id;
                 }
             });
    }


    public void openCampaign(String id) {
        DB.transaction
            ("Mark popup " + id + " as open campaign",
             new DBAction<Void>() {
                 public Void call(Connection db) throws SQLException {
                     String sql = "INSERT INTO PASYSTEM_SPLASH_ASSIGN (uuid, open_campaign) VALUES (?, 1)";

                     try (PreparedStatement ps = db.prepareStatement(sql)) {
                         ps.setString(1, id);
                         ps.executeUpdate();
                         db.commit();

                         return null;
                     }
                 }
             });
    }


    public boolean hasCampaign(String descriptor) {
        return DB.transaction
            ("Check whether campaign exists for descriptor: " + descriptor,
             new DBAction<Boolean>() {
                 public Boolean call(Connection db) throws SQLException {
                     String sql = "SELECT uuid from PASYSTEM_SPLASH_SCREENS WHERE descriptor = ?";

                     try (PreparedStatement ps = db.prepareStatement(sql)) {
                         ps.setString(1, descriptor);

                         try (ResultSet rs = ps.executeQuery()) {
                             if (rs.next()) {
                                 return true;
                             }

                             return false;
                         }
                     }
                 }
             });
    }


    private String insertSplashScreen(Connection db, String descriptor, Date startDate, Date endDate) throws SQLException {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO PASYSTEM_SPLASH_SCREENS (uuid, descriptor, start_time, end_time) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, descriptor);
            ps.setLong(3, startDate.getTime());
            ps.setLong(4, endDate.getTime());

            ps.executeUpdate();
        }
        return id;
    }


    private void insertSplashContent(Connection db, String id, InputStream templateContent) throws SQLException {
        String sql = "INSERT INTO PASYSTEM_SPLASH_CONTENT (uuid, template_content) VALUES (?, ?)";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
        
            ps.setString(1, id);
            ps.setClob(2, new InputStreamReader(templateContent));

            ps.executeUpdate();
        }
    }
}
