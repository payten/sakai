package org.sakaiproject.pasystem.impl;

import java.io.FileInputStream;
import java.io.IOException;

import org.sakaiproject.pasystem.api.PASystem;

import org.sakaiproject.component.cover.ServerConfigurationService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;

import java.util.logging.Logger;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import org.flywaydb.core.Flyway;

import org.sakaiproject.pasystem.impl.banners.BannerSystem;
import org.sakaiproject.portal.util.PortalUtils;

import org.sakaiproject.pasystem.impl.popups.PopupSystem;


import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;


class PASystemImpl implements PASystem {

    public void init() {
        if (ServerConfigurationService.getBoolean("auto.ddl", false) || ServerConfigurationService.getBoolean("pasystem.auto.ddl", false)) {
            runDBMigration(ServerConfigurationService.getString("vendor@org.sakaiproject.db.api.SqlService"));
        }

        PopupSystem popupSystem = new PopupSystem();
        if (!popupSystem.hasCampaign("goat-warning")) {
          try {
            String id = popupSystem.createPopup("goat-warning", new Date(0), new Date(System.currentTimeMillis() + Integer.MAX_VALUE),
                                                new FileInputStream("/var/tmp/custom-templates/goatwarning.vm"));

            popupSystem.openCampaign(id);

          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
    }

    public void destroy() {}

    public String getFooter() {
      StringBuilder result = new StringBuilder();

      Handlebars handlebars = new Handlebars();

      try {
        Template template = handlebars.compile("templates/shared_footer");

        Map<String, String> context = new HashMap<String, String>();

        context.put("portalCDNQuery", PortalUtils.getCDNQuery());

        result.append(template.apply(context));
      } catch (IOException e) {
        // Log.warn("something clever")
        return "";
      }

      BannerSystem bannerSystem = new BannerSystem();
      result.append(bannerSystem.getFooter());

      PopupSystem popupSystem = new PopupSystem();
      result.append(popupSystem.getFooter());

      return result.toString();
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
}
