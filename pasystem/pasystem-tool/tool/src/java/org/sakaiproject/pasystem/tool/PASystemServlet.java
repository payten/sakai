package org.sakaiproject.pasystem.tool;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class PASystemServlet extends HttpServlet {

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Writer w = response.getWriter();

        response.setHeader("Content-Type", "text/html");

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

        try {
            Template template = handlebars.compile("org/sakaiproject/pasystem/tool/views/layout");
            Map<String, Object> context = new HashMap<String, Object>();

            context.put("subpage", "index");
            context.put("now", new java.util.Date());

            w.write(template.apply(context));
        } catch (IOException e) {
            e.printStackTrace();
            // Log.warn("something clever")
        }
    }
}
