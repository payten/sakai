package org.sakaiproject.pasystem.api;

import java.util.List;
import java.util.Optional;


public interface Banners {

    public List<Banner> getActiveAlertsForServer(String serverId);

    public String createBanner(String message, String hosts, boolean isDismissible, boolean isActive, long startTime, long endTime);

    public void updateBanner(String uuid, String message, String hosts, boolean isDismissible, boolean isActive, long startTime, long endTime);

    public void deleteBanner(String uuid);

    public void setBannerActiveState(String uuid, boolean isActive);

    public List<Banner> getAll();

    public Optional<Banner> getForId(String uuid);
}
    
