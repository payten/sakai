package org.sakaiproject.pasystem.impl.popups;

import java.io.InputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.tool.cover.SessionManager;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.User;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;


public class PopupSystem {

    private final String SPLASH_SCREEN_SHOWN = "pasystem.splash.screen.shown";

    private static final Logger LOG = LoggerFactory.getLogger(PopupSystem.class);

    public String getFooter() {
        Session session = SessionManager.getCurrentSession();
        User currentUser = UserDirectoryService.getCurrentUser();

        if (currentUser == null || session.getAttribute(SPLASH_SCREEN_SHOWN) != null) {
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
                session.setAttribute(SPLASH_SCREEN_SHOWN, "true");
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


    public String createPopup(String campaign, Date startDate, Date endDate, InputStream templateContent) {
        return new PopupManager().createPopup(campaign, startDate, endDate, templateContent);
    }

    public void openCampaign(String id) {
        new PopupManager().openCampaign(id);
    }
   
    public boolean hasCampaign(String descriptor) {
        return new PopupManager().hasCampaign(descriptor);
    }

   
}
