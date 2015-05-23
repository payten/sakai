package org.sakaiproject.pasystem.tool.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


public abstract class CrudHandler extends BaseHandler {

    protected String extractId(HttpServletRequest request) {
        String[] bits = request.getPathInfo().split("/");

        if (bits.length < 2) {
            addError("uuid", "uuid_missing", request.getPathInfo());
            return "";
        } else {
            return bits[bits.length - 2];
        }
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) {
        if (request.getPathInfo().contains("/edit")) {
            if (isGet(request)) {
                handleEdit(request, context);
            } else if (isPost(request)) {
                handleCreateOrUpdate(request, context, CrudMode.UPDATE);
            }
        } else if (request.getPathInfo().contains("/new")) {
            if (isGet(request)) {
                showNewForm(context);
            } else if (isPost(request)) {
                handleCreateOrUpdate(request, context, CrudMode.CREATE);
            }
        } else if (request.getPathInfo().contains("/delete")) {
            if (isGet(request)) {
                sendRedirect("");
            } else if (isPost(request)) {
                handleDelete(request);
            }
        } else {
            sendRedirect("");
        }
    }


    protected abstract void handleDelete(HttpServletRequest request);

    protected abstract void showNewForm(Map<String, Object> context);

    protected abstract void handleCreateOrUpdate(HttpServletRequest request, Map<String, Object> context, CrudMode mode);

    protected abstract void handleEdit(HttpServletRequest request, Map<String, Object> context);


    public enum CrudMode {
        CREATE,
        UPDATE
    }
}

