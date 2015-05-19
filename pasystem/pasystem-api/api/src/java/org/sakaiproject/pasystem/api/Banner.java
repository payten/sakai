package org.sakaiproject.pasystem.api;

public interface Banner {

    public boolean isActive();

    public boolean isDismissible();

    public boolean isActiveForHost(String hostname);

    public String getUuid();

    public String getMessage();
}
    
