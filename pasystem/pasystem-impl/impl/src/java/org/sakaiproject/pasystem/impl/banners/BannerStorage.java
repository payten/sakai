package org.sakaiproject.pasystem.impl.banners;

import org.sakaiproject.pasystem.api.Acknowledger;
import org.sakaiproject.pasystem.api.Banner;
import org.sakaiproject.pasystem.api.Banners;
import org.sakaiproject.pasystem.impl.acknowledgements.AcknowledgementStorage;
import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class BannerStorage implements Banners, Acknowledger {

    private static final Logger LOG = LoggerFactory.getLogger(BannerStorage.class);

    public List<Banner> getAll() {
        return DB.transaction
                ("Find all banners",
                        new DBAction<List<Banner>>() {
                            public List<Banner> call(DBConnection db) throws SQLException {
                                List<Banner> banners = new ArrayList<Banner>();
                                try (DBResults results = db.run("SELECT * from PASYSTEM_BANNER_ALERT")
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        banners.add(new Banner(result.getString("uuid"),
                                                result.getString("message"),
                                                result.getString("hosts"),
                                                result.getInt("dismissible"),
                                                result.getInt("active"),
                                                result.getLong("start_time"),
                                                result.getLong("end_time"),
                                                result.getString("type")));
                                    }

                                    return banners;
                                }
                            }
                        }
                );
    }


    public Optional<Banner> getForId(final String uuid) {
        return DB.transaction
                ("Find a banner by uuid",
                        new DBAction<Optional<Banner>>() {
                            public Optional<Banner> call(DBConnection db) throws SQLException {
                                try (DBResults results = db.run("SELECT * from PASYSTEM_BANNER_ALERT WHERE UUID = ?")
                                        .param(uuid)
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        return Optional.of(new Banner(result.getString("uuid"),
                                                result.getString("message"),
                                                result.getString("hosts"),
                                                result.getInt("dismissible"),
                                                result.getInt("active"),
                                                result.getLong("start_time"),
                                                result.getLong("end_time"),
                                                result.getString("type")));
                                    }

                                    return Optional.empty();
                                }
                            }
                        }
                );
    }


    public List<Banner> getRelevantAlerts(final String serverId, final String userEid) {
        final String sql = ("SELECT alert.*" +
                            " from PASYSTEM_BANNER_ALERT alert" +
                            " LEFT OUTER JOIN PASYSTEM_BANNER_DISMISSED dismissed on dismissed.uuid = alert.uuid" +
                            "  AND ((? = '') OR lower(dismissed.user_eid) = ?)" +
                            " where ACTIVE = 1 AND" +

                            // And either hasn't been dismissed yet
                            " (dismissed.state is NULL OR" +

                            // Or was dismissed temporarily, but some time has passed
                            "  (dismissed.state = 'temporary' AND" +
                            "   (? - dismissed.dismiss_time) >= ?))");

        return DB.transaction
                ("Find all active alerts for the server: " + serverId,
                        new DBAction<List<Banner>>() {
                            public List<Banner> call(DBConnection db) throws SQLException {
                                List<Banner> alerts = new ArrayList<Banner>();
                                try (DBResults results = db.run(sql)
                                        .param((userEid == null) ? "" : userEid.toLowerCase())
                                        .param((userEid == null) ? "" : userEid.toLowerCase())
                                        .param(System.currentTimeMillis())
                                        .param(getTemporaryTimeoutMilliseconds())
                                        .executeQuery()) {
                                    for (ResultSet result : results) {
                                        Banner alert = new Banner(result.getString("uuid"),
                                                result.getString("message"),
                                                result.getString("hosts"),
                                                result.getInt("dismissible"),
                                                result.getInt("active"),
                                                result.getLong("start_time"),
                                                result.getLong("end_time"),
                                                result.getString("type"));

                                        if (alert.isActiveForHost(serverId)) {
                                            alerts.add(alert);
                                        }
                                    }

                                    return alerts;
                                }
                            }
                        }
                );
    }


    private int getTemporaryTimeoutMilliseconds() {
        return ServerConfigurationService.getInt("pasystem.banner.temporary-timeout-ms", (24 * 60 * 60 * 1000));
    }


    public String createBanner(String message, String hosts, boolean isDismissible, boolean isActive, long startTime, long endTime, String type) {
        return DB.transaction(
                "Create an banner",
                new DBAction<String>() {
                    public String call(DBConnection db) throws SQLException {
                        String id = UUID.randomUUID().toString();

                        db.run("INSERT INTO PASYSTEM_BANNER_ALERT (uuid, message, hosts, dismissible, active, start_time, end_time, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
                                .param(id)
                                .param(message)
                                .param(hosts)
                                .param(new Integer(isDismissible ? 1 : 0))
                                .param(new Integer(isActive ? 1 : 0))
                                .param(startTime)
                                .param(endTime)
                                .param(type)
                                .executeUpdate();

                        db.commit();

                        return id;
                    }
                }
        );
    }


    public void updateBanner(String uuid, String message, String hosts, boolean isDismissible, boolean isActive, long startTime, long endTime, String type) {
        DB.transaction(
                "Update banner with uuid " + uuid,
                new DBAction<Void>() {
                    public Void call(DBConnection db) throws SQLException {
                        db.run("UPDATE PASYSTEM_BANNER_ALERT SET message = ?, hosts = ?, dismissible = ?, active = ?, start_time = ?, end_time = ?, type = ? WHERE uuid = ?")
                                .param(message)
                                .param(hosts)
                                .param(new Integer(isDismissible ? 1 : 0))
                                .param(new Integer(isActive ? 1 : 0))
                                .param(startTime)
                                .param(endTime)
                                .param(type)
                                .param(uuid)
                                .executeUpdate();

                        db.commit();

                        return null;
                    }
                }
        );
    }


    public void deleteBanner(String uuid) {
        DB.transaction(
                "Update banner with uuid " + uuid,
                new DBAction<Void>() {
                    public Void call(DBConnection db) throws SQLException {
                        db.run("DELETE FROM PASYSTEM_BANNER_DISMISSED WHERE uuid = ?")
                            .param(uuid)
                            .executeUpdate();

                        db.run("DELETE FROM PASYSTEM_BANNER_ALERT WHERE uuid = ?")
                                .param(uuid)
                                .executeUpdate();

                        db.commit();

                        return null;
                    }
                }
        );
    }


    public void setBannerActiveState(String uuid, boolean isActive) {
        DB.transaction(
                "Set active state for banner with uuid " + uuid,
                new DBAction<Void>() {
                    public Void call(DBConnection db) throws SQLException {
                        db.run("UPDATE PASYSTEM_BANNER_ALERT SET (active) VALUES (?) WHERE uuid = ?")
                                .param(new Integer(isActive ? 1 : 0))
                                .param(uuid)
                                .executeUpdate();

                        db.commit();

                        return null;
                    }
                }
        );
    }


    public void acknowledge(final String uuid, final String userEid, final String acknowledgementType) {
        new AcknowledgementStorage(AcknowledgementStorage.NotificationType.BANNER).acknowledge(uuid, userEid, acknowledgementType);
    }
}
