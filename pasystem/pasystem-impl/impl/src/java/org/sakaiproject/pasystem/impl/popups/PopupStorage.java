package org.sakaiproject.pasystem.impl.popups;

import org.sakaiproject.pasystem.api.Acknowledger;
import org.sakaiproject.pasystem.api.Popup;
import org.sakaiproject.pasystem.api.Popups;
import org.sakaiproject.pasystem.impl.acknowledgements.AcknowledgementStorage;
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


public class PopupStorage implements Popups, Acknowledger {

    private static final Logger LOG = LoggerFactory.getLogger(PopupStorage.class);

    public String createCampaign(Popup popup,
                                 InputStream templateInput,
                                 Optional<List<String>> assignToUsers) {
        return DB.transaction
                ("Popup creation",
                        new DBAction<String>() {
                            public String call(DBConnection db) throws SQLException {
                                String uuid = UUID.randomUUID().toString();

                                db.run("INSERT INTO PASYSTEM_POPUP_SCREENS (uuid, descriptor, start_time, end_time, open_campaign) VALUES (?, ?, ?, ?, ?)")
                                        .param(uuid)
                                        .param(popup.getDescriptor())
                                        .param(popup.getStartTime())
                                        .param(popup.getEndTime())
                                        .param(popup.isOpenCampaign() ? 1 : 0)
                                        .executeUpdate();

                                setPopupContent(db, uuid, templateInput);
                                setPopupAssignees(db, uuid, assignToUsers);

                                db.commit();

                                return uuid;
                            }
                        }
                );
    }


    public boolean updateCampaign(String uuid,
                                  Popup popup,
                                  Optional<InputStream> templateInput,
                                  Optional<List<String>> assignToUsers) {
        return DB.transaction
                ("Update an existing popup campaign",
                        new DBAction<Boolean>() {
                            public Boolean call(DBConnection db) throws SQLException {

                                if (db.run("UPDATE PASYSTEM_POPUP_SCREENS SET descriptor = ?, start_time = ?, end_time = ?, open_campaign = ? WHERE uuid = ?")
                                        .param(popup.getDescriptor())
                                        .param(popup.getStartTime())
                                        .param(popup.getEndTime())
                                        .param(popup.isOpenCampaign() ? 1 : 0)
                                        .param(uuid)
                                        .executeUpdate() == 0) {
                                    LOG.warn("Failed to update popup with UUID: {}", uuid);
                                    return false;
                                }

                                setPopupAssignees(db, uuid, assignToUsers);

                                if (templateInput.isPresent()) {
                                    setPopupContent(db, uuid, templateInput.get());
                                }

                                db.commit();

                                LOG.info("Update of popup {} completed", uuid);

                                return true;
                            }
                        }
                );
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
                                        popups.add(Popup.create(result.getString("uuid"),
                                                result.getString("descriptor"),
                                                result.getLong("start_time"),
                                                result.getLong("end_time"),
                                                result.getInt("open_campaign") == 1));
                                    }

                                    return popups;
                                }
                            }
                        }
                );
    }


    public String getPopupContent(final String uuid) {
        return DB.transaction
                ("Get the content for a popup",
                        new DBAction<String>() {
                            public String call(DBConnection db) throws SQLException {
                                try (DBResults results = db.run("SELECT template_content from PASYSTEM_POPUP_CONTENT where uuid = ?")
                                        .param(uuid)
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        Clob contentClob = result.getClob("template_content");
                                        return contentClob.getSubString(1, (int) contentClob.length());
                                    }

                                    return "";
                                }
                            }
                        }
                );
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
                                        return Optional.of(Popup.create(result.getString("uuid"),
                                                result.getString("descriptor"),
                                                result.getLong("start_time"),
                                                result.getLong("end_time"),
                                                result.getInt("open_campaign") == 1));
                                    }

                                    return Optional.empty();
                                }
                            }
                        }
                );
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
                                        users.add(result.getString("user_eid"));
                                    }

                                    return users;
                                }
                            }
                        }
                );
    }


    private void setPopupContent(DBConnection db, String uuid, InputStream templateContent) throws SQLException {
        // A little hoop jumping here to avoid having to rewind the InputStream
        //
        // Add an empty record if one is missing
        try {
            db.run("INSERT INTO PASYSTEM_POPUP_CONTENT (uuid) VALUES (?)")
                    .param(uuid)
                    .executeUpdate();
        } catch (SQLException e) {
            // Expected for updates
        }

        // Set the content CLOB
        db.run("UPDATE PASYSTEM_POPUP_CONTENT set template_content = ? WHERE uuid = ?")
                .param(new InputStreamReader(templateContent))
                .param(uuid)
                .executeUpdate();
    }


    private void setPopupAssignees(DBConnection db, String uuid, Optional<List<String>> assignToUsers) throws SQLException {
        if (assignToUsers.isPresent()) {
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


    public boolean deleteCampaign(final String uuid) {
        return DB.transaction
                ("Delete an existing popup campaign",
                        new DBAction<Boolean>() {
                            public Boolean call(DBConnection db) throws SQLException {
                                db.run("DELETE FROM PASYSTEM_POPUP_ASSIGN where uuid = ?")
                                        .param(uuid)
                                        .executeUpdate();

                                db.run("DELETE FROM PASYSTEM_POPUP_DISMISSED where uuid = ?")
                                        .param(uuid)
                                        .executeUpdate();

                                db.run("DELETE FROM PASYSTEM_POPUP_CONTENT where uuid = ?")
                                        .param(uuid)
                                        .executeUpdate();

                                db.run("DELETE FROM PASYSTEM_POPUP_SCREENS WHERE uuid = ?")
                                        .param(uuid)
                                        .executeUpdate();

                                db.commit();

                                return true;
                            }
                        }
                );
    }


    public void acknowledge(final String uuid, final String userEid, final String acknowledgementType) {
        new AcknowledgementStorage(AcknowledgementStorage.NotificationType.POPUP).acknowledge(uuid, userEid, acknowledgementType);
    }
}
