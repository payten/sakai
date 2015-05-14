package org.sakaiproject.pasystem.impl.popups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.UUID;
import java.util.Date;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;


public class PopupManager {

    private static final Logger LOG = LoggerFactory.getLogger(PopupManager.class);


    public String createPopup(String descriptor, Date startDate, Date endDate, InputStream templateContent) {
        return DB.transaction
            ("Popup creation",
             new DBAction<String>() {
                 public String call(DBConnection db) throws SQLException {
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
                 public Void call(DBConnection db) throws SQLException {
                     db.run("INSERT INTO PASYSTEM_SPLASH_ASSIGN (uuid, open_campaign) VALUES (?, 1)")
                         .param(id)
                         .executeUpdate();

                     db.commit();
                     return null;
                 }
             });
    }


    public boolean hasCampaign(String descriptor) {
        return DB.transaction
            ("Check whether campaign exists for descriptor: " + descriptor,
             new DBAction<Boolean>() {
                 public Boolean call(DBConnection db) throws SQLException {
                     try (DBResults results = db.run("SELECT uuid from PASYSTEM_SPLASH_SCREENS WHERE descriptor = ?")
                          .param(descriptor)
                          .executeQuery()) {
                         for (ResultSet result : results) {
                             return true;
                         }

                         return false;
                     }
                 }
             });
    }


    private String insertSplashScreen(DBConnection db, String descriptor, Date startDate, Date endDate) throws SQLException {
        String id = UUID.randomUUID().toString();

        db.run("INSERT INTO PASYSTEM_SPLASH_SCREENS (uuid, descriptor, start_time, end_time) VALUES (?, ?, ?, ?)")
            .param(id)
            .param(descriptor)
            .param(startDate.getTime())
            .param(endDate.getTime())
            .executeUpdate();

        return id;
    }


    private void insertSplashContent(DBConnection db, String id, InputStream templateContent) throws SQLException {
        db.run("INSERT INTO PASYSTEM_SPLASH_CONTENT (uuid, template_content) VALUES (?, ?)")
            .param(id)
            .param(new InputStreamReader(templateContent))
            .executeUpdate();
    }
}
