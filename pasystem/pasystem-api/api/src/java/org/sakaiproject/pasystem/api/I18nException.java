package org.sakaiproject.pasystem.api;

public class I18nException extends RuntimeException {

    public I18nException(String msg) {
        super(msg);
    }

    public I18nException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
