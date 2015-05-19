package org.sakaiproject.pasystem.impl.banners;

import java.io.IOException;

import java.util.UUID;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.sakaiproject.component.cover.ServerConfigurationService;

import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;

import org.sakaiproject.pasystem.api.Banners;
import org.sakaiproject.pasystem.api.Banner;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;



public class BannerManager implements Banners {
  
  public String getFooter() {
    Handlebars handlebars = new Handlebars();

    try {
      Template template = handlebars.compile("templates/banner_footer");

      Map<String, String> context = new HashMap<String, String>();

      context.put("bannerJSON", getActiveBannersJSON());

      return template.apply(context);
    } catch (IOException e) {
      // Log.warn("something clever")
      return "";
    }
  }
  
  private String getActiveBannersJSON() {
    JSONArray alerts = new JSONArray();
    String serverId = ServerConfigurationService.getString("serverId","localhost");

    for (Banner alert : new BannerManager().getActiveAlertsForServer(serverId)) {
      JSONObject alertData = new JSONObject();
      alertData.put("id", alert.getUuid());
      alertData.put("message", alert.getMessage());
      alertData.put("dismissible", alert.isDismissible());
      alerts.add(alertData);
    }

    return alerts.toJSONString();
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
                                            result.getLong("active_from"),
                                            result.getLong("active_until"));
  
              if (alert.isActiveForHost(serverId)) {
                alerts.add(alert);
              }
            }
  
            return alerts;
          }
        }
      });
  }


  public String createBanner(String message, String hosts, boolean isDismissible, boolean isActive, Date activeFrom, Date activeUntil) throws SQLException {
    return DB.transaction(
      "Create an banner",
      new DBAction<String>() {
        public String call(DBConnection db) throws SQLException {
          String id = UUID.randomUUID().toString();

          db.run("INSERT INTO PASYSTEM_BANNER_ALERT (uuid, message, hosts, dismissible, active, active_from, active_until) VALUES (?, ?, ?, ?, ?, ?, ?)")
            .param(id)
            .param(message)
            .param(hosts)
            .param(new Integer(isDismissible ? 1 : 0))
            .param(new Integer(isActive ? 1 : 0))
            .param(activeFrom.getTime())
            .param(activeUntil.getTime())
            .executeUpdate();

          return id;
        }
      }
    );
  }


  public void updateBanner(String uuid, String message, String hosts, boolean isDismissible, boolean isActive, Date activeFrom, Date activeUntil) {
    DB.transaction(
      "Update banner with uuid " + uuid,
      new DBAction<Void>() {
        public Void call(DBConnection db) throws SQLException {
          db.run("UPDATE PASYSTEM_BANNER_ALERT SET (message, hosts, dismissible, active, active_from, active_until) VALUES (?, ?, ?, ?, ?, ?) WHERE uuid = ?")
            .param(message)
            .param(hosts)
            .param(new Integer(isDismissible ? 1 : 0))
            .param(new Integer(isActive ? 1 : 0))
            .param(activeFrom.getTime())
            .param(activeUntil.getTime())
            .param(uuid)
            .executeUpdate();

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

        return null;
      }
    }
    );
  }

}
