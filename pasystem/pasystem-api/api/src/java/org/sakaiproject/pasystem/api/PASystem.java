package org.sakaiproject.pasystem.api;

public interface PASystem {

    public void init();

    public void destroy();

    public String getFooter();

    public Popups getPopups();

    public Banners getBanners();
}
    
