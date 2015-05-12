package org.sakaiproject.pasystem.impl;

import java.io.IOException;

import org.sakaiproject.pasystem.api.PASystem;

import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.component.cover.ServerConfigurationService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;

import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;

import org.flywaydb.core.Flyway;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.sakaiproject.pasystem.impl.banners.BannerAlert;
import org.sakaiproject.pasystem.impl.banners.BannerSystem;
import org.sakaiproject.portal.util.PortalUtils;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;


class PASystemImpl implements PASystem {

    public void init() {
        if (ServerConfigurationService.getBoolean("auto.ddl", false) || ServerConfigurationService.getBoolean("pasystem.auto.ddl", false)) {
            runDBMigration(ServerConfigurationService.getString("vendor@org.sakaiproject.db.api.SqlService"));
        }
    }

    public void destroy() {}

    public String getFooter() {
      Handlebars handlebars = new Handlebars();

      try {
      Template template = handlebars.compile("templates/footer");

      Map<String, String> context = new HashMap<String, String>();

      context.put("bannerJSON", getActiveBannersJSON());
      context.put("portalCDNQuery", PortalUtils.getCDNQuery());

      return template.apply(context);
      } catch (IOException e) {
        // Log.warn("something clever")
        return "";
      }
    }


    private void runDBMigration(final String vendor) {

        // We run this in a separate thread because Flyway will look to the
        // current thread's class loader for its files.  The system StartStop
        // thread has an unpredictable classloader state so we provide our own.

        Thread migrationRunner = new Thread() {
                public void run() {
                    Flyway flyway = new Flyway();

                    flyway.setLocations("db/migration/" + vendor);
                    flyway.setBaselineOnMigrate(true);
                    flyway.setTable("pasystem_schema_version");

                    flyway.setDataSource(ServerConfigurationService.getString("url@javax.sql.BaseDataSource"),
                                         ServerConfigurationService.getString("username@javax.sql.BaseDataSource"),
                                         ServerConfigurationService.getString("password@javax.sql.BaseDataSource"));

                    flyway.migrate();
                }
            };

        migrationRunner.setContextClassLoader(PASystemImpl.class.getClassLoader());
        migrationRunner.start();

        try {
            migrationRunner.join();
        } catch (InterruptedException e) {}
    }


    private String getActiveBannersJSON() {
      BannerSystem bannerSystem = new BannerSystem();
      JSONArray alerts = new JSONArray();
      String serverId = ServerConfigurationService.getString("serverId","localhost");

      for (BannerAlert alert : bannerSystem.getActiveBannerAlertsForServer(serverId)) {
        JSONObject alertData = new JSONObject();
        alertData.put("id", alert.uuid);
        alertData.put("message", alert.message);
        alertData.put("dismissible", alert.isDismissible());
        alerts.add(alertData);
      }

      return alerts.toJSONString();
    }
}
