package org.sakaiproject.pasystem.api;

public interface Acknowledger {

    public final String TEMPORARY = "temporary";
    public final String PERMANENT = "permanent";

    public void acknowledge(final String uuid, final String userEid);

    public void acknowledge(final String uuid, final String userEid, final String acknowledgementType);
}
