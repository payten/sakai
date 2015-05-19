package org.sakaiproject.pasystem.tool;

import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Handler {

    public boolean willHandle(HttpServletRequest request);

    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context);
}
