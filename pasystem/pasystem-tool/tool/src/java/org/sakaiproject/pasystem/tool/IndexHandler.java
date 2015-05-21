package org.sakaiproject.pasystem.tool;

import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.pasystem.api.PASystem;


public class IndexHandler extends BaseHandler {

    private PASystem paSystem;

    public IndexHandler(PASystem pasystem) {
        this.paSystem = pasystem;
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        context.put("subpage", "index");
        context.put("banners", paSystem.getBanners().getAll());
        context.put("popups", paSystem.getPopups().getAll());
        context.put("timezoneCheckActive", ServerConfigurationService.getBoolean("pasystem.timezone-check", false));
    }
}
