package org.sakaiproject.pasystem.api;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface Popups {

    public String createCampaign(String descriptor, long startTime, long endTime,
                                 InputStream templateContent,
                                 boolean isOpenCampaign,
                                 Optional<List<String>> assignToUsers);

    public boolean updateCampaign(String uuid, String descriptor, long startTime, long endTime,
                                  Optional<InputStream> templateInput,
                                  boolean isOpenCampaign,
                                  Optional<List<String>> assignToUsers);

    public List<Popup> getAll();

    public Optional<Popup> getForId(String uuid);

    public List<String> getAssignees(String uuid);

    public boolean isOpenCampaign(final String uuid);
}
    
