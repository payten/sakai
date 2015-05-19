package org.sakaiproject.pasystem.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.authz.cover.FunctionManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.pasystem.api.Banner;
import org.sakaiproject.pasystem.api.Banners;
import org.sakaiproject.pasystem.api.PASystem;
import org.sakaiproject.pasystem.api.Popup;
import org.sakaiproject.pasystem.api.Popups;
import org.sakaiproject.pasystem.impl.banners.BannerStorage;
import org.sakaiproject.pasystem.impl.popups.PopupStorage;
import org.sakaiproject.pasystem.impl.popups.PopupForUser;
import org.sakaiproject.portal.util.PortalUtils;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class PASystemImpl implements PASystem {

    private static final Logger LOG = LoggerFactory.getLogger(PASystemImpl.class);

    private final String POPUP_SCREEN_SHOWN = "pasystem.popup.screen.shown";

    public void init() {
        if (ServerConfigurationService.getBoolean("auto.ddl", false) || ServerConfigurationService.getBoolean("pasystem.auto.ddl", false)) {
            runDBMigration(ServerConfigurationService.getString("vendor@org.sakaiproject.db.api.SqlService"));
        }

        FunctionManager.registerFunction("pasystem.manage");


        Popups popupSystem = getPopups();
        if (!popupSystem.hasCampaign("goat-warning")) {
          try {
              String id = popupSystem.createCampaign("goat-warning", new Date(0), new Date(System.currentTimeMillis() + Integer.MAX_VALUE),
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

      result.append(getBannersFooter());
      result.append(getPopupsFooter());

      return result.toString();
    }


    public Banners getBanners() {
        return new BannerStorage();
    }


    public Popups getPopups() {
        return new PopupStorage();
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


    private String getBannersFooter() {
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

        for (Banner alert : getBanners().getActiveAlertsForServer(serverId)) {
            JSONObject alertData = new JSONObject();
            alertData.put("id", alert.getUuid());
            alertData.put("message", alert.getMessage());
            alertData.put("dismissible", alert.isDismissible());
            alerts.add(alertData);
        }

        return alerts.toJSONString();
    }


    private String getPopupsFooter() {
        Session session = SessionManager.getCurrentSession();
        User currentUser = UserDirectoryService.getCurrentUser();

        if (currentUser == null || session.getAttribute(POPUP_SCREEN_SHOWN) != null) {
            return "";
        }

        Popup popup = new PopupForUser(currentUser).getPopup();

        if (popup.isActive()) {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("popupTemplate", popup.getTemplate());
            context.put("popupUuid", popup.getUuid());
            context.put("sakai_csrf_token", session.getAttribute("sakai.csrf.token"));
            context.put("popup", true);

            if (currentUser.getEid() != null) {
                // Delivered!
                session.setAttribute(POPUP_SCREEN_SHOWN, "true");
            }

            Handlebars handlebars = new Handlebars();

            try {
                Template template = handlebars.compile("templates/popup_footer");
                return template.apply(context);
            } catch (IOException e) {
                LOG.warn("Popup footer failed", e);
                return "";
            }
        }


        return "";
    }

}
