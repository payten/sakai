package org.sakaiproject.pasystem.tool;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Handler {
    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context);

    public boolean hasRedirect();

    public String getRedirect();

    public List<Error> getErrors();

    public Map<String, List<String>> getFlashMessages();
}
