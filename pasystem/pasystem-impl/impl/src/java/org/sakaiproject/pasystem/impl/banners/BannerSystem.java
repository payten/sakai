package org.sakaiproject.pasystem.impl.banners;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import org.sakaiproject.db.api.SqlService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.sakaiproject.component.cover.ServerConfigurationService;


public class BannerSystem {

  private SqlService sqlService;

  public BannerSystem() {
    if(sqlService == null) {
      sqlService = (SqlService) org.sakaiproject.component.cover.ComponentManager.get("org.sakaiproject.db.api.SqlService");
    }
  }

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
      alertData.put("id", alert.uuid);
      alertData.put("message", alert.message);
      alertData.put("dismissible", alert.isDismissible());
      alerts.add(alertData);
    }

    return alerts.toJSONString();
  }
}
