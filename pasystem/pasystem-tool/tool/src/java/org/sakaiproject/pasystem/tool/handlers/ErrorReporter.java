package org.sakaiproject.pasystem.tool.handlers;

public interface ErrorReporter {
    public void addError(String field, String errorCode, String... values);
}
