package org.sakaiproject.pasystem.api;

import java.util.List;
import java.util.Date;
import java.sql.SQLException;


public interface Banners {

    public List<Banner> getActiveAlertsForServer(String serverId);

    public String createBanner(String message, String hosts, boolean isDismissible, boolean isActive, Date activeFrom, Date activeUntil) throws SQLException;

    public void updateBanner(String uuid, String message, String hosts, boolean isDismissible, boolean isActive, Date activeFrom, Date activeUntil);

    public void deleteBanner(String uuid);

    public void setBannerActiveState(String uuid, boolean isActive);

    public String getFooter();
}
    
