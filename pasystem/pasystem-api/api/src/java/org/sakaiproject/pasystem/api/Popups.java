package org.sakaiproject.pasystem.api;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface Popups {

    public String createCampaign(String descriptor, Date startDate, Date endDate, InputStream templateContent);

    public void openCampaign(String id);

    public boolean hasCampaign(String descriptor);

    public List<Popup> getAll();

    public Optional<Popup> getForId(String uuid);

    public List<String> getAssignees(String uuid);

    public boolean isOpenCampaign(final String uuid);
}
    
