package org.sakaiproject.pasystem.api;

public class PASystemException extends RuntimeException {

    public PASystemException(String msg) {
        super(msg);
    }

    public PASystemException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
