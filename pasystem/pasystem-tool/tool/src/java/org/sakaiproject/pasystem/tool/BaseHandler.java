package org.sakaiproject.pasystem.tool;

import javax.servlet.http.HttpServletRequest;


abstract class BaseHandler implements Handler {

    public boolean isGet(HttpServletRequest request) {
        return "GET".equals(request.getMethod());
    }


    public String extractId(HttpServletRequest request) {
        String[] bits = request.getPathInfo().split("/");

        if (bits.length < 2) {
            throw new RuntimeException("Couldn't extract an ID from: " + request.getPathInfo());
        } else {
            return bits[bits.length - 2];
        }
    }


    public String toString() {
        return this.getClass().toString();
    }
}

