package org.sakaiproject.pasystem.impl.popups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.UUID;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.sakaiproject.pasystem.impl.common.DB;
import org.sakaiproject.pasystem.impl.common.DBAction;
import org.sakaiproject.pasystem.impl.common.DBConnection;
import org.sakaiproject.pasystem.impl.common.DBResults;

import org.sakaiproject.pasystem.api.Popups;

import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;




public class PopupManager implements Popups {

    private static final Logger LOG = LoggerFactory.getLogger(PopupManager.class);

    private final String POPUP_SCREEN_SHOWN = "pasystem.popup.screen.shown";


    public String getFooter() {
        Session session = SessionManager.getCurrentSession();
        User currentUser = UserDirectoryService.getCurrentUser();

        if (currentUser == null || session.getAttribute(POPUP_SCREEN_SHOWN) != null) {
            return "";
        }

        UserPopupInteraction popups = new UserPopupInteraction(currentUser);

        Popup popup = popups.getPopup();

        if (popup.isActive()) {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("popupTemplate", popup.getTemplate());
            context.put("popupCampaign", popup.getCampaign());
            context.put("sakai_csrf_token", session.getAttribute("sakai.csrf.token"));
            context.put("popup", true);

            if (currentUser.getEid() != null) {
                // Delivered!
                session.setAttribute(POPUP_SCREEN_SHOWN, "true");
            }

            return generateFooter(context);
        }


        return "";
    }


    private String generateFooter(Map<String, Object> context) {

        Handlebars handlebars = new Handlebars();

        try {
            Template template = handlebars.compile("templates/popup_footer");
            return template.apply(context);
        } catch (IOException e) {
            LOG.warn("Popup footer failed", e);
            return "";
        }
    }


    public String createCampaign(String descriptor, Date startDate, Date endDate, InputStream templateContent) {
        return DB.transaction
            ("Popup creation",
             new DBAction<String>() {
                 public String call(DBConnection db) throws SQLException {
                     String id = insertPopupScreen(db, descriptor, startDate, endDate);
                     insertPopupContent(db, id, templateContent);
                     db.commit();

                     return id;
                 }
             });
    }


    public void openCampaign(String id) {
        DB.transaction
            ("Mark popup " + id + " as open campaign",
             new DBAction<Void>() {
                 public Void call(DBConnection db) throws SQLException {
                     db.run("INSERT INTO PASYSTEM_POPUP_ASSIGN (uuid, open_campaign) VALUES (?, 1)")
                         .param(id)
                         .executeUpdate();

                     db.commit();
                     return null;
                 }
             });
    }


    public boolean hasCampaign(String descriptor) {
        return DB.transaction
            ("Check whether campaign exists for descriptor: " + descriptor,
             new DBAction<Boolean>() {
                 public Boolean call(DBConnection db) throws SQLException {
                     try (DBResults results = db.run("SELECT uuid from PASYSTEM_POPUP_SCREENS WHERE descriptor = ?")
                          .param(descriptor)
                          .executeQuery()) {
                         for (ResultSet result : results) {
                             return true;
                         }

                         return false;
                     }
                 }
             });
    }


    private String insertPopupScreen(DBConnection db, String descriptor, Date startDate, Date endDate) throws SQLException {
        String id = UUID.randomUUID().toString();

        db.run("INSERT INTO PASYSTEM_POPUP_SCREENS (uuid, descriptor, start_time, end_time) VALUES (?, ?, ?, ?)")
            .param(id)
            .param(descriptor)
            .param(startDate.getTime())
            .param(endDate.getTime())
            .executeUpdate();

        return id;
    }


    private void insertPopupContent(DBConnection db, String id, InputStream templateContent) throws SQLException {
        db.run("INSERT INTO PASYSTEM_POPUP_CONTENT (uuid, template_content) VALUES (?, ?)")
            .param(id)
            .param(new InputStreamReader(templateContent))
            .executeUpdate();
    }
}
