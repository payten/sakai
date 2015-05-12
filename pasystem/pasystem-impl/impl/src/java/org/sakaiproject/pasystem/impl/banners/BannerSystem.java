package org.sakaiproject.pasystem.impl.banners;

import org.sakaiproject.db.api.SqlService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BannerSystem {

  private SqlService sqlService;

  public BannerSystem() {
    if(sqlService == null) {
      sqlService = (SqlService) org.sakaiproject.component.cover.ComponentManager.get("org.sakaiproject.db.api.SqlService");
    }
  }

  public List<BannerAlert> getActiveBannerAlertsForServer(String serverId) {
    List<BannerAlert> alerts = new ArrayList<BannerAlert>();

    try {
      Connection db = sqlService.borrowConnection();

      try {
        PreparedStatement ps = db.prepareStatement("select * from PASYSTEM_BANNER_ALERT where ACTIVE = 1");

        ResultSet rs = ps.executeQuery();
        try {
          while (rs.next()) {
            BannerAlert alert = new BannerAlert(rs.getString("uuid"),
                                                rs.getString("message"),
                                                rs.getString("hosts"),
                                                rs.getInt("dismissible"),
                                                rs.getInt("active"),
                                                rs.getLong("active_from"),
                                                rs.getLong("active_until"));

            if (alert.isActiveForHost(serverId)) {
              alerts.add(alert);
            }
          }
        } finally {
          rs.close();
        }
      } finally {
        sqlService.returnConnection(db);
      }
    } catch (SQLException e) {
      System.err.println(e);
    }

    return alerts;
  }
}
