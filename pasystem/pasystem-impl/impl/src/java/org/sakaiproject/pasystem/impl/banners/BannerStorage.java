package org.sakaiproject.pasystem.impl.banners;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.sakaiproject.pasystem.api.Banner;
import org.sakaiproject.pasystem.api.Banners;
import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BannerStorage implements Banners {

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
               banners.add(new BannerImpl(result.getString("uuid"),
                                          result.getString("message"),
                                          result.getString("hosts"),
                                          result.getInt("dismissible"),
                                          result.getInt("active"),
                                          result.getLong("start_time"),
                                          result.getLong("end_time")));
             }

             return banners;
           }
         }
       });
  };


  public Optional<Banner> getForId(final String uuid) {
    return DB.transaction
      ("Find a banner by uuid",
       new DBAction<Optional<Banner>>() {
         public Optional<Banner> call(DBConnection db) throws SQLException {
           try (DBResults results = db.run("SELECT * from PASYSTEM_BANNER_ALERT WHERE UUID = ?")
                .param(uuid)
                .executeQuery()) {
             for (ResultSet result : results) {
               return Optional.of(new BannerImpl(result.getString("uuid"),
                                                 result.getString("message"),
                                                 result.getString("hosts"),
                                                 result.getInt("dismissible"),
                                                 result.getInt("active"),
                                                 result.getLong("start_time"),
                                                 result.getLong("end_time")));
             }

             return Optional.empty();
           }
         }
       });
  }


  public List<Banner> getActiveAlertsForServer(String serverId) {
    return DB.transaction
      ("Find all active alerts for the server: " + serverId,
      new DBAction<List<Banner>>() {
        public List<Banner> call(DBConnection db) throws SQLException {
          List<Banner> alerts = new ArrayList<Banner>();
          try (DBResults results = db.run("SELECT * from PASYSTEM_BANNER_ALERT where ACTIVE = 1")
                                     .executeQuery()) {
            for (ResultSet result : results) {
              Banner alert = new BannerImpl(result.getString("uuid"),
                                            result.getString("message"),
                                            result.getString("hosts"),
                                            result.getInt("dismissible"),
                                            result.getInt("active"),
                                            result.getLong("start_time"),
                                            result.getLong("end_time"));
  
              if (alert.isActiveForHost(serverId)) {
                alerts.add(alert);
              }
            }
  
            return alerts;
          }
        }
      });
  }


  public String createBanner(String message, String hosts, boolean isDismissible, boolean isActive, long startTime, long endTime) {
    return DB.transaction(
      "Create an banner",
      new DBAction<String>() {
        public String call(DBConnection db) throws SQLException {
          String id = UUID.randomUUID().toString();

          db.run("INSERT INTO PASYSTEM_BANNER_ALERT (uuid, message, hosts, dismissible, active, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?)")
            .param(id)
            .param(message)
            .param(hosts)
            .param(new Integer(isDismissible ? 1 : 0))
            .param(new Integer(isActive ? 1 : 0))
            .param(startTime)
            .param(endTime)
            .executeUpdate();

          db.commit();

          return id;
        }
      }
    );
  }


  public void updateBanner(String uuid, String message, String hosts, boolean isDismissible, boolean isActive, long startTime, long endTime) {
    DB.transaction(
      "Update banner with uuid " + uuid,
      new DBAction<Void>() {
        public Void call(DBConnection db) throws SQLException {
          db.run("UPDATE PASYSTEM_BANNER_ALERT SET message = ?, hosts = ?, dismissible = ?, active = ?, start_time = ?, end_time = ? WHERE uuid = ?")
            .param(message)
            .param(hosts)
            .param(new Integer(isDismissible ? 1 : 0))
            .param(new Integer(isActive ? 1 : 0))
            .param(startTime)
            .param(endTime)
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
          db.run("DELETE FROM PASYSTEM_BANNER_ALERT WHERE uuid = ?")
          .param(uuid)
          .executeUpdate();

          db.commit();

          return null;
        }
      });
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

}
