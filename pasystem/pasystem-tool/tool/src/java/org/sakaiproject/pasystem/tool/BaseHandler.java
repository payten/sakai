package org.sakaiproject.pasystem.tool;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


abstract class BaseHandler implements Handler {

    private List<Error> errors;
    private Map<String, List<String>> flashMessages;
    private String redirectURI;

    protected boolean isGet(HttpServletRequest request) {
        return "GET".equals(request.getMethod());
    }

    protected boolean isPost(HttpServletRequest request) {
        return "POST".equals(request.getMethod());
    }

    protected void sendRedirect(String uri) {
        redirectURI = uri;
    }

    public boolean hasRedirect() {
        return redirectURI != null;
    }

    public String getRedirect() {
        return redirectURI;
    }

    protected String extractId(HttpServletRequest request) {
        String[] bits = request.getPathInfo().split("/");

        if (bits.length < 2) {
            addError("uuid", "uuid_missing", request.getPathInfo());
            return "";
        } else {
            return bits[bits.length - 2];
        }
    }

    protected void addError(String field, String errorCode, String... values) {
        if (errors == null) {
            errors = new ArrayList<Error>();
        }

        errors.add(new Error(field, errorCode, values));
    }

    protected boolean hasErrors() {
        return !getErrors().isEmpty();
    }

    public List<Error> getErrors() {
        if (errors == null) {
            errors = new ArrayList<Error>();
        }

        return errors;
    }

    public void flash(String level, String message) {
        if (flashMessages == null) {
            flashMessages = new HashMap<String, List<String>>();
        }

        if (flashMessages.get(level) == null) {
            flashMessages.put(level, new ArrayList<String>());
        }

        flashMessages.get(level).add(message);
    }

    public Map<String, List<String>> getFlashMessages() {
        if (flashMessages == null) {
            flashMessages = new HashMap<String, List<String>>();
        }

        return flashMessages;
    }

    public String toString() {
        return this.getClass().toString();
    }


    enum CrudMode {
        CREATE,
        UPDATE
    }
}

