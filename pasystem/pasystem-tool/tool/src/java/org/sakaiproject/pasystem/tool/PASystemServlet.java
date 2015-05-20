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

import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;

import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.tool.cover.ToolManager;

import org.sakaiproject.pasystem.api.PASystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sakaiproject.tool.api.ToolURL;
import org.sakaiproject.tool.api.ToolURLManager;


public class PASystemServlet extends HttpServlet {

    private static String FLASH_MESSAGE_KEY = "pasystem-tool.flash.errors";

    private static final Logger LOG = LoggerFactory.getLogger(PASystemServlet.class);

    private PASystem paSystem;
    private URL toolBaseURL;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        paSystem = (PASystem) ComponentManager.get(PASystem.class);
    }


    private Handler handlerForRequest(HttpServletRequest request) {
        String path = request.getPathInfo();

        if (path == null) {
            path = "";
        }

        if (path.contains("/popups/")) {
            return new PopupsHandler(paSystem);
        } else if (path.contains("/banners/")) {
            return new BannersHandler(paSystem);
        } else {
            return new IndexHandler(paSystem);
        }
    }


    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Content-Type", "text/html");

        URL toolBaseURL = determineBaseURL();
        Handlebars handlebars = loadHandlebars(toolBaseURL);

        try {
            Template template = handlebars.compile("org/sakaiproject/pasystem/tool/views/layout");
            Map<String, Object> context = new HashMap<String, Object>();

            context.put("skinRepo", ServerConfigurationService.getString("skin.repo", ""));
            context.put("randomSakaiHeadStuff", request.getAttribute("sakai.html.head"));

            Handler handler = handlerForRequest(request);


            Map<String, List<String>> messages = loadFlashMessages();

            handler.handle(request, response, context);

            storeFlashMessages(handler.getFlashMessages());

            if (handler.hasRedirect()) {
                response.sendRedirect(toolBaseURL + handler.getRedirect());
            } else {
                context.put("flash", messages);
                context.put("errors", handler.getErrors());
                response.getWriter().write(template.apply(context));
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Log.warn("something clever")
        }
    }



    private void storeFlashMessages(Map<String, List<String>> messages) {
        Session session = SessionManager.getCurrentSession();
        session.setAttribute(FLASH_MESSAGE_KEY, messages);
    }


    private Map<String, List<String>> loadFlashMessages() {
        Session session = SessionManager.getCurrentSession();
        

        if (session.getAttribute(FLASH_MESSAGE_KEY) != null) {
            Map<String, List<String>> flashErrors = (Map<String, List<String>>) session.getAttribute(FLASH_MESSAGE_KEY);
            session.removeAttribute(FLASH_MESSAGE_KEY);

            return flashErrors;
        } else {
            return new HashMap<String, List<String>>();
        }
    }


    private URL determineBaseURL() {
        String siteId = ToolManager.getCurrentPlacement().getContext();
        String toolId = ToolManager.getCurrentPlacement().getId();

        try {
            return new URL(ServerConfigurationService.getPortalUrl() + "/site/" + siteId + "/tool/" + toolId + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    private Handlebars loadHandlebars(final URL baseURL) {
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
                    long utcTime = options.param(0) == null ? 0 : options.param(0);

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
                        return new URL(baseURL, type + "/" + uuid + "/" + action).toString();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
            });


        handlebars.registerHelper("newURL", new Helper<Object>() {
            public CharSequence apply(final Object context, final Options options) {
                String type = options.param(0);
                String action = options.param(1);


                try {
                    return new URL(baseURL, type + "/" + action).toString();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        return handlebars;
    }
}
