package org.sakaiproject.pasystem.tool;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.tool.cover.ToolManager;

import org.sakaiproject.pasystem.api.PASystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.tool.api.ToolURL;
import org.sakaiproject.tool.api.ToolURLManager;


public class PASystemServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PASystemServlet.class);

    private PASystem paSystem;
    private URL toolBaseURL;

    private List<Handler> handlers;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        paSystem = (PASystem) ComponentManager.get(PASystem.class);

        try { 
            String siteId = ToolManager.getCurrentPlacement().getContext();
            String toolId = ToolManager.getCurrentPlacement().getId();

            toolBaseURL = new URL(ServerConfigurationService.getPortalUrl() + "/site/" + siteId + "/tool/" + toolId + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        handlers = new ArrayList<Handler>();

        handlers.add(new IndexHandler(paSystem));
        handlers.add(new BannersHandler(paSystem));
        handlers.add(new PopupsHandler(paSystem));
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Content-Type", "text/html");

        Handlebars handlebars = loadHandlebars();

        try {
            Template template = handlebars.compile("org/sakaiproject/pasystem/tool/views/layout");
            Map<String, Object> context = new HashMap<String, Object>();

            context.put("skinRepo", ServerConfigurationService.getString("skin.repo", ""));
            context.put("randomSakaiHeadStuff", request.getAttribute("sakai.html.head"));

            for (Handler handler : handlers) {
                if (handler.willHandle(request)) {
                    LOG.info("Using request handler {}", handler);
                    
                    handler.handle(request, response, context);
                    break;
                }
            }

        response.getWriter().write(template.apply(context));
        } catch (IOException e) {
            e.printStackTrace();
            // Log.warn("something clever")
        }
    }


    private Handlebars loadHandlebars() {
        Handlebars handlebars = new Handlebars();

        handlebars.registerHelper("subpage", new Helper<Object>() {
                public CharSequence apply(final Object context, final Options options) {
                    String subpage = options.param(0);
                    try {
                        Template template = handlebars.compile("org/sakaiproject/pasystem/tool/views/" + subpage);
                        return template.apply(context);
                    } catch (IOException e) {
                        return "";
                    }
                }
            });

        handlebars.registerHelper("show-time", new Helper<Object>() {
                public CharSequence apply(final Object context, final Options options) {
                    long utcTime = options.param(0);

                    if (utcTime == 0) {
                        return "-";
                    }

                    Time time = TimeService.newTime(utcTime);

                    return time.toStringLocalFull();
                }
            });


        handlebars.registerHelper("actionURL", new Helper<Object>() {
                public CharSequence apply(final Object context, final Options options) {
                    String type = options.param(0);
                    String uuid = options.param(1);
                    String action = options.param(2);


                    try {
                        return new URL(toolBaseURL, type + "/" + uuid + "/" + action).toString();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            });


        return handlebars;
    }
}
