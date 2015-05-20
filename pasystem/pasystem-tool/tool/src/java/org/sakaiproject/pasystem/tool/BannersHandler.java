package org.sakaiproject.pasystem.tool;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.pasystem.api.Banner;
import org.sakaiproject.pasystem.api.PASystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BannersHandler extends BaseHandler implements Handler {

    private PASystem paSystem;

    private static final Logger LOG = LoggerFactory.getLogger(PopupsHandler.class);

    public BannersHandler(PASystem pasystem) {
        this.paSystem = pasystem;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        if (request.getPathInfo().contains("/edit")) {
            if (isGet(request)) {
              handleEdit(extractId(request), context);
            }
        }
        if (request.getPathInfo().contains("/new")) {
            if (isGet(request)) {
                handleNew(context);
            }
        }
    }


    private void handleEdit(String uuid, Map<String, Object> context) {
        context.put("subpage", "banner_form");

        Optional<Banner> banner = paSystem.getBanners().getForId(uuid);

        if (banner.isPresent()) {
            context.put("banner", banner.get());
        } else {
            LOG.warn("No banner found for UUID: " + uuid);
        }
    }


    private void handleNew(Map<String, Object> context) {
        context.put("subpage", "banner_form");
    }
}
