package org.sakaiproject.pasystem.tool;

import org.sakaiproject.pasystem.api.Popup;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.pasystem.api.PASystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PopupsHandler extends BaseHandler implements Handler {

    private static final Logger LOG = LoggerFactory.getLogger(PopupsHandler.class);

    private PASystem paSystem;

    public PopupsHandler(PASystem pasystem) {
        this.paSystem = pasystem;
    }

    public boolean willHandle(HttpServletRequest request) {
        return (request.getPathInfo() != null) && (request.getPathInfo().contains("/popups/"));
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        if (request.getPathInfo().contains("/edit")) {
            if (isGet(request)) {
                handleEdit(extractId(request), context);
            }
        }
    }


    private void handleEdit(String uuid, Map<String, Object> context) {
        context.put("subpage", "popup_form");

        Optional<Popup> popup = paSystem.getPopups().getForId(uuid);

        if (popup.isPresent()) {
            if (paSystem.getPopups().isOpenCampaign(uuid)) {
                context.put("isOpen", true);
            } else {
                context.put("isOpen", false);
                context.put("popupAssignees", paSystem.getPopups().getAssignees(uuid));
            }

            context.put("popup", popup.get());
        } else {
            LOG.warn("No popup found for UUID: " + uuid);
        }
    }

}
