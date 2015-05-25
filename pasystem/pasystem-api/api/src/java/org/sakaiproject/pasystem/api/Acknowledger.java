package org.sakaiproject.pasystem.api;

public interface Acknowledger {
    public void acknowledge(final String uuid, final String userEid, final String acknowledgementType);
}
