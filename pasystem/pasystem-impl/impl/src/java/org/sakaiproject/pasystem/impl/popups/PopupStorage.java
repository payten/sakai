package org.sakaiproject.pasystem.impl.popups;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.sakaiproject.pasystem.api.Popup;
import org.sakaiproject.pasystem.api.Popups;
import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PopupStorage implements Popups {

    private static final Logger LOG = LoggerFactory.getLogger(PopupStorage.class);

    public String createCampaign(String descriptor, Date startDate, Date endDate, InputStream templateContent) {
        return DB.transaction
            ("Popup creation",
             new DBAction<String>() {
                 public String call(DBConnection db) throws SQLException {
                     String id = insertPopupScreen(db, descriptor, startDate, endDate);
                     insertPopupContent(db, id, templateContent);
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
                     db.run("INSERT INTO PASYSTEM_POPUP_ASSIGN (uuid, open_campaign) VALUES (?, 1)")
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
                     try (DBResults results = db.run("SELECT uuid from PASYSTEM_POPUP_SCREENS WHERE descriptor = ?")
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


    public List<Popup> getAll() {
        return DB.transaction
            ("Find all popups",
             new DBAction<List<Popup>>() {
                 public List<Popup> call(DBConnection db) throws SQLException {
                     List<Popup> popups = new ArrayList<Popup>();
                     try (DBResults results = db.run("SELECT * from PASYSTEM_POPUP_SCREENS")
                          .executeQuery()) {
                         for (ResultSet result : results) {
                             popups.add(PopupImpl.createPopup(result.getString("uuid"),
                                                              result.getString("descriptor"),
                                                              result.getLong("start_time"),
                                                              result.getLong("end_time")));
                         }

                         return popups;
                     }
                 }
             });
    }


    public Optional<Popup> getForId(final String uuid) {
        return DB.transaction
            ("Find a popup by uuid",
             new DBAction<Optional<Popup>>() {
                 public Optional<Popup> call(DBConnection db) throws SQLException {
                     try (DBResults results = db.run("SELECT * from PASYSTEM_POPUP_SCREENS WHERE UUID = ?")
                          .param(uuid)
                          .executeQuery()) {
                         for (ResultSet result : results) {
                             return Optional.of(PopupImpl.createPopup(result.getString("uuid"),
                                                                      result.getString("descriptor"),
                                                                      result.getLong("start_time"),
                                                                      result.getLong("end_time")));
                         }

                         return Optional.empty();
                     }
                 }
             });
    }


    public boolean isOpenCampaign(final String uuid) {
        return DB.transaction
            ("True if uuid refers to an open campaign",
             new DBAction<Boolean>() {
                 public Boolean call(DBConnection db) throws SQLException {
                     List<String> users = new ArrayList<String>();

                     try (DBResults results = db.run("SELECT * from PASYSTEM_POPUP_ASSIGN WHERE UUID = ? AND open_campaign = 1")
                          .param(uuid)
                          .executeQuery()) {
                         for (ResultSet result : results) {
                             return true;
                         }

                         return false;
                     }
                 }
             });
    }


    public List<String> getAssignees(final String uuid) {
        return DB.transaction
            ("Find a list of assignees by popup uuid",
             new DBAction<List<String>>() {
                 public List<String> call(DBConnection db) throws SQLException {
                     List<String> users = new ArrayList<String>();

                     try (DBResults results = db.run("SELECT user_eid from PASYSTEM_POPUP_ASSIGN WHERE UUID = ? AND user_eid is not NULL")
                          .param(uuid)
                          .executeQuery()) {
                         for (ResultSet result : results) {
                             users.add(result.getString(1));
                         }

                         return users;
                     }
                 }
             });
    }


    private String insertPopupScreen(DBConnection db, String descriptor, Date startDate, Date endDate) throws SQLException {
        String id = UUID.randomUUID().toString();

        db.run("INSERT INTO PASYSTEM_POPUP_SCREENS (uuid, descriptor, start_time, end_time) VALUES (?, ?, ?, ?)")
            .param(id)
            .param(descriptor)
            .param(startDate.getTime())
            .param(endDate.getTime())
            .executeUpdate();

        return id;
    }


    private void insertPopupContent(DBConnection db, String id, InputStream templateContent) throws SQLException {
        db.run("INSERT INTO PASYSTEM_POPUP_CONTENT (uuid, template_content) VALUES (?, ?)")
            .param(id)
            .param(new InputStreamReader(templateContent))
            .executeUpdate();
    }
}
