package org.sakaiproject.pasystem.api;

import java.util.Locale;

public interface PASystem {

    public void init();

    public void destroy();

    public String getFooter();

    public Popups getPopups();

    public Banners getBanners();

    public I18n getI18n(ClassLoader loader, String resourceBase, Locale locale);
}
    
