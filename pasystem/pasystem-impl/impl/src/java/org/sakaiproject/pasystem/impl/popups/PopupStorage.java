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

    public String createCampaign(String descriptor, long startTime, long endTime,
                                 InputStream templateInput,
                                 boolean isOpenCampaign,
                                 Optional<List<String>> assignToUsers) {
        return DB.transaction
            ("Popup creation",
             new DBAction<String>() {
                 public String call(DBConnection db) throws SQLException {
                     String uuid = UUID.randomUUID().toString();
                     
                     db.run("INSERT INTO PASYSTEM_POPUP_SCREENS (uuid, descriptor, start_time, end_time) VALUES (?, ?, ?, ?)")
                         .param(uuid)
                         .param(descriptor)
                         .param(startTime)
                         .param(endTime)
                         .executeUpdate();

                     setPopupContent(db, uuid, templateInput);
                     setPopupAssignees(db, uuid, isOpenCampaign, assignToUsers);

                     db.commit();

                     return uuid;
                 }
             });
    }


  public boolean updateCampaign(String uuid, String descriptor, long startTime, long endTime,
                                Optional<InputStream> templateInput,
                                boolean isOpenCampaign,
                                Optional<List<String>> assignToUsers) {
    return DB.transaction
            ("Update an existing popup campaign",
             new DBAction<Boolean>() {
                 public Boolean call(DBConnection db) throws SQLException {

                     if (db.run("UPDATE PASYSTEM_POPUP_SCREENS SET descriptor = ?, start_time = ?, end_time = ? WHERE uuid = ?")
                         .param(descriptor).param(startTime).param(endTime).param(uuid)
                         .executeUpdate() == 0) {
                         LOG.warn("Failed to update popup with UUID: {}", uuid);
                         return false;
                     }

                     setPopupAssignees(db, uuid, isOpenCampaign, assignToUsers);

                     if (templateInput.isPresent()) {
                         setPopupContent(db, uuid, templateInput.get());
                     }

                     db.commit();

                     LOG.info("Update of popup {} completed", uuid);
                     
                     return true;
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
                           popups.add(Popup.createPopup(result.getString("uuid"),
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
                           return Optional.of(Popup.createPopup(result.getString("uuid"),
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


    private void setPopupContent(DBConnection db, String uuid, InputStream templateContent) throws SQLException {
        if (db.run("UPDATE PASYSTEM_POPUP_CONTENT set template_content = ? WHERE uuid = ?")
            .param(new InputStreamReader(templateContent))
            .param(uuid)
            .executeUpdate() == 0) {

            db.run("INSERT INTO PASYSTEM_POPUP_CONTENT (uuid, template_content) VALUES (?, ?)")
                .param(uuid)
                .param(new InputStreamReader(templateContent))
                .executeUpdate();
        }
    }


    private void setPopupAssignees(DBConnection db, String uuid, boolean isOpenCampaign, Optional<List<String>> assignToUsers) throws SQLException {
        db.run("DELETE FROM PASYSTEM_POPUP_ASSIGN where uuid = ? AND open_campaign = 1")
            .param(uuid)
            .executeUpdate();

        if (isOpenCampaign) {
            db.run("INSERT INTO PASYSTEM_POPUP_ASSIGN (uuid, open_campaign) VALUES (?, 1)")
                .param(uuid)
                .executeUpdate();
        } else if (assignToUsers.isPresent()) {
            db.run("DELETE FROM PASYSTEM_POPUP_ASSIGN where uuid = ? AND user_eid is not NULL")
                .param(uuid)
                .executeUpdate();

            for (String userEid : assignToUsers.get()) {
                db.run("INSERT INTO PASYSTEM_POPUP_ASSIGN (uuid, user_eid) VALUES (?, ?)")
                    .param(uuid)
                    .param(userEid)
                    .executeUpdate();
            }
        }
    }
                                   
}
