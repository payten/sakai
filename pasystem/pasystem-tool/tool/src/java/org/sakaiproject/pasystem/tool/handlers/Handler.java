package org.sakaiproject.pasystem.tool.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface Handler {
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context);

    public boolean hasRedirect();

    public String getRedirect();

    public List<Error> getErrors();

    public Map<String, List<String>> getFlashMessages();
}
